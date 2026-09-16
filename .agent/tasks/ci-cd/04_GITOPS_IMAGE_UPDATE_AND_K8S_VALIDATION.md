# ☸️ Görev 04: GitOps Manifest Enjeksiyonu ve Kubernetes Testleri

Bu görev; derlenen Docker imajının Commit SHA hash'inin Kustomize `kustomization.yaml` dosyasına otomatik olarak işlenmesi, Git'e `[skip ci]` ile commit edilmesi ve ardından canlı ortama dağıtılmadan önce **Kubernetes Render ve Kubeconform OpenAPI Şema Doğrulama Testlerinin** koşulması adımlarını içerir.

---

## 🎯 Aşamalar ve Uygulama Detayları

### 1. Aşama: Kustomize Manifestosuna İmaj Tag'i Enjekte Etme
Docker Hub'a başarıyla pushlanan `sha-xxxxxxx` etiketi, hedef overlay (`kubernetes/overlays/prod/kustomization.yaml`) dosyasına `kustomize edit set image` komutuyla yazılır:

```bash
cd kubernetes/overlays/prod
kustomize edit set image doguhannilt/<service-name>=doguhannilt/<service-name>:sha-xxxxxxx
```

### 2. Aşama: Otomatik Git Commit ve Push (`[skip ci]`)
Değişiklik Git repository'sine pushlanırken sonsuz CI/CD döngüsüne girmemesi için commit mesajına `[skip ci]` eklenir.

### 3. Aşama: Kubernetes Shift-Left Doğrulama ve Uyumluluk Testleri
Manifestolar canlı ortama gitmeden önce runner makinesinde doğrudan test edilir:
1. **Render Testi:** `kustomize build kubernetes/overlays/prod > /tmp/rendered.yaml`
2. **K8s OpenAPI Schema Testi (`kubeconform`):**
   - K8s v1.31 API sözdizim kontrolü
   - Bilinmeyen veya yanlış alan tespiti (`-strict`)
   - CRD (ExternalSecrets vb.) şema kontrolü
3. **Güvenlik/PSS Kontrolü:** Pod Security Standard Restricted (UID 1000, readOnlyRootFilesystem, drop ALL) denetimi.

---

## 🛠️ GitHub Actions YAML Adım Şablonu

```yaml
  gitops-update-and-k8s-validate:
    name: ☸️ GitOps Manifest & K8s Validation
    needs: docker-build-scan-push
    runs-on: [self-hosted, linux]
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          ref: main

      - name: Inject SHA Image Tag to Kustomize Overlay
        run: |
          TAG="${{ needs.docker-build-scan-push.outputs.short_sha }}"
          echo "Injecting tag ${TAG} into ${{ inputs.k8s_overlay_path }}/kustomization.yaml..."
          
          cd ${{ inputs.k8s_overlay_path }}
          kustomize edit set image ${{ inputs.image_name }}=${{ inputs.image_name }}:${TAG}
          cat kustomization.yaml

      - name: Commit and Push Manifest Changes
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          
          git add ${{ inputs.k8s_overlay_path }}/kustomization.yaml
          if git diff --staged --quiet; then
            echo "No manifest changes detected."
          else
            git commit -m "chore(gitops): promote ${{ inputs.service_name }} to ${{ needs.docker-build-scan-push.outputs.short_sha }} [skip ci]"
            git push origin main
          fi

      - name: 🔍 1. Kustomize Render Verification
        run: |
          echo "Rendering Kustomize manifests for ${{ inputs.k8s_overlay_path }}..."
          kustomize build ${{ inputs.k8s_overlay_path }} > /tmp/rendered-manifests.yaml
          echo "Render OK! Total lines: $(wc -l < /tmp/rendered-manifests.yaml)"

      - name: 🛡️ 2. Kubeconform K8s OpenAPI Schema Validation
        run: |
          echo "Running Kubeconform against Kubernetes v1.31 schema..."
          kubeconform \
            -strict \
            -ignore-missing-schemas \
            -kubernetes-version 1.31.0 \
            -schema-location default \
            -schema-location 'https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json' \
            /tmp/rendered-manifests.yaml
          echo "Kubeconform Validation PASSED! ✅"

      - name: 🏁 Pipeline Execution Completed (Deployment Gate Paused)
        run: |
          echo "================================================================="
          echo "🎉 PIPELINE SUCCESSFUL!"
          echo "📦 Service: ${{ inputs.service_name }}"
          echo "🏷️ Image Tag: ${{ inputs.image_name }}:${{ needs.docker-build-scan-push.outputs.short_sha }}"
          echo "☸️ K8s Manifests: Rendered & Schema Verified!"
          echo "⏸️ Deployment: Deployment stage paused as requested by policy."
          echo "================================================================="
```

---

## ✅ Görev Tamamlanma Kriterleri

- [ ] `kustomize edit set image` ile `kustomization.yaml` dosyası hatasız güncelleniyor.
- [ ] Git commit mesajında `[skip ci]` bayrağı kullanılarak döngü engelleniyor.
- [ ] `kustomize build` ve `kubeconform` testleri sıfır hata ile geçiyor.
- [ ] Canlı dağıtım yapılmadan pipeline başarı raporu ile güvenli noktada tamamlanıyor.

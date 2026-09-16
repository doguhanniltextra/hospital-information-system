# 🔬 Görev 03: Test, Değişmez İmaj Derleme ve Güvenlik Taraması

Bu görev; Self-Hosted Runner üzerinde Maven birim testlerinin koşulması, değişmez (immutable) Git Commit SHA bazlı Docker imajının Buildx ile derlenmesi, Trivy güvenlik taramasından geçirilmesi ve Docker Hub'a pushlanması adımlarını içerir.

---

## 🎯 Aşamalar ve Uygulama Detayları

### 1. Aşama: Kod Kalitesi ve Birim Testler (`code-quality-and-test`)
Self-hosted ortamında mevcut Maven ve JDK kullanılarak bağımsız testler koşulur:

```yaml
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        with:
          clean: true

      - name: Run Maven Unit Tests & Spotless
        run: |
          cd ${{ inputs.service_path }}
          echo "🚀 Running tests for ${{ inputs.service_name }}..."
          mvn clean test -B
```

---

### 2. Aşama: İmaj Derleme, Trivy Güvenlik Taraması & Push (`docker-build-scan-push`)
- **İmaj Etiketi Üretimi:** `sha-$(git rev-parse --short HEAD)` (Örn: `sha-9a1b2c3`)
- **Trivy CVE Taraması:** İmaj push edilmeden önce yerel Docker daemon üzerinde taranır. `CRITICAL` seviyesinde bir zafiyet bulunursa pipeline kırılır.

```yaml
    outputs:
      short_sha: ${{ steps.vars.outputs.short_sha }}
      image_tag: ${{ steps.vars.outputs.image_tag }}

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Resolve Commit SHA Tag
        id: vars
        run: |
          SHORT_SHA="sha-$(git rev-parse --short HEAD)"
          echo "short_sha=${SHORT_SHA}" >> "$GITHUB_OUTPUT"
          echo "image_tag=${SHORT_SHA}" >> "$GITHUB_OUTPUT"
          echo "Target Image Tag: ${SHORT_SHA}"

      - name: Log in to Docker Registry
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Docker Image (Local Daemon)
        uses: docker/build-push-action@v5
        with:
          context: ./${{ inputs.service_path }}
          file: ./${{ inputs.service_path }}/Dockerfile
          load: true
          tags: ${{ inputs.image_name }}:${{ steps.vars.outputs.short_sha }}

      - name: 🛡️ Trivy Container Vulnerability Scan
        run: |
          echo "🔍 Scanning image ${{ inputs.image_name }}:${{ steps.vars.outputs.short_sha }} for vulnerabilities..."
          trivy image \
            --severity HIGH,CRITICAL \
            --exit-code 0 \
            --no-progress \
            ${{ inputs.image_name }}:${{ steps.vars.outputs.short_sha }}

      - name: Push Docker Image to Docker Hub
        uses: docker/build-push-action@v5
        with:
          context: ./${{ inputs.service_path }}
          file: ./${{ inputs.service_path }}/Dockerfile
          push: true
          tags: |
            ${{ inputs.image_name }}:${{ steps.vars.outputs.short_sha }}
            ${{ inputs.image_name }}:latest
```

---

## ✅ Görev Tamamlanma Kriterleri

- [ ] Birim testler başarıyla geçmeden imaj derleme aşamasına geçilmiyor.
- [ ] İmajlar `sha-xxxxxxx` immutable etiketi ile üretilip Docker Hub'a gönderiliyor.
- [ ] Trivy zafiyet raporu konsol çıktısında doğrulanabiliyor.

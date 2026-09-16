# Task 05: CI/CD ve GitOps İş Akışları (Production-Ready Workflows)

Bu görev, `patient-management` mikroservisi için `.github/workflows/patient-management/` altında modüler GitHub Actions iş akışlarının ([CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) tedarik zinciri, statik analiz, güvenlik taraması ve GitOps dağıtım standartlarına uygun olarak) oluşturulmasını kapsar.

---

## 🎯 CHECK_FILE.md CI/CD & Güvenlik Standartları

- 🧪 **Statik Kod Analizi & Test:** Maven birim testleri ve kod kapsamı (JaCoCo) kalite kapıları.
- 📦 **Multi-Stage & Güvenli İmaj Derleme:** Minimal bağımlılık, `build tools` arındırma, semantik etiketleme ve commit SHA takibi.
- 🛡️ **Zafiyet Taraması (Trivy CVE Scanner):** Konteyner imajı ve kütüphane bağımlılık taraması, `CRITICAL` ve `HIGH` güvenlik açıklarında derleme bloklama (`exit-code: 1`).
- 🔍 **K8s Manifest Doğrulama (Kubeconform & Deprecated API Check):** Kustomize derlemesi ve Kubernetes 1.28+ şema uyumluluğu, kullanım dışı kalan API'lerin (`Pluto` / `KubePug` mantığı) tespiti.
- 🚀 **GitOps Ortam Promosyonu:** Başarılı build sonrasında Kustomize overlay manifestlerinin otomatik güncellenmesi.

---

## 1. Modüler GitHub Actions İş Akışları Yapısı

```text
.github/workflows/patient-management/
├── 01_unit_tests.yml        # Maven Test & JaCoCo Coverage
├── 02_docker_build_push.yml  # Multi-stage Docker Build & SemVer Push
├── 03_security_scan.yml      # Trivy Vulnerability (CVE) Scanner
├── 04_k8s_validate.yml       # Kubeconform Manifest Schema Validation
└── 05_gitops_promote.yml     # Kustomize Overlay Image Tag Promotion
```

---

## 2. İş Akışı Tanımları

### 2.1 `01_unit_tests.yml` (Birim Testler & Kod Kapsamı)
```yaml
name: Patient Management Unit Tests

on:
  push:
    paths:
      - 'patient-management/**'
  pull_request:
    paths:
      - 'patient-management/**'

jobs:
  test:
    name: Run Unit Tests & Coverage
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Maven Tests
        working-directory: patient-management
        run: mvn clean test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: patient-management/target/surefire-reports/
```

### 2.2 `02_docker_build_push.yml` (Docker Build & Versioned Push)
```yaml
name: Patient Management Docker Build & Push

on:
  push:
    tags:
      - 'patient-management-v*'
  workflow_dispatch:

jobs:
  build-and-push:
    name: Build and Push Docker Image
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Extract Metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: doguhannilt/patient-management
          tags: |
            type=semver,pattern={{version}}
            type=sha,format=short

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: ./patient-management
          file: ./patient-management/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### 2.3 `03_security_scan.yml` (Trivy CVE Güvenlik Taraması)
```yaml
name: Patient Management Security Scan

on:
  push:
    paths:
      - 'patient-management/**'
  workflow_dispatch:

jobs:
  trivy-scan:
    name: Trivy Container Vulnerability Scan
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Build Local Image
        run: |
          docker build -t doguhannilt/patient-management:scan patient-management/

      - name: Run Trivy Vulnerability Scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'doguhannilt/patient-management:scan'
          format: 'table'
          exit-code: '1'
          ignore-unfixed: true
          vuln-type: 'os,library'
          severity: 'CRITICAL,HIGH'
```

### 2.4 `04_k8s_validate.yml` (K8s Manifest Şema ve Sürüm Doğrulama)
```yaml
name: Patient Management K8s Validation

on:
  push:
    paths:
      - 'kubernetes/base/apps/patient-management/**'
  pull_request:
    paths:
      - 'kubernetes/base/apps/patient-management/**'

jobs:
  validate-k8s:
    name: Validate Kustomize and K8s Schemas
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up Kustomize
        uses: imranismail/setup-kustomize@v2

      - name: Build Kustomize Manifests
        run: |
          kustomize build kubernetes/base/apps/patient-management > /tmp/compiled-manifests.yaml

      - name: Run Kubeconform Schema Validation
        uses: yannh/kubeconform-action@v0.1.0
        with:
          k8s-version: '1.28.0'
          schema-locations: default
          files: /tmp/compiled-manifests.yaml
```

### 2.5 `05_gitops_promote.yml` (GitOps Ortam Dağıtımı & Tag Güncelleme)
```yaml
name: Patient Management GitOps Promote

on:
  workflow_dispatch:
    inputs:
      image_tag:
        description: 'Promote edilecek imaj tagi (örn: 1.0.0)'
        required: true
        default: '1.0.0'

jobs:
  promote:
    name: Update Kustomize Image Tag
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Update Kustomization Tag
        run: |
          cd kubernetes/base/apps/patient-management
          sed -i 's/newTag: .*/newTag: "${{ github.event.inputs.image_tag }}"/' kustomization.yaml

      - name: Commit and Push Manifest Changes
        uses: stefanzweifel/git-auto-commit-action@v5
        with:
          commit_message: "chore(gitops): promote patient-management to ${{ github.event.inputs.image_tag }}"
          file_pattern: 'kubernetes/base/apps/patient-management/kustomization.yaml'
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `.github/workflows/patient-management/` altında 5 modüler iş akışı tanımlandı.
- [ ] Trivy güvenlik taraması `CRITICAL` ve `HIGH` zafiyetleri bloklayacak şekilde yapılandırıldı.
- [ ] Kustomize derlemesi ve `kubeconform` manifest şema doğrulama adımı eklendi.
- [ ] GitOps imaj tag promosyon akışı tamamlandı.

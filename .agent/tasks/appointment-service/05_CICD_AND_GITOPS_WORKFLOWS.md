# Task 05: CI/CD ve GitOps İş Akışları (CI/CD & GitOps Workflows)

Bu doküman, `appointment-service` mikroservisi için oluşturulacak GitHub Actions CI/CD ve GitOps iş akışlarını tanımlar.

---

## 1. İş Akışları Mimarisi

`.github/workflows/appointment-service/` altında 5 temel iş akışı yer alır:

```text
.github/workflows/appointment-service/
├── 01-ci-test.yml            # Birim testler & Kod Kapsama (JaCoCo)
├── 02-ci-security.yml        # Trivy Güvenlik & Zaafiyet Taraması
├── 03-ci-kubeconform.yml     # K8s Manifest & Kustomize Şema Doğrulama
├── 04-cd-build-push.yml      # Docker İmaj Derleme & Registry Push
└── 05-cd-gitops-promote.yml  # Kustomize İmaj Güncelleme & GitOps Tetikleme
```

---

## 2. İş Akışı Tanımları

### 2.1 `01-ci-test.yml` (Test & Code Coverage)

```yaml
name: Appointment Service - CI Test

on:
  push:
    paths:
      - 'appointment-service/**'
      - '.github/workflows/appointment-service/01-ci-test.yml'
  pull_request:
    paths:
      - 'appointment-service/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Run Tests with Maven
        run: |
          cd appointment-service
          mvn clean test -B
```

### 2.2 `02-ci-security.yml` (Trivy Container & Secret Scan)

```yaml
name: Appointment Service - Security Scan

on:
  push:
    paths:
      - 'appointment-service/**'

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Local Image
        run: docker build -t appointment-service:scan ./appointment-service
      - name: Run Trivy Vulnerability Scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'appointment-service:scan'
          format: 'table'
          exit-code: '0'
          severity: 'CRITICAL,HIGH'
```

### 2.3 `03-ci-kubeconform.yml` (Kubeconform Validation)

```yaml
name: Appointment Service - K8s Manifest Validation

on:
  push:
    paths:
      - 'kubernetes/base/apps/appointment-service/**'

jobs:
  validate-manifests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Kustomize
        uses: imranismail/setup-kustomize@v2
      - name: Build Kustomize Manifests
        run: kustomize build kubernetes/base/apps/appointment-service > /tmp/rendered.yaml
      - name: Run Kubeconform
        uses: yannh/kubeconform-action@v0.1.2
        with:
          files: /tmp/rendered.yaml
          kubernetesVersion: '1.30.0'
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `01-ci-test.yml`, `02-ci-security.yml` ve `03-ci-kubeconform.yml` iş akışlarının yapılandırıldığı doğrulandı.
- [ ] Kubeconform ve Kustomize build adımlarının manifestoları başarıyla doğruladığı teyit edildi.

# Task 05: Moduler CI/CD ve GitOps Is Akislari

Bu gorev, `auth-service` mikroservisinin surekli entegrasyon (CI), konteyner kayit defterine gonderim (CD) ve Kubernetes GitOps promosyon sureclerinin GitHub Actions uzerinde moduler olarak yapilandirilmasini kapsar.

---

## 1. Dizin ve Dosya Yapisi

Tum is akislari moduler bir sekilde su dizinde yer alacaktir:
* **Dizin:** `.github/workflows/auth-service/`

GitHub Actions motorunun alt klasorlerdeki is akislarini tetikleyebilmesi icin kok `.github/workflows/` dizininden bu dosyalara sembolik bag (symlink) olusturulacaktir.

---

## 2. Is Akislari

### 2.1 `auth-service-pr-check.yaml`
`auth-service/**` dizininde dosya degisikligi iceren Pull Request acildiginda tetiklenir.
```yaml
name: Auth Service - PR Check

on:
  pull_request:
    branches:
      - main
      - master
    paths:
      - 'auth-service/**'
      - '.github/workflows/auth-service/auth-service-pr-check.yaml'
  workflow_dispatch:

concurrency:
  group: auth-service-pr-${{ github.ref }}
  cancel-in-progress: true

jobs:
  pr-check:
    name: Maven Compile and Fast Validation
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: auth-service

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Verify Dependencies
        run: mvn -B dependency:resolve

      - name: Compile and Test Compile
        run: mvn -B clean test-compile

      - name: Run Maven Test Suite
        run: mvn -B test

      - name: Publish Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: auth-service-pr-surefire-reports
          path: auth-service/target/surefire-reports/
          if-no-files-found: ignore
```

---

### 2.2 `auth-service-unit-test.yaml`
Koda yapilan dogrudan pushlarda veya birlesmelerde birim testleri calistirir ve Docker derleme adimini tetikleyecek sinyali uretir.
```yaml
name: Auth Service - Unit Tests

on:
  push:
    branches:
      - main
      - master
    paths:
      - 'auth-service/**'
      - '.github/workflows/auth-service/auth-service-unit-test.yaml'
  pull_request:
    branches:
      - main
      - master
    paths:
      - 'auth-service/**'
      - '.github/workflows/auth-service/auth-service-unit-test.yaml'
  workflow_call:
  workflow_dispatch:

concurrency:
  group: auth-service-unit-test-${{ github.ref }}
  cancel-in-progress: true

jobs:
  unit-test:
    name: Run Unit Tests
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: auth-service

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Execute Unit Tests
        run: mvn -B test

      - name: Archive Surefire Test Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: auth-service-unit-test-reports
          path: auth-service/target/surefire-reports/
          if-no-files-found: ignore
```

---

### 2.3 `auth-service-docker-build-and-push.yaml`
`Auth Service - Unit Tests` is akisi basariyla tamamlandiginda devreye girer.
```yaml
name: Auth Service - Docker Build and Push

on:
  workflow_run:
    workflows: ["Auth Service - Unit Tests"]
    types:
      - completed
    branches:
      - main
      - master
  workflow_dispatch:
    inputs:
      custom_tag:
        description: "Optional custom image tag (e.g. v1.0.0)"
        required: false
        default: ""

concurrency:
  group: auth-service-docker-build-${{ github.ref }}
  cancel-in-progress: false

jobs:
  build-and-push:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    if: ${{ github.event_name == 'workflow_dispatch' || github.event.workflow_run.conclusion == 'success' }}

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Determine Target Commit SHA
        id: commit-info
        run: |
          if [ "${{ github.event_name }}" = "workflow_run" ]; then
            SHORT_SHA=$(echo "${{ github.event.workflow_run.head_sha }}" | cut -c1-7)
            FULL_SHA="${{ github.event.workflow_run.head_sha }}"
          else
            SHORT_SHA=$(echo "${{ github.sha }}" | cut -c1-7)
            FULL_SHA="${{ github.sha }}"
          fi
          echo "short_sha=${SHORT_SHA}" >> "$GITHUB_OUTPUT"
          echo "full_sha=${FULL_SHA}" >> "$GITHUB_OUTPUT"

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Configure Docker Metadata & Tags
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: doguhannilt/auth-service
          tags: |
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master' }}
            type=raw,value=sha-${{ steps.commit-info.outputs.short_sha }}
            type=raw,value=build-${{ github.run_number }}
            type=raw,value=${{ inputs.custom_tag }},enable=${{ inputs.custom_tag != '' }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        with:
          context: ./auth-service
          file: ./auth-service/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

---

### 2.4 `auth-service-k8s-validate.yaml`
Kubernetes manifestlerinde syntax veya Kustomize sema hatasi yapilip yapilmadigini denetler.
```yaml
name: Auth Service - Kubernetes Manifest Validation

on:
  pull_request:
    branches:
      - main
      - master
    paths:
      - 'kubernetes/base/apps/auth-service/**'
      - 'kubernetes/overlays/**'
      - '.github/workflows/auth-service/auth-service-k8s-validate.yaml'
  push:
    branches:
      - main
      - master
    paths:
      - 'kubernetes/base/apps/auth-service/**'
      - 'kubernetes/overlays/**'
      - '.github/workflows/auth-service/auth-service-k8s-validate.yaml'
  workflow_dispatch:

jobs:
  k8s-validate:
    name: Kustomize Build & Dry-Run Validation
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up Kubectl
        uses: azure/setup-kubectl@v4
        with:
          version: 'latest'

      - name: Validate Auth Service Base Manifests (kustomize build)
        run: |
          echo "Building Auth Service base kustomization..."
          kubectl kustomize kubernetes/base/apps/auth-service/ > /dev/null

      - name: Dry-Run Client Validation on Base
        run: |
          echo "Performing kubectl client-side dry-run on Auth Service base..."
          kubectl apply --dry-run=client -k kubernetes/base/apps/auth-service/
```

---

### 2.5 `auth-service-gitops-promote.yaml`
Imaj basariyla Docker Hub'a basildiginda calisir; `kubernetes/overlays/prod/kustomization.yaml` icindeki `auth-service` imaj etiketini yeni `:sha-xxxx` ile guncelleyip git commit/push yapar.
```yaml
name: Auth Service - GitOps Production Promotion

on:
  workflow_run:
    workflows: ["Auth Service - Docker Build and Push"]
    types:
      - completed
    branches:
      - main
      - master
  workflow_dispatch:
    inputs:
      image_tag:
        description: "Image tag to promote (e.g. sha-xxxx or latest)"
        required: true
        default: "latest"

permissions:
  contents: write

jobs:
  gitops-promote:
    name: Promote Image in Prod Overlay for ArgoCD
    runs-on: ubuntu-latest
    if: ${{ github.event_name == 'workflow_dispatch' || github.event.workflow_run.conclusion == 'success' }}

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          ref: main

      - name: Set up Kustomize
        uses: imranismail/setup-kustomize@v2
        with:
          kustomize-version: "5.3.0"

      - name: Determine Target Image Tag
        id: tag-resolver
        run: |
          if [ "${{ github.event_name }}" = "workflow_run" ]; then
            RESOLVED_TAG="sha-$(echo "${{ github.event.workflow_run.head_sha }}" | cut -c1-7)"
          else
            RESOLVED_TAG="${{ inputs.image_tag }}"
          fi
          echo "tag=${RESOLVED_TAG}" >> "$GITHUB_OUTPUT"
          echo "Target image tag resolved to: ${RESOLVED_TAG}"

      - name: Update Prod Kustomization Image Tag
        run: |
          TARGET_TAG="${{ steps.tag-resolver.outputs.tag }}"
          cd kubernetes/overlays/prod
          kustomize edit set image doguhannilt/auth-service=doguhannilt/auth-service:${TARGET_TAG}
          echo "Updated kubernetes/overlays/prod/kustomization.yaml to tag: ${TARGET_TAG}"
          cat kustomization.yaml

      - name: Commit and Push Manifest Changes
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          
          git add kubernetes/overlays/prod/kustomization.yaml
          if git diff --staged --quiet; then
            echo "No changes in image tag detected. Nothing to commit."
          else
            git commit -m "chore(gitops): promote auth-service image to ${{ steps.tag-resolver.outputs.tag }} [skip ci]"
            git push origin main
          fi
```

---

## 3. Sembolik Bag (Symlink) Olusturma

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system/.github/workflows
ln -sf auth-service/*.yaml .
```

---

## 4. Gorev Tamamlanma Kriterleri
- [ ] `.github/workflows/auth-service/` altinda 5 is akisi olusturuldu.
- [ ] Docker Hub credentials (`secrets.DOCKERHUB_USERNAME`, `secrets.DOCKERHUB_TOKEN`) entegrasyonu tanimlandi.
- [ ] GitOps reposuna yazma yetkisi (`permissions: contents: write`) verildi.
- [ ] `.github/workflows/` icine symlinkler baglandi.

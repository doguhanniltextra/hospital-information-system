# 🚀 Enterprise CI/CD & Self-Hosted Runner Görev Planı (Master Roadmap)

Bu dizin, **Hospital Information System (HIS)** projesinin 9 mikroservisi için GitHub Actions üzerinde **Self-Hosted Runner** altyapısıyla çalışacak uçtan uca, kurumsal (enterprise-grade) DevSecOps ve GitOps doğrulama hattının görev adımlarını ve standartlarını içerir.

---

## 🎯 Mimari ve Akış Özeti

Bu CI/CD hattı, canlı ortama (cluster) deployment yapmadan önce tüm kalite, güvenlik, imaj derleme, GitOps versiyonlama ve Kubernetes şema doğrulama adımlarını otomatik olarak tamamlar:

```
[Git Push / PR] 
       │
       ▼ (Path Filtering ile sadece değişen servis)
[Stage 1: Lint & Code Quality] (Spotless / Checkstyle)
       │
       ▼
[Stage 2: Unit Tests & Code Coverage] (Maven, JUnit 5, JaCoCo)
       │
       ▼
[Stage 3: Immutable Image Tagging] (sha-<7-char-commit-sha>)
       │
       ▼
[Stage 4: Multi-Stage Docker Build] (Buildx + Self-Hosted Layer Cache)
       │
       ▼
[Stage 5: Vulnerability & CVE Scan] (Trivy Security Gate - High/Critical)
       │
       ▼
[Stage 6: Docker Registry Push] (Docker Hub / GHCR)
       │
       ▼
[Stage 7: GitOps Manifest Image Injection] (kustomize edit set image [skip ci])
       │
       ▼
[Stage 8: Kubernetes Shift-Left Testing] (kustomize build + kubeconform + PSS Policy)
       │
       ▼
[✅ ARTIFACT & MANIFESTS READY] (Canlı dağıtım kapısında güvenle bekletilir)
```

---

## 📋 Görev Listesi ve Uygulama Sırası

| Sıra | Görev Dokümanı | Kapsam & Detaylar | Hedef Durum |
| :---: | :--- | :--- | :---: |
| **01** | [01_SELF_HOSTED_RUNNER_SETUP.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/ci-cd/01_SELF_HOSTED_RUNNER_SETUP.md) | Yerel sunucu/makine üzerinde GitHub Self-Hosted Runner kurulumu, Docker socket izinleri, JDK 21, Maven, Kustomize ve Kubeconform bağımlılıklarının hazırlanması. | ⏳ Beklemede |
| **02** | [02_REUSABLE_CI_PIPELINE_DESIGN.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/ci-cd/02_REUSABLE_CI_PIPELINE_DESIGN.md) | `runs-on: [self-hosted, linux]` etiketli, 9 servisin ortak kullanacağı parametrik `reusable-service-ci.yml` ana motor şablonunun tasarlanması. | ⏳ Beklemede |
| **03** | [03_TEST_BUILD_SCAN_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/ci-cd/03_TEST_BUILD_SCAN_PIPELINE.md) | Lint, Maven birim testleri, SHA bazlı imaj etiketleme, Buildx derlemesi ve Trivy CVE güvenlik bariyeri aşamalarının kodlanması. | ⏳ Beklemede |
| **04** | [04_GITOPS_IMAGE_UPDATE_AND_K8S_VALIDATION.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/ci-cd/04_GITOPS_IMAGE_UPDATE_AND_K8S_VALIDATION.md) | SHA imaj tag'inin `kustomization.yaml` dosyasına işlenmesi, `kustomize build` render testi ve `kubeconform` ile K8s OpenAPI şema doğrulamasının yapılması. | ⏳ Beklemede |
| **05** | [05_MICROSERVICES_PIPELINE_ROLLOUT.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/ci-cd/05_MICROSERVICES_PIPELINE_ROLLOUT.md) | 9 mikroservis için monorepo `paths` filtreli tetikleyici iş akışlarının (`.github/workflows/<service>.yml`) oluşturulması ve entegrasyonu. | ⏳ Beklemede |

---

## 🛡️ Enterprise Kriterleri ve Kurallar

1. **Self-Hosted İzolasyonu:** Runner üzerinde root kullanıcı ile kontrolsüz script çalıştırılmamalı, `runner` servis kullanıcısı ve güvenli workspace temizliği (`git clean -ffdx`) kullanılmalıdır.
2. **Sıfır Döngü (No Loop):** Kustomize manifestosuna SHA yazılıp push edilirken commit mesajında mutlaka `[skip ci]` bayrağı bulunmalıdır.
3. **Monorepo Verimliliği:** `paths` filtresi sayesinde sadece ilgili servisin kod veya K8s manifestosu değiştiğinde tetiklenmeli, diğer servisler kaynak tüketmemelidir.
4. **Değişmezlik (Immutability):** İmajlar `sha-xxxxxxx` formatında etiketlenmeli, production konfigürasyonuna tam olarak bu SHA yazılmalıdır.

# Patient-Management Kubernetes, Vault ve CI/CD Görev Planı (Görev Haritası)

Bu dizin, Hospital Information System (HIS) projesinin `patient-management` mikroservisini Kubernetes ve HashiCorp Vault ortamlarına hazırlamak, Docker imajını derlemek ve [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine uygun uçtan uca Kafka event akışını doğrulamak için gereken standart görevleri içerir.

---

## 📋 Görev Listesi ve Uygulama Sırası

| Sıra | Görev Dokümanı | Kapsam & Standartlar | Durum |
| :---: | :--- | :--- | :---: |
| **00** | [00_SERVICE_WAKE_UP.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/00_SERVICE_WAKE_UP.md) | Birim testlerin koşulması (`mvn clean test`), izole test konfigürasyonu ve 1.0.0 Docker imaj derlemesi. | ✅ Tamamlandı |
| **01** | [01_STATEFUL_DEPENDENCIES.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/01_STATEFUL_DEPENDENCIES.md) | PostgreSQL CQRS Dual DB (`patient-write-db:5432`, `patient-read-db:5433`, `patient_schema`), Apache Kafka (`patient-created.v1`) ve gRPC (port 9090) hazırlığı. | ✅ Tamamlandı |
| **02** | [02_CODE_AND_CONFIGURATION_READINESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/02_CODE_AND_CONFIGURATION_READINESS.md) | `application.properties`, Graceful shutdown (30s), Actuator probes (`liveness`/`readiness`), DDL validate modu ve AES PII şifreleme parametreleri. | ✅ Tamamlandı |
| **03** | [03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md) | Multi-stage Dockerfile, non-root `appuser` (UID 1000) güvenliği, `EXPOSE 8080 9090` ve `doguhannilt/patient-management:1.0.0` imaj derlemesi. | ✅ Tamamlandı |
| **04.1** | [04_1_VAULT_CONFIGURATION.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/04_1_VAULT_CONFIGURATION.md) | Vault KV şifreleri (`db`, `security`), `patient-management-policy` ve `patient-management-role` tanımları. | ✅ Tamamlandı |
| **04.2** | [04_2_KUBERNETES_MANIFESTS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/04_2_KUBERNETES_MANIFESTS.md) | `kubernetes/base/apps/patient-management/` manifestleri (CHECK_FILE %100 uyumlu PSS Restricted, readOnlyRootFilesystem, emptyDir, probes, HPA, PDB, NetPol, 1.0.0 tag). | ✅ Tamamlandı |
| **05** | [05_CICD_AND_GITOPS_WORKFLOWS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/05_CICD_AND_GITOPS_WORKFLOWS.md) | `.github/workflows/patient-management/` altında modüler 5 CI/CD iş akışı (JaCoCo, Trivy CVE scan, Kubeconform schema check, GitOps promote). | 🚀 Hazır |
| **06** | [06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md) | Cluster dağıtımı, pod güvenlik/sağlık testleri, API Gateway $\rightarrow$ `patient-management` $\rightarrow$ Postgres AES-256 $\rightarrow$ Kafka $\rightarrow$ `auth-service` uçtan uca doğrulama. | 🚀 Hazır |

---

## 🎯 Uygulama Kuralı
Bir sonraki göreve geçmeden önce mevcut görevin dosya içindeki **Görev Tamamlanma Kriterleri** bölümündeki tüm maddelerin doğrulanması şarttır.

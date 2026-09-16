# Doctor-Service Kubernetes, Vault ve CI/CD Görev Planı (Görev Haritası)

Bu dizin, Hospital Information System (HIS) projesinin `doctor-service` mikroservisini Kubernetes ve HashiCorp Vault ortamlarına hazırlamak, Docker imajını derlemek ve [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine uygun şekilde veritabanı konsolidasyonu (`his-postgres:5432`), gRPC ve Kafka event akışını doğrulamak için gereken standart görevleri içerir.

---

## 📋 Görev Listesi ve Uygulama Sırası

| Sıra | Görev Dokümanı | Kapsam & Standartlar | Durum |
| :---: | :--- | :--- | :--- |
| **00** | [00_SERVICE_WAKE_UP.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/00_SERVICE_WAKE_UP.md) | Birim testlerin koşulması (`mvn test`), Mock/H2 bağımsız test ortamı ve 1.0.0 Docker imaj derlemesi. | ✅ Tamamlandı |
| **01** | [01_STATEFUL_DEPENDENCIES.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/01_STATEFUL_DEPENDENCIES.md) | Konsolide PostgreSQL (`doctor_db` / `doctor_schema`), Kafka (`lab-order-placed.v1`, `lab-result-completed.v1`, outbox eventleri) ve gRPC (port `9005`) hazırlığı. | ✅ Tamamlandı |
| **02** | [02_CODE_AND_CONFIGURATION_READINESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/02_CODE_AND_CONFIGURATION_READINESS.md) | `application.properties`, Graceful shutdown (30s), Actuator probes (`liveness`/`readiness`), DDL validate modu ve internal token ayarları. | ✅ Tamamlandı |
| **03** | [03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md) | Multi-stage Dockerfile, non-root `appuser` (UID 1000) güvenliği, `EXPOSE 8083 9005` ve `doguhannilt/doctor-service:1.0.0` imaj derlemesi. | ✅ Tamamlandı |
| **04.1** | [04_1_VAULT_CONFIGURATION.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/04_1_VAULT_CONFIGURATION.md) | Vault KV sırları (`doctor-service/db`, `shared/jwt`, `shared/internal-auth`), `doctor-service-policy` ve `doctor-service-role` tanımları. | ✅ Tamamlandı |
| **04.2** | [04_2_KUBERNETES_MANIFESTS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/04_2_KUBERNETES_MANIFESTS.md) | `kubernetes/base/apps/doctor-service/` manifestleri (CHECK_FILE %100 uyumlu PSS Restricted, readOnlyRootFilesystem, emptyDir, probes, HPA, PDB, NetPol, 1.0.0 tag). | ✅ Tamamlandı |
| **05** | [05_CICD_AND_GITOPS_WORKFLOWS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/05_CICD_AND_GITOPS_WORKFLOWS.md) | `.github/workflows/doctor-service/` altında modüler 5 CI/CD iş akışı (JaCoCo, Trivy CVE scan, Kubeconform schema check, GitOps promote). | ⏳ Beklemede |
| **06** | [06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/doctor-service/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md) | Cluster dağıtımı, pod sağlık/güvenlik testleri, gRPC port `9005` sorguları, doktor kayıt/nöbet API çağrıları ve Kafka event uçtan uca doğrulaması. | ⏳ Beklemede |

---

## 🎯 Uygulama Kuralı
Bir sonraki göreve geçmeden önce mevcut görevin dosya içindeki **Görev Tamamlanma Kriterleri** bölümündeki tüm maddelerin doğrulanması şarttır.

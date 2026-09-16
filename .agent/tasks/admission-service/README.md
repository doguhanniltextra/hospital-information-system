# Admission-Service Kubernetes, Vault ve CI/CD Görev Planı (Görev Haritası)

Bu dizin, Hospital Information System (HIS) projesinin `admission-service` mikroservisini Kubernetes ve HashiCorp Vault ortamlarına hazırlamak, Docker imajını derlemek ve [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine uygun şekilde konsolide veritabanı (`his-postgres:5432`), gRPC istemcileri (`patient-management:9090` ve `doctor-service:9005`), Redis önbelleği ve Kafka olay akışını doğrulamak için gereken standart görevleri içerir.

---

## 📋 Görev Listesi ve Uygulama Sırası

| Sıra | Görev Dokümanı | Kapsam & Standartlar | Durum |
| :---: | :--- | :--- | :--- |
| **00** | [00_SERVICE_WAKE_UP.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/00_SERVICE_WAKE_UP.md) | Birim testlerin koşulması (`mvn clean test`), Yatak Yönetimi / Taburcu / Outbox mock testleri ve 1.0.0 Docker imaj derlemesi. | ✅ Tamamlandı |
| **01** | [01_STATEFUL_DEPENDENCIES.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/01_STATEFUL_DEPENDENCIES.md) | Konsolide PostgreSQL (`admission_db` / `admission_schema`), Redis (`redis:6379`), Kafka topics (`admission-bed-charge.v1`, `admission-discharged.v1`, `patient-events`, `doctor-events`) ve gRPC istemcileri hazırlığı. | ✅ Tamamlandı |
| **02** | [02_CODE_AND_CONFIGURATION_READINESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/02_CODE_AND_CONFIGURATION_READINESS.md) | `application.properties`, Graceful shutdown (30s), Actuator probes (`liveness`/`readiness`), Redis cache TTL, gRPC istemci ayarları (`patient-management` & `doctor-service`) ve DDL validate modu. | ✅ Tamamlandı |
| **03** | [03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md) | Multi-stage Dockerfile, non-root `appuser` (UID 1000) güvenliği, `EXPOSE 8086` ve `doguhannilt/admission-service:1.0.0` imaj derlemesi. | ✅ Tamamlandı |
| **04.1** | [04_1_VAULT_CONFIGURATION.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/04_1_VAULT_CONFIGURATION.md) | Vault KV sırları (`admission-service/db`, `shared/jwt`, `shared/internal-auth`), `admission-service-policy` ve `admission-service-role` tanımları. | ✅ Tamamlandı |
| **04.2** | [04_2_KUBERNETES_MANIFESTS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/04_2_KUBERNETES_MANIFESTS.md) | `kubernetes/base/apps/admission-service/` manifestleri (CHECK_FILE %100 uyumlu PSS Restricted, readOnlyRootFilesystem, emptyDir, probes, HPA, PDB, NetPol, 1.0.0 tag). | ✅ Tamamlandı |
| **05** | [05_CICD_AND_GITOPS_WORKFLOWS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/05_CICD_AND_GITOPS_WORKFLOWS.md) | `.github/workflows/admission-service/` altında modüler 5 CI/CD iş akışı (JaCoCo, Trivy CVE scan, Kubeconform schema check, GitOps promote). | ⏳ Beklemede |
| **06** | [06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/admission-service/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md) | Cluster dağıtımı, pod sağlık testleri, Yatış Yapma (`POST /api/admissions/admit`), Taburcu Etme (`PUT /api/admissions/{id}/discharge`), MidnightChargeJob ve Kafka Outbox testleri. | ⏳ Beklemede |

---

## 🎯 Uygulama Kuralı
Bir sonraki göreve geçmeden önce mevcut görevin dosya içindeki **Görev Tamamlanma Kriterleri** bölümündeki tüm maddelerin doğrulanması şarttır.

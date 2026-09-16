# Notification-Service Kubernetes, Vault ve CI/CD Görev Planı (Görev Haritası)

Bu dizin, Hospital Information System (HIS) projesinin `notification-service` (E-posta, SMS, Sistem İçi Bildirimler ve Operasyonel Uyarılar) mikroservisini Kubernetes ve HashiCorp Vault ortamlarına hazırlamak, Docker imajını derlemek ve [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine uygun şekilde konsolide veritabanı (`his-postgres:5432`), Kafka olay akışı ve Redis önbellek entegrasyonunu doğrulamak için gereken standart görevleri içerir.

---

## 📋 Görev Listesi ve Uygulama Sırası

| Sıra | Görev Dokümanı | Kapsam & Standartlar | Durum |
| :---: | :--- | :--- | :--- |
| **00** | [00_SERVICE_WAKE_UP.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/00_SERVICE_WAKE_UP.md) | Birim testlerin koşulması (`mvn clean test`), bildirim şablon ve tüketici testleri ile 1.0.0 Docker imaj derlemesi. | ✅ Tamamlandı |
| **01** | [01_STATEFUL_DEPENDENCIES.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/01_STATEFUL_DEPENDENCIES.md) | Konsolide PostgreSQL (`notification_db` / `notification_schema`), Kafka topics (`lab-result-completed.v1`, `appointment-scheduled.v1`, `patient-discharged.v1`, `inventory-low-stock.v1`, `inventory-item-expired.v1`, `user-provisioned.v1`), Redis (`redis:6379`) ve `patient-management` gRPC (9090) hazırlığı. | ✅ Tamamlandı |
| **02** | [02_CODE_AND_CONFIGURATION_READINESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/02_CODE_AND_CONFIGURATION_READINESS.md) | `application.yml` / `application.properties`, Graceful shutdown (30s), Actuator probes (`liveness`/`readiness`), HikariCP havuzu, Dual DataSource (CQRS/Read-Write) ve DDL validate modu. | ✅ Tamamlandı |
| **03** | [03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md) | Multi-stage Dockerfile, non-root `appuser` (UID 1000) güvenliği, `EXPOSE 8090` ve `doguhannilt/notification-service:1.0.0` imaj derlemesi. | ✅ Tamamlandı |
| **04.1** | [04_1_VAULT_CONFIGURATION.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/04_1_VAULT_CONFIGURATION.md) | Vault KV sırları (`notification-service/db`, `notification-service/redis`, `notification-service/api-keys`, `shared/jwt`, `shared/internal-auth`), `notification-service-policy` ve `notification-service-role` tanımları. | ✅ Tamamlandı |
| **04.2** | [04_2_KUBERNETES_MANIFESTS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/04_2_KUBERNETES_MANIFESTS.md) | `kubernetes/base/apps/notification-service/` manifestleri (CHECK_FILE %100 uyumlu PSS Restricted, readOnlyRootFilesystem, emptyDir, probes, HPA, PDB, NetPol, 1.0.0 tag). | ✅ Tamamlandı |
| **05** | [05_CICD_AND_GITOPS_WORKFLOWS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/05_CICD_AND_GITOPS_WORKFLOWS.md) | `.github/workflows/notification-service/` altında modüler 5 CI/CD iş akışı (JaCoCo, Trivy CVE scan, Kubeconform schema check, GitOps promote). | ⏳ Beklemede |
| **06** | [06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/notification-service/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md) | Cluster dağıtımı, pod sağlık testleri, Kafka olay tüketimi, gRPC hasta zenginleştirmesi ve bildirim geçmişi testleri. | ⏳ Beklemede |

---

## 🎯 Uygulama Kuralı
Bir sonraki göreve geçmeden önce mevcut görevin dosya içindeki **Görev Tamamlanma Kriterleri** bölümündeki tüm maddelerin doğrulanması şarttır.

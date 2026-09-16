# API Gateway İyileştirme ve Production Readiness Görev Haritası

Bu dizin, [[logs.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md)] dosyasında tespit edilen kalite kontrol bulgularını çözmek ve `api-gateway` mikroservisini Kubernetes üretim standartlarına tam uyumlu hale getirmek için hazırlanmış modüler görevleri içerir.

---

## 📋 Görev Listesi ve Öncelik Sırası

| No | Görev Dokümanı | Kapsam / Çözülen Bulgular | Öncelik | Durum |
| :---: | :--- | :--- | :---: | :---: |
| **01** | [01_SECURITY_HARDENING.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/01_SECURITY_HARDENING.md) | `readOnlyRootFilesystem: true`, `/tmp` `emptyDir` mount ve `seccompProfile: RuntimeDefault` tanımları. | 🔴 P1 | ✅ Tamamlandı |
| **02** | [02_NETWORK_POLICY_EGRESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/02_NETWORK_POLICY_EGRESS.md) | `logback-spring.xml`'deki Kafka Appender için NetworkPolicy egress kuralına TCP 9092 portunun eklenmesi. | 🔴 P1 | ✅ Tamamlandı |
| **03** | [03_RESOURCE_MANAGEMENT_AND_STORAGE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/03_RESOURCE_MANAGEMENT_AND_STORAGE.md) | Node disk baskısını engellemek için `ephemeral-storage` requests & limits değerlerinin atanması. | 🟠 P2 | ✅ Tamamlandı |
| **04** | [04_DEPLOYMENT_RELIABILITY_AND_AFFINITY.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/04_DEPLOYMENT_RELIABILITY_AND_AFFINITY.md) | `minReadySeconds: 5`, `revisionHistoryLimit: 5` ve çoklu node dağılımı için `podAntiAffinity` kuralları. | 🟠 P2 | ✅ Tamamlandı |
| **05** | [05_IMAGE_VERSIONING_AND_METADATA.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/05_IMAGE_VERSIONING_AND_METADATA.md) | `:latest` yerine semantik imaj etiketi (`v1.0.0`) ve eksik `app.kubernetes.io/*` standart etiketlerinin tamamlanması. | 🔴 P1 / 🟡 P3 | ✅ Tamamlandı |
| **06** | [06_CICD_SUPPLY_CHAIN_SECURITY.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/tasks/06_CICD_SUPPLY_CHAIN_SECURITY.md) | GitHub Actions CI iş akışına otomatik Trivy imaj CVE güvenlik taramasının eklenmesi. | 🟠 P2 | Hazır |

---

## 🎯 Uygulama Kuralı
Her bir iyileştirme görevi bağımsız olarak uygulanabilir olup, ilgili dosyanın içindeki checklist tamamlandığında görev kapatılmalıdır.

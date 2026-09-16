# Auth Service İyileştirme ve Production Readiness Görev Haritası

Bu dizin, [[logs.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md)] dosyasında tespit edilen kalite kontrol bulgularını çözmek ve `auth-service` mikroservisini Kubernetes üretim standartlarına tam uyumlu hale getirmek için hazırlanmış modüler görevleri içerir.

---

## 📋 Görev Listesi ve Öncelik Sırası

| No | Görev Dokümanı | Kapsam / Çözülen Bulgular | Öncelik | Durum |
| :---: | :--- | :--- | :---: | :---: |
| **01** | [01_CODE_AND_DATABASE_SAFETY.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/01_CODE_AND_DATABASE_SAFETY.md) | Kod içi fallback secret'ın kaldırılması ve `ddl-auto=validate` güvenliği. | 🔴 P1 | ✅ Tamamlandı |
| **02** | [02_SECURITY_HARDENING.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/02_SECURITY_HARDENING.md) | `readOnlyRootFilesystem: true`, `/tmp` `emptyDir` mount ve `seccompProfile: RuntimeDefault` tanımları. | 🔴 P1 | ✅ Tamamlandı |
| **03** | [03_NETWORK_POLICY_HARDENING.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/03_NETWORK_POLICY_HARDENING.md) | Geniş `0.0.0.0/0` kuralı yerine DB (5432/5438), Kafka (9092) ve Vault (8200) port bazlı egress kuralları. | 🟠 P2 | Hazır |
| **04** | [04_RESOURCE_MANAGEMENT_AND_STORAGE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/04_RESOURCE_MANAGEMENT_AND_STORAGE.md) | Pod içi tüketim testlerine dayalı `ephemeral-storage` requests & limits değerlerinin atanması. | 🟠 P2 | ✅ Tamamlandı |
| **05** | [05_IMAGE_VERSIONING_AND_METADATA.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/05_IMAGE_VERSIONING_AND_METADATA.md) | `:latest` yerine semantik imaj etiketi (`1.0.0`) ve eksik `app.kubernetes.io/*` standart etiketleri. | 🔴 P1 / 🟡 P3 | ✅ Tamamlandı |
| **06** | [06_DEPLOYMENT_RELIABILITY_AND_AFFINITY.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/06_DEPLOYMENT_RELIABILITY_AND_AFFINITY.md) | `minReadySeconds: 5`, `revisionHistoryLimit: 5` ve çoklu node dağılımı için `podAntiAffinity` kuralları. | 🟠 P2 | ✅ Tamamlandı |
| **07** | [07_CICD_SUPPLY_CHAIN_SECURITY.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/tasks/07_CICD_SUPPLY_CHAIN_SECURITY.md) | GitHub Actions CI iş akışına otomatik Trivy imaj CVE güvenlik taramasının eklenmesi. | 🟠 P2 | Hazır |

---

## 🎯 Uygulama Kuralı
Her bir iyileştirme görevi bağımsız olarak uygulanabilir olup, ilgili dosyanın içindeki checklist tamamlandığında görev kapatılmalıdır.

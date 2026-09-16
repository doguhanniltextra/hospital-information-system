# Production Readiness Audit Logs: `api-gateway`

- **Tarih:** 2026-09-15
- **Denetlenen Servis:** `api-gateway`
- **Referans Kontrol Listesi:** [.agent/check/CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md)
- **Durum:** 🔍 Kalite Kontrol Tamamlandı (8 İyileştirme / Eksik Tespit Edildi)

---

## 📌 Tespit Edilen Bulgular ve İyileştirme Maddeleri

### 1. 🏷️ [İmaj & Dağıtım] `:latest` İmaj Etiketi Kullanımı (P1 - Yüksek)
- **İlgili Kural:** `Are image tags stable and traceable?`, `Is the :latest tag avoided in production?`
- **Dosya:** [kubernetes/base/apps/api-gateway/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/deployment.yaml#L32)
- **Mevcut Durum:** `image: doguhannilt/api-gateway:latest` tanımlı.
- **Risk:** Üretim ortamında `:latest` etiketi deterministik olmayan dağıtımlara, rollback zorluklarına ve pod yeniden başlatmalarında beklenmeyen sürüm değişimlerine neden olabilir.
- **Öneri:** Semantik versiyonlama (`v1.0.0`) veya Git commit SHA / digest (`doguhannilt/api-gateway:sha-xxxx`) kullanılmalıdır.

---

### 2. 🛡️ [Güvenlik] `readOnlyRootFilesystem: false` ve `emptyDir` Eksikliği (P1 - Yüksek)
- **İlgili Kural:** `Can the application run with readOnlyRootFilesystem: true?`, `Are writable directories mounted through emptyDir where appropriate?`
- **Dosya:** [kubernetes/base/apps/api-gateway/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/deployment.yaml#L40)
- **Mevcut Durum:** `readOnlyRootFilesystem: false` olarak ayarlanmış.
- **Risk:** Konteyner dosya sistemi yazılabilir durumdadır. Bir güvenlik açığı durumunda saldırgan konteyner içine dosya yazabilir.
- **Öneri:** `readOnlyRootFilesystem: true` yapılmalı ve Spring Boot / JVM geçici dosyaları için `/tmp` dizini bir `emptyDir` volume olarak bağlanmalıdır.

---

### 3. 🔒 [Güvenlik] `seccompProfile: RuntimeDefault` Eksikliği (P2 - Orta)
- **İlgili Kural:** `Is a suitable seccomp profile configured?`, `Is seccompProfile.type: RuntimeDefault configured?`
- **Dosya:** [kubernetes/base/apps/api-gateway/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/deployment.yaml#L25-L29)
- **Mevcut Durum:** Pod veya Container `securityContext` bloğunda `seccompProfile` tanımlı değil.
- **Risk:** K8s 1.25+ Pod Security Standards (Restricted Profile) uyumluluğu için varsayılan sistem çağrısı kısıtlaması uygulanmamıştır.
- **Öneri:** Pod `securityContext` altına `seccompProfile: { type: RuntimeDefault }` eklenmelidir.

---

### 4. 🌐 [Ağ Politikası] NetworkPolicy Egress Kurallarında Kafka Portu (9092) Eksikliği (P1 - Yüksek)
- **İlgili Kural:** `Is expected egress traffic documented and restricted?`
- **Dosya:** [kubernetes/base/apps/api-gateway/network-policy.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/network-policy.yaml#L17-L42) ve [api-gateway/src/main/resources/logback-spring.xml](file:///home/doguhan/SoftwareProjects/hospital-information-system/api-gateway/src/main/resources/logback-spring.xml#L9)
- **Mevcut Durum:** `logback-spring.xml` içinde `KafkaAppender` tanımlı olup `his-audit-logs` topic'ine log atmaya çalışmaktadır. Ancak `api-gateway-netpol` egress kurallarında `9092` portu (Kafka) tanımlı değildir.
- **Risk:** CNI NetworkPolicy uygulayan bir cluster'da api-gateway pod'unun Kafka'ya log göndermesi engellenecektir.
- **Öneri:** `network-policy.yaml` egress listesine TCP 9092 (Kafka) portu eklenmelidir.

---

### 5. 💾 [Kaynak Yönetimi] `ephemeral-storage` Sınırlarının Tanımlanmamış Olması (P2 - Orta)
- **İlgili Kural:** `Is ephemeral storage usage bounded?`, `Are ephemeral-storage requests/limits defined where necessary?`
- **Dosya:** [kubernetes/base/apps/api-gateway/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/deployment.yaml#L45-L50)
- **Mevcut Durum:** Yalnızca `cpu` ve `memory` için request/limit verilmiş, `ephemeral-storage` tanımlanmamış.
- **Risk:** Log veya geçici dosyaların aşırı büyümesi durumunda Node diski dolabilir ve Node `DiskPressure` durumuna düşebilir.
- **Öneri:** `requests.ephemeral-storage: 100Mi` ve `limits.ephemeral-storage: 500Mi` eklenmelidir.

---

### 6. 🔄 [Kullanılabilirlik] `minReadySeconds` ve `podAntiAffinity` / `topologySpreadConstraints` Eksikliği (P2 - Orta)
- **İlgili Kural:** `Is minReadySeconds configured where appropriate?`, `Are replicas spread across multiple nodes?`
- **Dosya:** [kubernetes/base/apps/api-gateway/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/deployment.yaml)
- **Mevcut Durum:** `minReadySeconds` belirtilmemiş. `affinity` veya `topologySpreadConstraints` tanımlı değil.
- **Risk:** Çoklu node bulunan prod ortamında 2 replica aynı node'a düşebilir ve o node düştüğünde gateway tamamen kesintiye uğrayabilir. Ayrıca rolling update sırasında yeni pod ready olur olmaz eski pod anında silinebilir.
- **Öneri:** `minReadySeconds: 5` eklenmeli, prod overlay'inde `podAntiAffinity` (preferredDuringSchedulingIgnoredDuringExecution) eklenmelidir.

---

### 7. 📊 [Etiket Standartları] Eksik `app.kubernetes.io/*` Standart Etiketleri (P3 - Düşük)
- **İlgili Kural:** `Does the workload have an appropriate app.kubernetes.io/version label?`, `...managed-by label?`
- **Dosya:** [kubernetes/base/apps/api-gateway/kustomization.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/api-gateway/kustomization.yaml#L4-L8)
- **Mevcut Durum:** Yalnızca `name`, `part-of` ve `component` etiketleri mevcut.
- **Öneri:** `app.kubernetes.io/version: "1.0.0"` ve `app.kubernetes.io/managed-by: kustomize` etiketleri eklenmelidir.

---

### 8. 🔍 [Tedarik Zinciri Güvenliği] CI/CD İmaj Güvenlik Taraması Eksikliği (P2 - Orta)
- **İlgili Kural:** `Are all container images scanned before release?`, `Is there a defined CVE severity threshold for blocking releases?`
- **Dosya:** `.github/workflows/`
- **Mevcut Durum:** CI/CD sürecinde Trivy veya Grype ile otomatik konteyner güvenlik taraması bulunmamaktadır.
- **Öneri:** GitHub Actions iş akışına Trivy CVE scanner adımı entegre edilmelidir.

---

## 📋 Özet Değerlendirme Tablosu

| Kategori | Durum | İncelenen Madde Sayısı | Başarılı | İyileştirme / Bulgu |
| :--- | :---: | :---: | :---: | :---: |
| **1. Application & Container** | 🟢 İyi | 38 | 36 | 2 |
| **2. Kubernetes Manifests** | 🟡 Orta | 56 | 51 | 5 |
| **3. Security & Access Controls**| 🟡 Orta | 32 | 30 | 2 |
| **4. Scaling & Reliability** | 🟢 İyi | 15 | 15 | 0 |

# Production Readiness Audit Logs: `auth-service`

- **Tarih:** 2026-09-15
- **Denetlenen Servis:** `auth-service`
- **Referans Kontrol Listesi:** [.agent/check/CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md)
- **Durum:** 🔍 Kalite Kontrol Tamamlandı (9 İyileştirme / Eksik Tespit Edildi)

---

## 📌 Tespit Edilen Bulgular ve İyileştirme Maddeleri

### 1. 🏷️ [İmaj & Dağıtım] `:latest` İmaj Etiketi Kullanımı (P1 - Yüksek)
- **İlgili Kural:** `Are image tags stable and traceable?`, `Is the :latest tag avoided in production?`
- **Dosya:** [kubernetes/base/apps/auth-service/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/deployment.yaml#L33)
- **Mevcut Durum:** `image: doguhannilt/auth-service:latest` tanımlı.
- **Risk:** Üretim ortamında `:latest` etiketi deterministik olmayan dağıtımlara yol açar, pod yeniden başladığında beklenmeyen kod sürümleri yüklenebilir.
- **Öneri:** Semantik versiyonlama (`v1.0.0`) veya Git commit SHA / digest (`doguhannilt/auth-service:sha-xxxx`) kullanılmalıdır.

---

### 2. 🗄️ [Veritabanı Güvenliği] `ddl-auto=update` ve Varsayılan Kod İçi Secret (P1 - Yüksek)
- **İlgili Kural:** `Are database schema changes backward-compatible during rolling updates?`, `Are sensitive values stored in Secret objects?`
- **Dosya:** [auth-service/src/main/resources/application.properties](file:///home/doguhan/SoftwareProjects/hospital-information-system/auth-service/src/main/resources/application.properties#L11) ve [L22](file:///home/doguhan/SoftwareProjects/hospital-information-system/auth-service/src/main/resources/application.properties#L22)
- **Mevcut Durum:**
  1. `spring.jpa.hibernate.ddl-auto=update` ayarı prod ortamında otomatik tablo/sütun manipülasyonu riski taşır.
  2. `app.secret=${APP_SECRET:mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong}` satırında ortam değişkeni boş kaldığında koddaki varsayılan şifre devreye girecektir.
- **Risk:** Hibernate'in kontrolsüz şema güncellemesi rolling update sırasında diğer pod'ları bozabilir. Kod içi fallback secret zafiyete yol açabilir.
- **Öneri:** Prod profili için `ddl-auto=validate` (veya Flyway/Liquibase) kullanılmalı; `app.secret=${APP_SECRET}` olarak fallback'siz zorunlu kılınmalıdır.

---

### 3. 🛡️ [Güvenlik] `readOnlyRootFilesystem: false` ve `emptyDir` Eksikliği (P1 - Yüksek)
- **İlgili Kural:** `Can the application run with readOnlyRootFilesystem: true?`, `Are writable directories mounted through emptyDir where appropriate?`
- **Dosya:** [kubernetes/base/apps/auth-service/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/deployment.yaml#L73-L78)
- **Mevcut Durum:** `readOnlyRootFilesystem: true` tanımlanmamıştır.
- **Risk:** Konteyner kök dosya sistemi yazılabilir durumdadır.
- **Öneri:** `readOnlyRootFilesystem: true` yapılarak JVM ve Tomcat geçici dizinleri için `/tmp` mount'u (`emptyDir`) eklenmelidir.

---

### 4. 🔒 [Güvenlik] `seccompProfile: RuntimeDefault` Eksikliği (P2 - Orta)
- **İlgili Kural:** `Is a suitable seccomp profile configured?`, `Is seccompProfile.type: RuntimeDefault configured?`
- **Dosya:** [kubernetes/base/apps/auth-service/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/deployment.yaml#L26-L30)
- **Mevcut Durum:** Pod `securityContext` bloğunda `seccompProfile` tanımlı değil.
- **Risk:** Pod Security Standards (Restricted) profiliyle uyumsuzluk.
- **Öneri:** Pod `securityContext` içine `seccompProfile: { type: RuntimeDefault }` eklenmelidir.

---

### 5. 🌐 [Ağ Politikası] NetworkPolicy Egress Kurallarının Geniş Olması (`0.0.0.0/0`) (P2 - Orta)
- **İlgili Kural:** `Is expected egress traffic documented and restricted?`
- **Dosya:** [kubernetes/base/apps/auth-service/network-policy.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/network-policy.yaml#L38-L41)
- **Mevcut Durum:** Geliştirme kolaylığı için egress kuralı `0.0.0.0/0` olarak bırakılmıştır.
- **Risk:** İzolasyon tam olarak sağlanmamış olup pod'un internete veya beklenmeyen IP bloklarına çıkışı açıktır.
- **Öneri:** Prod ortamında egress kuralları spesifik olarak PostgreSQL (5432/5438), Kafka (9092) ve Vault (8200) port/IP bloklarına daraltılmalıdır.

---

### 6. 💾 [Kaynak Yönetimi] `ephemeral-storage` Sınırlarının Tanımlanmamış Olması (P2 - Orta)
- **İlgili Kural:** `Is ephemeral storage usage bounded?`, `Are ephemeral-storage requests/limits defined where necessary?`
- **Dosya:** [kubernetes/base/apps/auth-service/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/deployment.yaml#L67-L72)
- **Mevcut Durum:** `cpu` (250m/1000m) ve `memory` (384Mi/768Mi) tanımlı fakat `ephemeral-storage` tanımlı değil.
- **Risk:** JVM heap dump veya geçici log büyümesinde node diskinin dolması.
- **Öneri:** `requests.ephemeral-storage: 100Mi` ve `limits.ephemeral-storage: 500Mi` eklenmelidir.

---

### 7. 🔄 [Kullanılabilirlik] `minReadySeconds` ve `podAntiAffinity` Eksikliği (P2 - Orta)
- **İlgili Kural:** `Is minReadySeconds configured where appropriate?`, `Are replicas spread across multiple nodes?`
- **Dosya:** [kubernetes/base/apps/auth-service/deployment.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/deployment.yaml)
- **Mevcut Durum:** `minReadySeconds` ve `affinity` kuralları eksik.
- **Risk:** Node arızasında her iki auth-service pod'unun aynı anda erişilemez olma riski.
- **Öneri:** `minReadySeconds: 5` ve `podAntiAffinity` tanımlanmalıdır.

---

### 8. 📊 [Etiket Standartları] Eksik Standart Etiketler (P3 - Düşük)
- **İlgili Kural:** `Does the workload have an appropriate app.kubernetes.io/version label?`, `...managed-by label?`
- **Dosya:** [kubernetes/base/apps/auth-service/kustomization.yaml](file:///home/doguhan/SoftwareProjects/hospital-information-system/kubernetes/base/apps/auth-service/kustomization.yaml#L4-L9)
- **Mevcut Durum:** `app.kubernetes.io/version` ve `app.kubernetes.io/managed-by` etiketleri eksik.
- **Öneri:** `version: "1.0.0"` ve `managed-by: kustomize` eklenmelidir.

---

### 9. 🔍 [Tedarik Zinciri Güvenliği] Otomatik CVE Taraması (P2 - Orta)
- **İlgili Kural:** `Are all container images scanned before release?`
- **Dosya:** `.github/workflows/`
- **Mevcut Durum:** İmaj build adımlarında Trivy zafiyet denetimi entegrasyonu henüz eklenmemiştir.
- **Öneri:** CI pipeline'ına Trivy entegre edilmelidir.

---

## 📋 Özet Değerlendirme Tablosu

| Kategori | Durum | İncelenen Madde Sayısı | Başarılı | İyileştirme / Bulgu |
| :--- | :---: | :---: | :---: | :---: |
| **1. Application & Container** | 🟢 İyi | 38 | 35 | 3 |
| **2. Kubernetes Manifests** | 🟡 Orta | 56 | 51 | 5 |
| **3. Security & Access Controls**| 🟡 Orta | 32 | 31 | 1 |
| **4. Scaling & Reliability** | 🟢 İyi | 15 | 15 | 0 |

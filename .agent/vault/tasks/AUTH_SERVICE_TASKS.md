# Auth-Service Vault Entegrasyonu Görev Listesi (Task Breakdown)

Bu belge, `auth-service` mikroservisinin veritabanı şifreleri (`SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_USERNAME`), JWT imzalama anahtarı (`APP_SECRET`) ve iç iletişim anahtarlarının (`API_KEY`) HashiCorp Vault ile entegre edilmesi adımlarını içerir.

---

## 📋 Görev Adımları

### [Adım 1] Vault Tarafı Hazırlığı (Tamamlandı ✅)
- [x] **1.1.** Vault üzerinde `auth-service`'e özel şifreler yazıldı:
  * `secret/hospital/auth-service/db` $\rightarrow$ `{ "username": "auth_user", "password": "auth_pass_123" }`
  * `secret/hospital/auth-service/api-keys` $\rightarrow$ `{ "internal_api_key": "hospital-internal-secure-key" }`
  * *(Ortak `secret/hospital/shared/jwt` `app_secret` anahtarını `api-gateway` adımında yazmıştık, ortak okundu).*
- [x] **1.2.** `auth-service-policy.hcl` oluşturuldu ve Vault'a yüklendi:
  ```hcl
  path "secret/data/hospital/shared/jwt" {
    capabilities = ["read"]
  }
  path "secret/data/hospital/auth-service/*" {
    capabilities = ["read"]
  }
  ```
- [x] **1.3.** Vault Kubernetes Auth mekanizmasında `auth-service-sa` ServiceAccount'u ile `auth-service-policy` kuralını eşleyen `auth-service-role` rolü oluşturuldu.

---

### [Adım 2] Kubernetes Entegrasyon Katmanı (ESO / Secret Manifesti) (Tamamlandı ✅)
- [x] **2.1.** `SecretStore` ve `ExternalSecret` manifesti oluşturuldu (`kubernetes/base/apps/auth-service/external-secret.yaml`):
  * `APP_SECRET`, `JWT_SECRET`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `API_KEY` anahtarları Vault'tan dinamik olarak `auth-service-secret` nesnesine bağlandı.
- [x] **2.2.** `kubernetes/base/apps/auth-service/kustomization.yaml` dosyasına `external-secret.yaml` eklendi ve statik `secret.yaml` çıkarıldı.

---

### [Adım 3] Dağıtım ve Pod Güncellemesi (Tamamlandı ✅)
- [x] **3.1.** Kustomize ile `auth-service` manifestleri cluster'a uygulandı (`kubectl apply -k kubernetes/base/apps/auth-service`).
- [x] **3.2.** External Secrets Operator'ın `auth-service-secret` nesnesini 5 anahtarla birlikte başarıyla oluşturup senkronize ettiği teyit edildi (`SecretSynced - True`).
- [x] **3.3.** `auth-service` deployment'ı yeniden başlatıldı (Rolling restart başarıyla tamamlandı, 2 pod hazır).

---

### [Adım 4] Test ve Doğrulama (Tamamlandı ✅)
- [x] **4.1.** `auth-service` pod loglarında PostgreSQL (`auth_db`) bağlantısının ve Kafka `patient-created.v1` consumer'ının hatasız bağlandığı teyit edildi.
- [x] **4.2.** `api-gateway` üzerinden `POST /api/auth/register` ve `POST /api/auth/login` istekleri atılarak yeni kullanıcının (`vaultuser`) veritabanına kaydedildiği ve Vault'taki `app_secret` ile üretilen JWT token'ın başarıyla döndüğü doğrulandı.

---

## 🎯 Sonuç
Hem **`api-gateway`** hem de **`auth-service`** artık hiçbir düz metin (plain-text) şifre içermiyor. Tüm hassas bilgiler (veritabanı şifreleri, JWT imzalama anahtarları, API key'ler) HashiCorp Vault kasasında şifrelenmiş olarak tutuluyor ve External Secrets Operator ile güvenli bir şekilde pod'lara besleniyor!

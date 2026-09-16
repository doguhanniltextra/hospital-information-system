# API-Gateway Vault Entegrasyonu Görev Listesi (Task Breakdown)

Bu belge, `api-gateway` servisinin HashiCorp Vault ile entegre edilerek tüm gizli değişkenlerinin (özellikle `APP_SECRET` / JWT doğrulama anahtarı) güvenli hale getirilmesi adımlarını içerir.

---

## 📋 Görev Adımları

### [Adım 0] Altyapı Kurulumu (Tamamlandı ✅)
- [x] **0.1.** HashiCorp Vault Helm chart ile `vault` namespace'ine kuruldu ve `vault-0` unsealed olarak çalıştırıldı.
- [x] **0.2.** External Secrets Operator (ESO) `external-secrets` namespace'ine kuruldu ve controller'lar aktif edildi.

---

### [Adım 1] Vault Tarafı Hazırlığı (Tamamlandı ✅)
- [x] **1.1.** Vault üzerinde `secret/hospital/shared/jwt` yoluna `app_secret` anahtarı yazıldı.
- [x] **1.2.** Sadece `api-gateway`'in ihtiyaç duyduğu yolları okuyabilen `api-gateway-policy.hcl` oluşturuldu ve yüklendi:
  ```hcl
  path "secret/data/hospital/shared/jwt" {
    capabilities = ["read"]
  }
  path "secret/data/hospital/api-gateway/*" {
    capabilities = ["read"]
  }
  ```
- [x] **1.3.** Vault Kubernetes Auth mekanizmasında `api-gateway-sa` ServiceAccount'u ile `api-gateway-policy` kuralını eşleyen `api-gateway-role` rolü tanımlandı.

---

### [Adım 2] Kubernetes Entegrasyon Katmanı (ESO / Secret Manifesti) (Tamamlandı ✅)
- [x] **2.1.** `SecretStore` ve `ExternalSecret` manifesti oluşturuldu (`kubernetes/base/apps/api-gateway/external-secret.yaml`):
  * Vault'taki `secret/hospital/shared/jwt` içindeki `app_secret`'ı okuyup K8s içinde `APP_SECRET` olarak `api-gateway-secret` nesnesine dinamik senkronize ediyor.
- [x] **2.2.** `kubernetes/base/apps/api-gateway/kustomization.yaml` dosyasına `external-secret.yaml` eklendi.
- [x] **2.3.** Statik `secret.yaml` kustomization listesinden çıkarıldı.

---

### [Adım 3] Dağıtım ve Pod Güncellemesi (Tamamlandı ✅)
- [x] **3.1.** Kustomize ile `api-gateway` manifestleri cluster'a uygulandı (`kubectl apply -k kubernetes/base/apps/api-gateway`).
- [x] **3.2.** External Secrets Operator'ın `api-gateway-secret` Kubernetes Secret objesini Vault'tan başarıyla ürettiği ve senkronize ettiği teyit edildi (`SecretSynced - True`).
- [x] **3.3.** `api-gateway` pod'ları güncellenen secret ile yeniden başlatıldı (Rolling restart başarıyla tamamlandı).

---

### [Adım 4] Test ve Doğrulama (Tamamlandı ✅)
- [x] **4.1.** `api-gateway` pod'larının Vault'tan gelen `APP_SECRET` ile sorunsuz ayağa kalktığı doğrulandı.
- [x] **4.2.** `api-gateway` üzerinden `/api/auth/login` rotasına istek atılarak uçtan uca akış başarıyla doğrulandı.

---

## 🎯 Sonuç
`api-gateway` servisi artık hiçbir düz metin (plain-text) şifre barındırmıyor; tüm gizli anahtarlarını HashiCorp Vault üzerinden güvenli ve dinamik olarak çekiyor!

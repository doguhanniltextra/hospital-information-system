# Task 04.1: Vault Yapılandırması (Secrets, Policy & Role)

Bu görev, `patient-management` mikroservisinin ihtiyaç duyduğu tüm sırların (secrets) HashiCorp Vault'ta oluşturulmasını, en az yetki prensibine (Least Privilege) dayalı erişim kuralının (policy) yazılmasını ve Kubernetes ServiceAccount ile eşleşen Vault rolünün tanımlanmasını kapsar.

---

## 1. Vault KV v2 Sırlarının Oluşturulması

### 1.1 Veritabanı ve Güvenlik Anahtarları
`patient-management` için Vault üzerinde şu iki gizli dizin yolu açılacaktır:

1. **`secret/hospital/patient-management/db`:**
   * `username`: `"patient_user"`
   * `password`: `"patient_pass_123"`
2. **`secret/hospital/patient-management/security`:**
   * `encryption_key`: `"MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="` *(AES-256 PII Hasta Verisi Şifreleme Anahtarı)*
3. **`secret/hospital/shared/jwt`:**
   * `app_secret` *(Zaten kasada mevcut, ortak okunacak).*

### 1.2 CLI Uygulama Komutları:
```bash
# DB kimlik bilgileri
kubectl exec -n vault vault-0 -- vault kv put secret/hospital/patient-management/db \
    username="patient_user" \
    password="patient_pass_123"

# AES PII Şifreleme anahtarı
kubectl exec -n vault vault-0 -- vault kv put secret/hospital/patient-management/security \
    encryption_key="MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
```

---

## 2. Vault Policy Tanımı (`patient-management-policy.hcl`)

Dosya Konumu: `.agent/vault/policies/patient-management-policy.hcl`

```hcl
# 1. Ortak JWT imza doğrulama anahtarını okuma izni
path "secret/data/hospital/shared/jwt" {
  capabilities = ["read"]
}

# 2. Sadece patient-management'a ait veritabanı ve şifreleme anahtarlarını okuma izni
path "secret/data/hospital/patient-management/*" {
  capabilities = ["read"]
}
```

### 2.1 Policy Yükleme Komutu:
```bash
kubectl cp .agent/vault/policies/patient-management-policy.hcl vault/vault-0:/tmp/patient-management-policy.hcl
kubectl exec -n vault vault-0 -- vault policy write patient-management-policy /tmp/patient-management-policy.hcl
```

---

## 3. Kubernetes Auth Rolü Tanımı (`patient-management-role`)

Kubernetes `default` namespace'indeki `patient-management-sa` ServiceAccount kimliğini `patient-management-policy` kuralına bağlar:

```bash
kubectl exec -n vault vault-0 -- vault write auth/kubernetes/role/patient-management-role \
    bound_service_account_names=patient-management-sa \
    bound_service_account_namespaces=default \
    policies=patient-management-policy \
    ttl=24h
```

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] Vault üzerinde `secret/hospital/patient-management/db` oluşturuldu (`username`, `password`).
- [x] Vault üzerinde `secret/hospital/patient-management/security` oluşturuldu (`encryption_key`).
- [x] `.agent/vault/policies/patient-management-policy.hcl` dosyası oluşturuldu ve Vault'a yüklendi.
- [x] `patient-management-role` Kubernetes Auth mekanizmasına bağlandı.

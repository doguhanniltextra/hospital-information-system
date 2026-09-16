# Task 04.1: HashiCorp Vault Entegrasyonu ve Secret Yönetimi (Vault Configuration)

Bu doküman, `appointment-service` mikroservisinin veritabanı şifreleri, JWT ortak anahtarı ve dahili iletişim token'ının HashiCorp Vault üzerinde güvenli olarak saklanması, erişim politikası (ACL Policy) ve Kubernetes Auth rolünün tanımlanması adımlarını içerir.

---

## 1. Vault Secret Yolları ve Anahtarları

`appointment-service` için aşağıdaki KV Version 2 secret yolları kullanılır:

| Secret Yolu (Path) | Anahtarlar (Keys) | Açıklama |
| :--- | :--- | :--- |
| `secret/data/hospital/appointment-service/db` | `username`, `password` | PostgreSQL `appointment_db` erişim kimlik bilgileri |
| `secret/data/hospital/shared/jwt` | `app_secret` | Gelen isteklerdeki JWT imzasını doğrulama anahtarı |
| `secret/data/hospital/shared/internal-auth` | `internal_token` | Servisler arası (`X-Internal-Token`) iç token |

---

## 2. Vault CLI Yapılandırma Komutları

Aşağıdaki komutları çalıştırarak Vault içerisindeki secret, policy ve role tanımlarını gerçekleştirin:

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

# 1. Secret'ları yaz
vault kv put secret/hospital/appointment-service/db username=appointment_user password=appointment_pass_123
vault kv put secret/hospital/shared/jwt app_secret=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong
vault kv put secret/hospital/shared/internal-auth internal_token=pm-internal-token

# 2. appointment-service ACL Politikasını oluştur
vault policy write appointment-service-policy - <<EOF
path \"secret/data/hospital/appointment-service/*\" {
  capabilities = [\"read\"]
}
path \"secret/data/hospital/shared/*\" {
  capabilities = [\"read\"]
}
EOF

# 3. Kubernetes Auth Rolünü oluştur
vault write auth/kubernetes/role/appointment-service-role \
    bound_service_account_names=appointment-service-sa \
    bound_service_account_namespaces=default \
    policies=appointment-service-policy \
    ttl=24h
"
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `secret/hospital/appointment-service/db` secret'ının Vault'a başarıyla yazıldığı doğrulandı.
- [x] `appointment-service-policy` ACL politikasının oluşturulduğu teyit edildi.
- [x] `auth/kubernetes/role/appointment-service-role` rolünün `appointment-service-sa` ServiceAccount'ına bağlandığı doğrulandı.

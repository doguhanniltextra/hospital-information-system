# Task 04.1: HashiCorp Vault Yapılandırması ve Güvenlik Rolleri (Vault Configuration)

Bu görev, `billing-service` mikroservisinin ihtiyaç duyduğu tüm hassas verilerin (veritabanı kimlik bilgileri, harici fatura API anahtarları, ortak JWT ve servisler arası token) HashiCorp Vault KV v2 motoruna yazılmasını, servise özel ACL politikasının oluşturulmasını ve Kubernetes Authentication rolünün bağlanmasını kapsar.

---

## 1. Vault Secret Yolları ve İzin Şeması

| Sır Türü | Vault Yolu (Path) | Anahtarlar (Keys) |
| :--- | :--- | :--- |
| **Veritabanı Kimlik Bilgileri** | `secret/data/hospital/billing-service/db` | `username=billing_user`, `password=billing_pass_123` |
| **Harici API Anahtarı** | `secret/data/hospital/billing-service/api-keys` | `invoice_api_key=mock_invoice_api_key` |
| **Ortak JWT İmzası** | `secret/data/hospital/shared/jwt` | `app_secret=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong` |
| **Ortak İç Haberleşme Token** | `secret/data/hospital/shared/internal-auth` | `internal_token=pm-internal-token` |

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: Sırları, ACL Politikasını ve Kubernetes Auth Rolünü Vault'a Tanımlamak

`vault-0` podu içinde aşağıdaki komutları çalıştırın:

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

# 1. billing-service secret'larını yaz
vault kv put secret/hospital/billing-service/db username=billing_user password=billing_pass_123
vault kv put secret/hospital/billing-service/api-keys invoice_api_key=mock_invoice_api_key
vault kv put secret/hospital/shared/jwt app_secret=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong
vault kv put secret/hospital/shared/internal-auth internal_token=pm-internal-token

# 2. billing-service ACL Politikasını oluştur
vault policy write billing-service-policy - <<EOF
path \"secret/data/hospital/billing-service/*\" {
  capabilities = [\"read\"]
}
path \"secret/data/hospital/shared/*\" {
  capabilities = [\"read\"]
}
EOF

# 3. Kubernetes Auth Rolünü oluştur
vault write auth/kubernetes/role/billing-service-role \
    bound_service_account_names=billing-service-sa \
    bound_service_account_namespaces=default \
    policies=billing-service-policy \
    ttl=24h
"
```

---

### 2.2 Adım 2: Yapılandırmayı Doğrulamak

```bash
# Sırları oku:
kubectl exec -i vault-0 -n vault -- vault kv get secret/hospital/billing-service/db
kubectl exec -i vault-0 -n vault -- vault kv get secret/hospital/billing-service/api-keys

# Rolü oku:
kubectl exec -i vault-0 -n vault -- vault read auth/kubernetes/role/billing-service-role
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] Vault KV üzerinde `secret/hospital/billing-service/db` ve `api-keys` sırları oluşturuldu.
- [x] `billing-service-policy` ACL politikası tanımlandı.
- [x] `billing-service-role` Kubernetes auth rolü `billing-service-sa` ServiceAccount'ına bağlandı.
- [x] Vault CLI üzerinden sırların ve rolün okunabildiği teyit edildi.

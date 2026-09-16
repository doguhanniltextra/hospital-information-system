# Task 04.1: HashiCorp Vault Yapılandırması ve Entegrasyonu (Vault Configuration)

Bu görev, `notification-service` mikroservisinin ihtiyaç duyduğu tüm hassas verilerin (PostgreSQL kullanıcı bilgileri, Redis şifresi, paylaşılan JWT anahtarı, dahili token ve operasyonel e-posta listesi) HashiCorp Vault KV v2 secret engine içerisine yazılmasını, servise özel erişim politikasının (`notification-service-policy`) ve Kubernetes Auth rolünün (`notification-service-role`) oluşturulmasını kapsar.

---

## 1. Vault Secrets Haritası ve Yolları

| Gizli Bilgi (Secret) | Vault Dizin Yolu | Anahtar (Key) | Değer Türü / Açıklama |
| :--- | :--- | :--- | :--- |
| Veritabanı Kullanıcısı | `secret/data/hospital/notification-service/db` | `username` | `notification_user` |
| Veritabanı Şifresi | `secret/data/hospital/notification-service/db` | `password` | `notification_pass_123` |
| Redis Şifresi | `secret/data/hospital/notification-service/redis` | `password` | `""` (veya redis şifresi) |
| Operasyonel E-postalar | `secret/data/hospital/notification-service/api-keys` | `ops_alert_emails` | `admin@hospital.com,ops@hospital.com` |
| Ortak JWT Anahtarı | `secret/data/hospital/shared/jwt` | `app_secret` | `mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong` |
| Ortak Dahili Token | `secret/data/hospital/shared/internal-auth` | `internal_token` | `pm-internal-token` |

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: Sırları (Secrets) Vault'a Yazmak

`vault-0` podu içerisinde KV v2 motoruna sırları yazın:

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

# 1. notification-service Veritabanı Sırları
vault kv put secret/hospital/notification-service/db \
    username=notification_user \
    password=notification_pass_123

# 2. notification-service Redis Sırları
vault kv put secret/hospital/notification-service/redis \
    password=\"\"

# 3. notification-service Operasyonel E-posta Sırları
vault kv put secret/hospital/notification-service/api-keys \
    ops_alert_emails=admin@hospital.com,ops@hospital.com
"
```

### 2.2 Adım 2: `notification-service-policy` ACL Politikasını Oluşturmak

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

vault policy write notification-service-policy - <<EOF
path \"secret/data/hospital/notification-service/*\" {
  capabilities = [\"read\"]
}
path \"secret/data/hospital/shared/*\" {
  capabilities = [\"read\"]
}
EOF
"
```

### 2.3 Adım 3: Kubernetes Auth Rolünü (`notification-service-role`) Tanımlamak

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

vault write auth/kubernetes/role/notification-service-role \
    bound_service_account_names=notification-service-sa \
    bound_service_account_namespaces=default \
    policies=notification-service-policy \
    ttl=24h
"
```

### 2.4 Adım 4: Doğrulama

```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

vault kv get secret/hospital/notification-service/db
vault kv get secret/hospital/notification-service/api-keys
vault read auth/kubernetes/role/notification-service-role
"
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `secret/hospital/notification-service/db` ve `api-keys` sırları Vault'a yazıldı.
- [x] `notification-service-policy` ACL politikası oluşturuldu.
- [x] `auth/kubernetes/role/notification-service-role` rolü `notification-service-sa` için tanımlandı.
- [x] Rol ve yetkilerin çalıştığı doğrulandı.

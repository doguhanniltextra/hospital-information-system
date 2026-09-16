# Vault Secrets Envanteri

Bu doküman, **Hospital Information System** projesindeki mikroservislerin HashiCorp Vault üzerinde saklanması gereken hassas bilgilerini (secrets), bu bilgilerin amaçlarını, önem derecelerini ve Vault içerisindeki hiyerarşik dizin yollarını içerir.

---

## 1. `auth-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** / **`JWT_SECRET`** | Access Token üretimi ve HMAC-SHA256 imzalaması için kullanılan anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `auth_db` PostgreSQL veritabanı kullanıcı adı. | `auth_user` | 🟡 **Orta (P2)** | `secret/data/hospital/auth-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `auth_db` PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/auth-service/db` (`key: password`) |
| **`API_KEY`** | Servisler arası iç iletişim doğrulaması için API anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/auth-service/api-keys` (`key: internal_api_key`) |
| **`KAFKA_SASL_JAAS_CONFIG`** *(Opsiyonel / Prod)* | Kafka broker'a güvenli bağlanmak için SASL/SCRAM bilgileri. | JAAS config string | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/kafka` (`key: jaas_config`) |

---

## 2. `api-gateway` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Dışarıdan gelen isteklerdeki JWT imzasını doğrulamak (verify) için kullanılan anahtar. *(auth-service ile senkron olmalı)* | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`REDIS_PASSWORD`** | İstek sınırlama (Rate Limiting) için kullanılan Redis şifresi. | Alfanümerik şifre | 🟠 **Yüksek (P1)** | `secret/data/hospital/api-gateway/redis` (`key: password`) |
| **`TLS_PRIVATE_KEY`** *(Prod)* | Gateway'in HTTPS trafiğini karşılaması için SSL/TLS özel anahtarı. | PEM formatında private key | 🔴 **Kritik (P0)** | `secret/data/hospital/api-gateway/tls` (`key: tls_key`) |

---

## 3. `patient-management` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak için kullanılan anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`APP_SECURITY_ENCRYPTION_KEY`** | PostgreSQL'e kaydedilen hasta TC, telefon gibi hassas (PII) verileri AES ile şifreleme anahtarı. | 256-bit Base64 Key | 🔴 **Kritik (P0 - KVKK)** | `secret/data/hospital/patient-management/security` (`key: encryption_key`) |
| **`SPRING_DATASOURCE_USERNAME`** | `patient_db` CQRS Read/Write veritabanı kullanıcı adı. | `patient_user` | 🟡 **Orta (P2)** | `secret/data/hospital/patient-management/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `patient_db` CQRS Read/Write veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/patient-management/db` (`key: password`) |

---

## 4. `doctor-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak (verify) için kullanılan ortak anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `doctor_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı adı. | `doctor_user` | 🟡 **Orta (P2)** | `secret/data/hospital/doctor-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `doctor_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/doctor-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |

---

## 5. `appointment-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak (`JwtAuthFilter`) için ortak simetrik anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `appointment_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı adı. | `appointment_user` | 🟡 **Orta (P2)** | `secret/data/hospital/appointment-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `appointment_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/appointment-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |

---

## 6. `admission-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak (`JwtAuthFilter`) için ortak simetrik anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `admission_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı adı. | `admission_user` | 🟡 **Orta (P2)** | `secret/data/hospital/admission-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `admission_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/admission-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |
| **`REDIS_PASSWORD`** *(Opsiyonel / Prod)* | Hasta ve doktor varlık sorguları önbelleği için Redis bağlantı şifresi. | Alfanümerik şifre | 🟠 **Yüksek (P1)** | `secret/data/hospital/admission-service/redis` (`key: password`) |

---

## 7. `support-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak için ortak simetrik anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `support_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı adı. | `support_user` | 🟡 **Orta (P2)** | `secret/data/hospital/support-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `support_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/support-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |
| **`REDIS_PASSWORD`** *(Opsiyonel / Prod)* | Laboratuvar ve envanter sorguları önbelleği için Redis bağlantı şifresi. | Alfanümerik şifre | 🟠 **Yüksek (P1)** | `secret/data/hospital/support-service/redis` (`key: password`) |

---

## 8. `billing-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak için ortak simetrik anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `billing_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı adı. | `billing_user` | 🟡 **Orta (P2)** | `secret/data/hospital/billing-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `billing_db` CQRS Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/billing-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |
| **`INVOICE_API_KEY`** | Dış fatura üretim API'si (`invoice-generator.com`) entegrasyonu için gizli API anahtarı. | UUID / Alpha-numeric API Key | 🟠 **Yüksek (P1)** | `secret/data/hospital/billing-service/api-keys` (`key: invoice_api_key`) |

---

## 9. `notification-service` Secrets Tablosu

| Değişken Adı | Açıklama | Ortam / Örnek Değer Türü | Önem Derecesi | Önerilen Vault Yolu (Path) |
| :--- | :--- | :--- | :--- | :--- |
| **`APP_SECRET`** | Gelen HTTP isteklerindeki JWT imzasını doğrulamak için ortak simetrik anahtar. | 256-bit Hex / Base64 string | 🔴 **Kritik (P0)** | `secret/data/hospital/shared/jwt` (`key: app_secret`) |
| **`SPRING_DATASOURCE_USERNAME`** | `notification_db` Read/Write PostgreSQL veritabanı kullanıcı adı. | `notification_user` | 🟡 **Orta (P2)** | `secret/data/hospital/notification-service/db` (`key: username`) |
| **`SPRING_DATASOURCE_PASSWORD`** | `notification_db` Read/Write PostgreSQL veritabanı kullanıcı şifresi. | Alfanümerik güçlü şifre | 🔴 **Kritik (P0)** | `secret/data/hospital/notification-service/db` (`key: password`) |
| **`INTERNAL_SERVICE_TOKEN`** | Servisler arası (`X-Internal-Token`) güvenli dahili haberleşme anahtarı. | UUID / Random Token | 🟠 **Yüksek (P1)** | `secret/data/hospital/shared/internal-auth` (`key: internal_token`) |
| **`REDIS_PASSWORD`** *(Opsiyonel / Prod)* | Hasta iletişim bilgileri (`patientContacts`) önbelleği için Redis bağlantı şifresi. | Alfanümerik şifre | 🟠 **Yüksek (P1)** | `secret/data/hospital/notification-service/redis` (`key: password`) |
| **`OPS_ALERT_EMAILS`** | Düşük stok ve süresi dolmuş envanter uyarılarının gönderileceği operasyonel e-posta adresleri listesi. | Virgülle ayrılmış e-posta listesi | 🟡 **Orta (P2)** | `secret/data/hospital/notification-service/api-keys` (`key: ops_alert_emails`) |

---

## 10. Servisler Arası Ortak Sırlar (Shared Secrets)

| Anahtar | Kullanan Servisler | Açıklama |
| :--- | :--- | :--- |
| **`app_secret`** | `auth-service`, `api-gateway`, `patient-management`, `doctor-service`, `appointment-service`, `admission-service`, `support-service`, `billing-service`, `notification-service` | Token imzalama ve doğrulama için ortak simetrik anahtar. |
| **`internal_token`** | `doctor-service`, `patient-management`, `appointment-service`, `admission-service`, `support-service`, `billing-service`, `notification-service` | Servisler arası iç API çağrılarını (`ROLE_INTERNAL_SERVICE`) doğrulamak için paylaşılan token. |

---

## 11. Vault Hiyerarşik Dizin Şeması

```text
secret/ (KV Version 2 Engine)
  └── hospital/
        ├── shared/
        │     ├── jwt                    --> [ app_secret ]
        │     ├── internal-auth          --> [ internal_token ]
        │     └── kafka                  --> [ jaas_config ]
        │
        ├── auth-service/
        │     ├── db                     --> [ username, password ]
        │     └── api-keys               --> [ internal_api_key ]
        │
        ├── patient-management/
        │     ├── db                     --> [ username, password ]
        │     └── security               --> [ encryption_key ]
        │
        ├── doctor-service/
        │     └── db                     --> [ username, password ]
        │
        ├── appointment-service/
        │     └── db                     --> [ username, password ]
        │
        ├── admission-service/
        │     ├── db                     --> [ username, password ]
        │     └── redis                  --> [ password ]
        │
        ├── support-service/
        │     ├── db                     --> [ username, password ]
        │     └── redis                  --> [ password ]
        │
        ├── billing-service/
        │     ├── db                     --> [ username, password ]
        │     └── api-keys               --> [ invoice_api_key ]
        │
        ├── notification-service/
        │     ├── db                     --> [ username, password ]
        │     ├── redis                  --> [ password ]
        │     └── api-keys               --> [ ops_alert_emails ]
        │
        └── api-gateway/
              ├── redis                  --> [ password ]
              └── tls                    --> [ tls_cert, tls_key ]
```

# Kubernetes & Vault Geçiş Yol Haritası (Migration Roadmap)

Bu belge, **Hospital Information System** mikroservislerinin Kubernetes ortamına taşınması ve HashiCorp Vault ile güvenli hale getirilmesi sürecindeki servis geçiş sıralamasını, bağımlılıkları ve durumlarını içerir.

> [!TIP]
> **Altyapı Konsolidasyonu:** Veritabanı kaynak tasarrufu ve tekilleştirme operasyonu için [[POSTGRESQL_CONSOLIDATION_OPERATIONAL_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/shared/POSTGRESQL_CONSOLIDATION_OPERATIONAL_GUIDE.md)] dokümanını inceleyiniz.

---

## 🗺️ Servis Geçiş Sıralaması ve Durum Tablosu

| Sıra | Servis Adı | Rol / Sorumluluk | Temel Bağımlılıklar | Durum |
| :---: | :--- | :--- | :--- | :---: |
| **1** | **`api-gateway`** | Dış trafik karşılama, yönlendirme, rate limiting, JWT doğrulama. | Redis | ✅ **Tamamlandı** |
| **2** | **`auth-service`** | Kimlik doğrulama, JWT/Refresh token üretimi, kullanıcı yönetimi. | PostgreSQL (`auth_db`), Kafka | ✅ **Tamamlandı** |
| **3** | **`patient-management`** | Hasta kayıt/yönetim ana veri kaynağı (Master Data), gRPC sunucusu, Outbox event üreticisi. | PostgreSQL (`patient_db` - CQRS R/W), Kafka, Vault (`ENCRYPTION_KEY`) | ✅ **Tamamlandı** |
| **4** | **`doctor-service`** | Doktor ve poliklinik yönetimi, uzmanlık alanları. | PostgreSQL (`doctor_db`), Kafka, gRPC (9005) | ✅ **Tamamlandı** |
| **5** | **`appointment-service`** | Randevu oluşturma ve yönetimi (Saga). | PostgreSQL (`appointment_db`), Kafka, `patient-management`, `doctor-service` | ✅ **Tamamlandı** |
| **6** | **`admission-service`** | Hasta yatış, oda/yatak takibi, taburcu işlemleri. | PostgreSQL (`admission_db`), Kafka, `patient-management` (gRPC), `doctor-service` (gRPC), Redis | ✅ **Tamamlandı** |
| **7** | **`support-service`** | Laboratuvar sonuçları, stok/envanter takibi. | PostgreSQL (`support_db`), Kafka, Redis | ✅ **Tamamlandı** |
| **8** | **`billing-service`** | Fatura kesme, ödeme işlemleri. | PostgreSQL (`billing_db`), Kafka | ✅ **Tamamlandı** |
| **9** | **`notification-service`** | SMS, E-posta ve sistem içi bildirimler. | Kafka, `patient-management` (gRPC), Redis | ✅ **Tamamlandı** |

---

## 🔗 Neden Bu Sıralama? (Mimari Mantık)

1. **Katman 1: Güvenlik ve Giriş Kapısı (`api-gateway` + `auth-service`)**
   * Tüm sistemin güvenliğini, token mekanizmasını ve trafiğin giriş noktasını sağladığı için ilk olarak devreye alındı.

2. **Katman 2: Ana Veri Kaynakları (`patient-management` $\rightarrow$ `doctor-service`)**
   * Klinik işlemlerin (randevu, yatış, tahlil) gerçekleşebilmesi için önce "Hasta" ve "Doktor" varlıklarının canlıda olması gerekir.
   * `patient-management` tarafından üretilen `patient-created.v1` eventi `auth-service` tarafından dinlenir.

3. **Katman 3: Temel İş Mantığı (`appointment-service` $\rightarrow$ `admission-service`)**
   * Hasta ve doktor verisi hazır olduktan sonra randevu ve yatış süreçleri işletilebilir.

4. **Katman 4: Destek, Faturalama ve Bildirim (`support-service`, `billing-service`, `notification-service`)**
   * Tıbbi ve finansal yan süreçler ile event tabanlı bildirimler son katmanda sisteme entegre edilir.

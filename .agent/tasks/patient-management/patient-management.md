# Patient-Management Geçiş Genel Özeti

Bu doküman, `patient-management` mikroservisinin Kubernetes, HashiCorp Vault ve CI/CD süreçlerine entegrasyonunun üst seviye özetidir.

---

## 📌 Genel Bakış ve Mimari

* **Servis Adı:** `patient-management`
* **Rol:** Hastane Bilgi Sisteminin Ana Veri (Master Data) sağlayıcısı.
* **Portlar:** HTTP `8080` (REST & Actuator), gRPC `9090` (Internal Servis Sorguları).
* **Veritabanı:** PostgreSQL (CQRS Write DB: `5432`, Read DB: `5435`, `patient_schema`).
* **Kritik Sırlar (Vault):**
  * `APP_SECRET` (JWT Token Doğrulama)
  * `APP_SECURITY_ENCRYPTION_KEY` (AES-256 PII Şifreleme)
  * `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (CQRS Postgres)
* **Kafka Eventleri:**
  * Üretilen: `patient-created.v1` (Outbox ile basılır, `auth-service` dinler).
  * Tüketilen: `user-provisioned.v1`, `lab-result-completed.v1`.

---

## 📑 Görev Detayları
Detaylı uygulama adımları için [README.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/patient-management/README.md) dosyasını ve altındaki numaralandırılmış görevleri (`01_*.md` - `06_*.md`) takip ediniz.

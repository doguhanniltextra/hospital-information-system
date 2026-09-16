# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu görev, `notification-service` mikroservisinin ihtiyaç duyduğu tüm harici altyapı bağımlılıklarını (PostgreSQL veritabanı, Redis önbelleği, Kafka topic'leri ve `patient-management` gRPC bağlantısı) hazırlamayı ve doğrulamayı kapsar.

---

## 1. Mimari Bağımlılıklar

1. **PostgreSQL (`his-postgres:5432`):**
   * Veritabanı: `notification_db`
   * Kullanıcı: `notification_user`
   * Şifre: `notification_pass_123`
   * Şema: `notification_schema`
   * Tablolar: `notification_templates`, `notification_history`, `processed_events`
2. **Kafka Broker (`kafka:9092`):**
   * Tüketilen Topic'ler:
     - `lab-result-completed.v1` (3 partition)
     - `appointment-scheduled.v1` (3 partition)
     - `patient-discharged.v1` (3 partition)
     - `inventory-low-stock.v1` (3 partition)
     - `inventory-item-expired.v1` (3 partition)
     - `user-provisioned.v1` (3 partition)
3. **Redis (`redis:6379`):**
   * Hasta iletişim bilgileri (`patientContacts`) önbelleği
4. **gRPC:**
   * `patient-management` (Port 9090 - `PatientQueryServiceGrpc`)

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: PostgreSQL `notification_db` ve Şema Başlatma

`his-postgres` üzerinde `notification_db` ve şema yapısını oluşturun:

```sql
CREATE USER notification_user WITH PASSWORD 'notification_pass_123';
CREATE DATABASE notification_db OWNER notification_user;
\c notification_db

CREATE SCHEMA IF NOT EXISTS notification_schema AUTHORIZATION notification_user;
GRANT ALL ON SCHEMA notification_schema TO notification_user;
ALTER USER notification_user SET search_path = notification_schema;

CREATE TABLE IF NOT EXISTS notification_schema.notification_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(255) NOT NULL UNIQUE,
    channel VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_schema.notification_history (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    template_code VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_log TEXT,
    sent_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_schema.processed_events (
    id UUID PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    processed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE notification_schema.notification_templates OWNER TO notification_user;
ALTER TABLE notification_schema.notification_history OWNER TO notification_user;
ALTER TABLE notification_schema.processed_events OWNER TO notification_user;

INSERT INTO notification_schema.notification_templates (id, template_code, channel, subject, body, created_at, updated_at)
VALUES 
(gen_random_uuid(), 'LAB_RESULT_READY', 'EMAIL', 'Your Lab Results are Ready', 'Dear Patient, your lab results for order [(${patientId})] are now available at: [(${reportUrl})]', now(), now()),
(gen_random_uuid(), 'APPOINTMENT_CONFIRMATION', 'EMAIL', 'Appointment Confirmation', 'Dear Patient, your appointment is confirmed for [(${appointmentDate})].', now(), now()),
(gen_random_uuid(), 'HOSPITAL_DISCHARGE', 'EMAIL', 'Discharge Summary', 'Dear Patient, you have been discharged. Admission ID: [(${admissionId})]. Get well soon!', now(), now()),
(gen_random_uuid(), 'LOW_STOCK_ALERT', 'EMAIL', 'Low Stock Alert', 'Item [(${itemId})] at [(${location})] is running low. Current quantity: [(${currentQuantity})], Threshold: [(${threshold})].', now(), now()),
(gen_random_uuid(), 'INVENTORY_EXPIRY_ALERT', 'EMAIL', 'Item Expired Alert', 'Item [(${itemId})] at [(${location})] has expired. Expired quantity: [(${expiredQuantity})].', now(), now())
ON CONFLICT (template_code) DO NOTHING;
```

### 2.2 Adım 2: Kafka Topic'lerini Oluşturmak

Kafka konteyneri içerisinde eksik topic'leri 3 partition ile oluşturun:

```bash
# 1. lab-result-completed.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic lab-result-completed.v1 --partitions 3 --replication-factor 1

# 2. appointment-scheduled.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic appointment-scheduled.v1 --partitions 3 --replication-factor 1

# 3. patient-discharged.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic patient-discharged.v1 --partitions 3 --replication-factor 1

# 4. inventory-low-stock.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic inventory-low-stock.v1 --partitions 3 --replication-factor 1

# 5. inventory-item-expired.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic inventory-item-expired.v1 --partitions 3 --replication-factor 1

# 6. user-provisioned.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic user-provisioned.v1 --partitions 3 --replication-factor 1
```

### 2.3 Adım 3: Redis Bağlantı Doğrulaması

```bash
docker exec -i redis redis-cli ping
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `notification_db` ve `notification_schema` oluşturuldu, tablo yetkileri `notification_user` kullanıcısına verildi.
- [x] Başlangıç şablonları (`notification_templates`) eklendi.
- [x] 6 Kafka topic'i (`lab-result-completed.v1`, `appointment-scheduled.v1`, `patient-discharged.v1`, `inventory-low-stock.v1`, `inventory-item-expired.v1`, `user-provisioned.v1`) 3 partition ile oluşturuldu.
- [x] Redis ve gRPC endpoint (`patient-management:9090`) erişilebilirliği doğrulandı.

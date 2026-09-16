# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu görev, `billing-service` mikroservisinin ihtiyaç duyduğu veritabanı şeması ve Kafka topic'lerinin konsolide altyapı üzerinde hazırlanmasını ve doğrulanmasını kapsar.

---

## 1. Mimari Gereksinimler

1. **Konsolide PostgreSQL (`his-postgres:5432`):**
   * Veritabanı: `billing_db`
   * Şema: `billing_schema`
   * Kullanıcı: `billing_user` / Şifre: `billing_pass_123`
   * Tablolar:
     * `invoices` (Ana fatura kayıtları)
     * `claims` (Sigorta talep ve onay kayıtları)
     * `unbilled_charges` (Henüz faturalandırılmamış lab, yatak, sarf malzeme harcamaları)
     * `billing_outbox_events` (Transactional Outbox kayıtları)
     * `invoice_summaries` (CQRS Read-model özet tablosu)
2. **Kafka Olay Akışı (`kafka:9092`):**
   * Dinlenen (Consumer) Topic'ler:
     * `appointment-payment-updates.v1` (Randevu ödeme olayları)
     * `lab-order-placed.v1` (Laboratuvar tahlil istemi ve tutar bilgisi)
     * `inventory-item-consumed.v1` (Tıbbi sarf malzeme tüketimi ve birim fiyatı)
     * `admission-bed-charge.v1` (Yatış yatak ve oda ücretleri)
     * `admission-discharged.v1` (Hasta taburcu olma ve nihai fatura kapatma)
   * Üretilen (Producer / Outbox) Topic'ler:
     * `billing-events.v1` (Fatura kesildi / ödeme alındı olayları)

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: PostgreSQL Veritabanı ve Şema Doğrulaması

Konsolide PostgreSQL konteynerinde `billing_db` ve `billing_schema` varlığını kontrol edin ve gerekli tabloları oluşturun:

```bash
docker exec -i his-postgres psql -U postgres -d billing_db << 'EOF'
-- Kullanıcı ve Şema İzinleri
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema, public;

-- Write Model Tabloları
CREATE TABLE IF NOT EXISTS billing_schema.invoices (
    invoice_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    patient_owes DECIMAL(10,2) NOT NULL,
    insurance_owes DECIMAL(10,2) NOT NULL,
    invoice_pdf_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS billing_schema.claims (
    claim_id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS billing_schema.unbilled_charges (
    charge_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    admission_id UUID,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing_schema.billing_outbox_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);

-- Read Model Tablosu (CQRS)
CREATE TABLE IF NOT EXISTS billing_schema.invoice_summaries (
    invoice_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    patient_owes DECIMAL(10,2) NOT NULL,
    insurance_owes DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    patient_name VARCHAR(255),
    doctor_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- İzinleri billing_user'a ver
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA billing_schema TO billing_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA billing_schema TO billing_user;
EOF
```

Tabloların `billing_user` ile erişilebilir olduğunu doğrulayın:

```bash
docker exec -i his-postgres psql -U billing_user -d billing_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema='billing_schema';"
```

---

### 2.2 Adım 2: Kafka Topic'lerinin Oluşturulması

`kafka` konteyneri içinde ilgili topic'leri oluşturun:

```bash
# 1. appointment-payment-updates.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic appointment-payment-updates.v1 --partitions 3 --replication-factor 1

# 2. lab-order-placed.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic lab-order-placed.v1 --partitions 3 --replication-factor 1

# 3. inventory-item-consumed.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic inventory-item-consumed.v1 --partitions 3 --replication-factor 1

# 4. admission-bed-charge.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic admission-bed-charge.v1 --partitions 3 --replication-factor 1

# 5. admission-discharged.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic admission-discharged.v1 --partitions 3 --replication-factor 1

# 6. billing-events.v1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic billing-events.v1 --partitions 3 --replication-factor 1
```

Topic listesini doğrulayın:

```bash
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | grep -E "billing|lab-order|inventory|admission|appointment"
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `billing_db` veritabanında `billing_schema` altında `invoices`, `claims`, `unbilled_charges`, `billing_outbox_events` ve `invoice_summaries` tabloları başarıyla oluşturuldu.
- [x] `billing_user` kullanıcısı ile tablolara okuma/yazma yetkisi test edildi.
- [x] Kafka üzerinde `appointment-payment-updates.v1`, `lab-order-placed.v1`, `inventory-item-consumed.v1`, `admission-bed-charge.v1`, `admission-discharged.v1` ve `billing-events.v1` topic'leri oluşturuldu ve doğrulandı.

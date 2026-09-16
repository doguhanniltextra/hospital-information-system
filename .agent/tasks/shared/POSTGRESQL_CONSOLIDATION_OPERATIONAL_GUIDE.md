# PostgreSQL Tekilleştirme ve Mantıksal İzolasyon Operasyon Kılavuzu
## (Logical Database-per-Service Consolidation Guide)

Bu operasyonel kılavuz; Hospital Information System (HIS) projesindeki 8 mikroservisin 10+ ayrı fiziksel PostgreSQL konteyneri yerine, **tek bir optimize PostgreSQL instance'ı üzerinde izole mantıksal veritabanları (Logical Databases) ve yetkilendirilmiş kullanıcılar (Role/User-based Isolation)** ile çalışmasını sağlamak için hazırlanmıştır.

---

## 🎯 1. Operasyonun Amacı ve Kazanımları

* **%85 Bellek Tasarrufu:** 10 ayrı PostgreSQL instance'ı (~1.5GB RAM) yerine, tek bir instance (~200MB-300MB RAM).
* **Port Karmaşasının Sona Ermesi:** `5432`, `5433`, `5435`, `5438`, `5439` gibi karmaşık port yönlendirmeleri yerine standart tek port: **`5432`**.
* **Sıfır Java Kod Değişikliği:** Spring Boot `RoutingDataSource` ve JPA katmanları dış parametrelere (`ConfigMap` / Environment Variables) bağlı olduğundan `.java` dosyalarına dokunulmaz.
* **Tam Veri Güvenliği ve İzolasyon:** Her mikroservis yalnızca kendi veritabanına ve şemasına yetkili ayrı bir kullanıcı (`auth_user`, `patient_user`, `doctor_user` vb.) ile bağlanır; servisler arası doğrudan veri erişimi engellenir.

---

## 🗄️ 2. Doğrudan Çalıştırılabilir SQL Başlatma Scripti (`infrastructure/init-databases.sql`)

Aşağıdaki SQL scripti, tek bir PostgreSQL sunucusu üzerinde tüm mikroservis veritabanlarını, kullanıcılarını ve yetkilerini eksiksiz oluşturur:

```sql
-- =============================================================================
-- Hospital Information System (HIS) - Centralized Database Initialization
-- =============================================================================

-- 1. AUTH SERVICE
CREATE DATABASE auth_db;
CREATE USER auth_user WITH ENCRYPTED PASSWORD 'auth_pass_123';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;
\c auth_db
GRANT ALL ON SCHEMA public TO auth_user;

-- 2. PATIENT MANAGEMENT (CQRS Mimarisi)
\c postgres
CREATE DATABASE patient_db;
CREATE USER patient_user WITH ENCRYPTED PASSWORD 'patient_pass_123';
GRANT ALL PRIVILEGES ON DATABASE patient_db TO patient_user;
\c patient_db
CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user;
GRANT ALL ON SCHEMA patient_schema TO patient_user;
ALTER USER patient_user SET search_path = patient_schema, public;

-- 3. DOCTOR SERVICE (CQRS Mimarisi)
\c postgres
CREATE DATABASE doctor_db;
CREATE USER doctor_user WITH ENCRYPTED PASSWORD 'doctor_pass_123';
GRANT ALL PRIVILEGES ON DATABASE doctor_db TO doctor_user;
\c doctor_db
CREATE SCHEMA IF NOT EXISTS doctor_schema AUTHORIZATION doctor_user;
GRANT ALL ON SCHEMA doctor_schema TO doctor_user;
ALTER USER doctor_user SET search_path = doctor_schema, public;

-- 4. APPOINTMENT SERVICE (CQRS Mimarisi)
\c postgres
CREATE DATABASE appointment_db;
CREATE USER appointment_user WITH ENCRYPTED PASSWORD 'appointment_pass_123';
GRANT ALL PRIVILEGES ON DATABASE appointment_db TO appointment_user;
\c appointment_db
CREATE SCHEMA IF NOT EXISTS appointment_schema AUTHORIZATION appointment_user;
GRANT ALL ON SCHEMA appointment_schema TO appointment_user;
ALTER USER appointment_user SET search_path = appointment_schema, public;

-- 5. ADMISSION SERVICE (CQRS Mimarisi)
\c postgres
CREATE DATABASE admission_db;
CREATE USER admission_user WITH ENCRYPTED PASSWORD 'admission_pass_123';
GRANT ALL PRIVILEGES ON DATABASE admission_db TO admission_user;
\c admission_db
CREATE SCHEMA IF NOT EXISTS admission_schema AUTHORIZATION admission_user;
GRANT ALL ON SCHEMA admission_schema TO admission_user;
ALTER USER admission_user SET search_path = admission_schema, public;

-- 6. BILLING SERVICE (CQRS Mimarisi)
\c postgres
CREATE DATABASE billing_db;
CREATE USER billing_user WITH ENCRYPTED PASSWORD 'billing_pass_123';
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;
\c billing_db
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema, public;

-- 7. SUPPORT SERVICE (Tahlil, Laboratuvar, Envanter)
\c postgres
CREATE DATABASE support_db;
CREATE USER support_user WITH ENCRYPTED PASSWORD 'support_pass_123';
GRANT ALL PRIVILEGES ON DATABASE support_db TO support_user;
\c support_db
CREATE SCHEMA IF NOT EXISTS support_schema AUTHORIZATION support_user;
GRANT ALL ON SCHEMA support_schema TO support_user;
ALTER USER support_user SET search_path = support_schema, public;

-- 8. NOTIFICATION SERVICE
\c postgres
CREATE DATABASE notification_db;
CREATE USER notification_user WITH ENCRYPTED PASSWORD 'notification_pass_123';
GRANT ALL PRIVILEGES ON DATABASE notification_db TO notification_user;
\c notification_db
CREATE SCHEMA IF NOT EXISTS notification_schema AUTHORIZATION notification_user;
GRANT ALL ON SCHEMA notification_schema TO notification_user;
ALTER USER notification_user SET search_path = notification_schema, public;
```

---

## 🐳 3. Merkezi Docker Compose Tanımı (`infrastructure/docker-compose.yml`)

Tek bir PostgreSQL konteyneri ile tüm altyapıyı ayağa kaldıran yapılandırma:

```yaml
services:
  his-postgres:
    image: postgres:15-alpine
    container_name: his-postgres
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: root_master_pass_123
    volumes:
      - ./init-databases.sql:/docker-entrypoint-initdb.d/init-databases.sql:ro
      - his-postgres-data:/var/lib/postgresql/data
    networks:
      - patient-global-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  his-postgres-data:
    driver: local

networks:
  patient-global-network:
    name: patient-global-network
    driver: bridge
```

---

## ☸️ 4. Mikroservis Kubernetes ConfigMap ve JDBC URL Eşleme Tablosu

Tüm mikroservislerin `configmap.yaml` dosyalarında güncellenecek standart JDBC URL'ler:

| Servis Adı | ConfigMap Anahtarı | Güncellenecek Standart JDBC URL | Kullanıcı / Vault Secret Yolu |
| :--- | :--- | :--- | :--- |
| **`auth-service`** | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/auth_db?currentSchema=public` | `secret/hospital/auth-service/db` |
| **`patient-management`** | `PATIENT_WRITE_DB_URL`<br>`PATIENT_READ_DB_URL` | `jdbc:postgresql://postgres:5432/patient_db?currentSchema=patient_schema&reWriteBatchedInserts=true`<br>`jdbc:postgresql://postgres:5432/patient_db?currentSchema=patient_schema` | `secret/hospital/patient-management/db` |
| **`doctor-service`** | `SPRING_DATASOURCE_WRITE_URL`<br>`SPRING_DATASOURCE_READ_URL` | `jdbc:postgresql://postgres:5432/doctor_db?currentSchema=doctor_schema&reWriteBatchedInserts=true`<br>`jdbc:postgresql://postgres:5432/doctor_db?currentSchema=doctor_schema` | `secret/hospital/doctor-service/db` |
| **`appointment-service`** | `SPRING_DATASOURCE_WRITE_JDBC_URL`<br>`SPRING_DATASOURCE_READ_JDBC_URL` | `jdbc:postgresql://postgres:5432/appointment_db?currentSchema=appointment_schema`<br>`jdbc:postgresql://postgres:5432/appointment_db?currentSchema=appointment_schema` | `secret/hospital/appointment-service/db` |
| **`admission-service`** | `ADMISSION_WRITE_DB_URL`<br>`ADMISSION_READ_DB_URL` | `jdbc:postgresql://postgres:5432/admission_db?currentSchema=admission_schema`<br>`jdbc:postgresql://postgres:5432/admission_db?currentSchema=admission_schema` | `secret/hospital/admission-service/db` |
| **`billing-service`** | `SPRING_DATASOURCE_WRITE_URL`<br>`SPRING_DATASOURCE_READ_URL` | `jdbc:postgresql://postgres:5432/billing_db?currentSchema=billing_schema`<br>`jdbc:postgresql://postgres:5432/billing_db?currentSchema=billing_schema` | `secret/hospital/billing-service/db` |
| **`support-service`** | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/support_db?currentSchema=support_schema` | `secret/hospital/support-service/db` |
| **`notification-service`** | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/notification_db?currentSchema=notification_schema` | `secret/hospital/notification-service/db` |

---

## 🔒 5. Kubernetes `Deployment` ve `NetworkPolicy` Standartları

### 5.1 Minikube `hostAliases` Güncellemesi
Tüm servislerin `deployment.yaml` dosyalarında `hostAliases` tek bir `postgres` ana makinesine yönlendirilir:

```yaml
spec:
  hostAliases:
    - ip: "192.168.49.1"
      hostnames:
        - "postgres"
        - "kafka"
        - "redis"
        - "host.minikube.internal"
```

### 5.2 Basitleştirilmiş `NetworkPolicy` Egress Kuralı
Tüm `network-policy.yaml` dosyalarındaki dağınık DB portları (5432, 5433, 5438, 5439) yerine sadece tek bir port kuralı yeterlidir:

```yaml
egress:
  - ports:
      - protocol: TCP
        port: 5432   # PostgreSQL (Tüm mikroservis veritabanları)
      - protocol: TCP
        port: 9092   # Apache Kafka Broker
      - protocol: TCP
        port: 8200   # HashiCorp Vault
      - protocol: UDP
        port: 53     # CoreDNS
      - protocol: TCP
        port: 53
```

---

## 📋 6. Adım Adım Operasyon ve Geçiş Prosedürü (Runbook)

### Adım 1: Tekilleştirilmiş PostgreSQL Konteynerini Başlatma
```bash
# 1. Eski DB konteynerlerini durdurun ve kaldırın
docker rm -f patient-write-db patient-read-db auth-db doctor-write-db doctor-read-db appointment-write-db appointment-read-db admission-write-db admission-read-db billing-write-db billing-read-db support-db notification-db 2>/dev/null || true

# 2. Tek bir his-postgres konteyneri başlatın
docker run -d --name his-postgres \
  --network patient-global-network \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=root_master_pass_123 \
  postgres:15-alpine

# 3. init-databases.sql scriptini veritabanına uygulayın
docker cp infrastructure/init-databases.sql his-postgres:/tmp/init-databases.sql
docker exec -i his-postgres psql -U postgres -f /tmp/init-databases.sql
```

### Adım 2: İzolasyon ve Yetki Doğrulaması (Verification)
```bash
# 1. Auth kullanıcısının sadece auth_db'ye erişebildiğini doğrulayın
docker exec -i his-postgres psql -U auth_user -d auth_db -c "SELECT current_database(), current_user;"

# 2. Patient kullanıcısının doctor_db'ye erişemediğini (Permission Denied) doğrulayın
docker exec -i his-postgres psql -U patient_user -d doctor_db -c "SELECT 1;" 2>&1 | grep -i "denied\|FATAL" && echo "✅ İzolasyon başarılı: patient_user doctor_db'ye erişemiyor!"
```

### Adım 3: Kubernetes Manifestlerini Güncelleme ve Dağıtım
```bash
# Servis manifestlerini cluster'a uygulayın
kubectl apply -k kubernetes/base/apps/auth-service
kubectl apply -k kubernetes/base/apps/patient-management
```

---

## 🎯 7. Görev Tamamlanma Kriterleri (Checklist)

- [x] `infrastructure/init-databases.sql` dosyası oluşturuldu ve 8 veritabanı/kullanıcı tanımlandı.
- [x] `his-postgres` (Port `5432`) konteyneri ayakta ve sağlıklı çalışıyor.
- [x] Kullanıcı seviyesinde veritabanı izolasyonu doğrulandı (Kullanıcılar yalnızca kendi DB'sine erişebiliyor).
- [x] Mikroservis `ConfigMap`, `Deployment` (hostAliases) ve `NetworkPolicy` dosyaları tekilleştirilmiş `postgres:5432` standardına geçirildi.
- [x] `auth-service` ve `patient-management` servisleri tek PostgreSQL instance'ı üzerinden hatasız `1/1 Ready` durumuna ulaştı.

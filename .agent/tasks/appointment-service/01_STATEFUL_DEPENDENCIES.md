# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu doküman, `appointment-service` mikroservisinin ihtiyaç duyduğu PostgreSQL veritabanı şeması, Apache Kafka mesajlaşma kuyrukları ve gRPC istemci bağımlılıklarının hazırlanması adımlarını içerir.

---

## 1. PostgreSQL Konsolide Veritabanı ve Şema Yapısı

HIS mimarisinde konsolide PostgreSQL örneği (`his-postgres:5432`) kullanılmaktadır. `appointment-service` verileri `appointment_db` veritabanı altındaki `appointment_schema` şemasında tutulur.

### 1.1 Veritabanı Bağlantı ve İzolasyon Bilgileri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `postgres:5432` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Konsolide DB Container |
| **Database** | `appointment_db` | Servise özel mantıksal veritabanı |
| **Schema** | `appointment_schema` | Servise özel izole şema |
| **User** | `appointment_user` | İlgili DB ve şemaya tam yetkili kullanıcı |
| **Password** | `appointment_pass_123` | Vault üzerinden temin edilen parola |

### 1.2 Şema Tabloları

`appointment_schema` altında aşağıdaki CQRS tabloları yer alır:
1. `appointments` - Randevu ana kayıtları, hasta/doktor ID, randevu saati, durum (`PENDING`, `CONFIRMED`, `CANCELLED`).
2. `appointment_summaries` - CQRS Read Model özet tablosu.
3. `appointment_outbox_events` - Güvenilir Transactional Outbox event kuyruğu.

### 1.3 Tablo Doğrulama Komutu

```bash
docker exec -i his-postgres psql -U appointment_user -d appointment_db -c "\dt appointment_schema.*"
```

---

## 2. Apache Kafka Mesajlaşma Altyapısı

`appointment-service`, randevu oluşturma ve güncelleme süreçlerinde (Saga Orchestration & Outbox) olay üretir ve tüketir.

### 2.1 İlgili Kafka Topic Listesi

| Topic Adı | Rol | Payload / Açıklama |
| :--- | :---: | :--- |
| **`appointment-events`** | **Producer (Outbox)** | Randevu oluşturma, onaylama, iptal olaylarını yayınlar. |
| **`appointment-created.v1`** | **Producer** | Randevu oluşturulduğunda faturalama ve bildirim servisleri için üretilir. |
| **`patient-events`** | **Consumer** | Hasta bilgileri güncellendiğinde randevu özet modelini senkronize eder. |
| **`doctor-events`** | **Consumer** | Doktor bilgileri/izinleri değiştiğinde randevu özet modelini günceller. |

### 2.2 Kafka Topic Oluşturma Komutu

```bash
# Gerekli topic'lerin Kafka üzerinde oluşturulması:
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic appointment-events --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic appointment-created.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic patient-events --partitions 3 --replication-factor 1
```

---

## 3. gRPC İstemci Bağımlılıkları (Downstream Services)

`appointment-service`, randevu oluşturma Saga akışında hasta ve doktorun uygunluğunu doğrulamak için aşağıdaki servislere gRPC çağrısı yapar:

1. **`patient-management`:**
   - **Servis Adı:** `patient-management`
   - **Port:** `9090`
   - **Metot:** `GetPatientById` (Hasta varlık kontrolü)
2. **`doctor-service`:**
   - **Servis Adı:** `doctor-service`
   - **Port:** `9005`
   - **Metot:** `GetDoctorById` / `CheckAvailability` (Doktor çalışma/müsaitlik kontrolü)

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] `appointment_db` veritabanı ve `appointment_user` kullanıcısının `his-postgres` üzerinde aktif olduğu doğrulandı.
- [x] Kafka üzerinde `appointment-events`, `appointment-created.v1` ve `patient-events` topic'lerinin mevcut olduğu doğrulandı.
- [x] `patient-management` (port `9090`) ve `doctor-service` (port `9005`) gRPC bağlantı parametrelerinin hazırlandığı teyit edildi.

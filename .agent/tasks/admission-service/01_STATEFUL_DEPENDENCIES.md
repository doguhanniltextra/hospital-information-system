# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu doküman, `admission-service` mikroservisinin ihtiyaç duyduğu PostgreSQL veritabanı şeması, Redis önbellek altyapısı, Apache Kafka mesajlaşma kuyrukları ve gRPC istemci bağımlılıklarının hazırlanması adımlarını içerir.

---

## 1. PostgreSQL Konsolide Veritabanı ve Şema Yapısı

HIS mimarisinde konsolide PostgreSQL örneği (`his-postgres:5432`) kullanılmaktadır. `admission-service` verileri `admission_db` veritabanı altındaki `admission_schema` şemasında tutulur.

### 1.1 Veritabanı Bağlantı ve İzolasyon Bilgileri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `postgres:5432` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Konsolide DB Container |
| **Database** | `admission_db` | Servise özel mantıksal veritabanı |
| **Schema** | `admission_schema` | Servise özel izole şema |
| **User** | `admission_user` | İlgili DB ve şemaya tam yetkili kullanıcı |
| **Password** | `admission_pass_123` | Vault üzerinden temin edilen parola |

### 1.2 Şema Tabloları

`admission_schema` altında aşağıdaki tablolar yer alır:
1. `wards` - Klinik servis/servis katı tanımları ve günlük yatak ücretleri (`daily_rate`).
2. `rooms` - Servise bağlı oda tanımları.
3. `beds` - Odalara bağlı yataklar ve durumları (`EMPTY`, `OCCUPIED`, `CLEANING`, `MAINTENANCE`).
4. `admissions` - Hasta yatış kayıtları (`ACTIVE`, `DISCHARGED`), hasta ID, doktor ID, yatak ID, yatış ve taburcu tarihleri.
5. `admission_summaries` - CQRS Read Model özet tablosu.
6. `admission_outbox_events` - Güvenilir Transactional Outbox event kuyruğu (`DAILY_BED_CHARGE`, `PATIENT_DISCHARGED`).
7. `admission_processed_events` - İdempotency ve tekrar eden eventleri engelleme tablosu.

### 1.3 Tablo Doğrulama Komutu

```bash
docker exec -i his-postgres psql -U admission_user -d admission_db -c "\dt admission_schema.*"
```

---

## 2. Redis Önbellek Altyapısı (Cache Layer)

`admission-service`, yatış işlemleri sırasında gRPC ile sorgulanan hasta ve doktor varlık bilgilerini (`existsById`, `getPatientContactInfo`) gereksiz ağ trafiğini önlemek için Redis üzerinde önbelleğe alır.

### 2.1 Bağlantı Parametreleri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `redis:6379` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Redis Standalone Server |
| **Cache Names** | `doctorExistence`, `patientExistence`, `patientContacts` | Spring Cache yönetilen bölgeleri |

---

## 3. Apache Kafka Mesajlaşma Altyapısı

`admission-service`, yatak ücretlendirme ve hasta taburcu süreçlerinde (Transactional Outbox) olay üretir; hasta ve doktor güncellemelerini dinler.

### 3.1 İlgili Kafka Topic Listesi

| Topic Adı | Rol | Payload / Açıklama |
| :--- | :---: | :--- |
| **`admission-bed-charge.v1`** | **Producer (Outbox)** | Gece çalışan `MidnightChargeJob` tarafından aktif yatışlar için üretilen günlük yatak ücreti eventi. |
| **`admission-discharged.v1`** | **Producer (Outbox)** | Hasta taburcu edildiğinde faturalama ve temizlik süreçleri için üretilen event. |
| **`patient-events`** | **Consumer** | Hasta bilgileri güncellendiğinde özet okuma modelini senkronize eder. |
| **`doctor-events`** | **Consumer** | Doktor bilgileri güncellendiğinde özet okuma modelini senkronize eder. |

### 3.2 Kafka Topic Oluşturma Komutu

```bash
# Gerekli topic'lerin Kafka üzerinde oluşturulması:
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic admission-bed-charge.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic admission-discharged.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic patient-events --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic doctor-events --partitions 3 --replication-factor 1
```

---

## 4. gRPC İstemci Bağımlılıkları (Downstream Services)

`admission-service`, yatış oluşturma sürecinde hasta ve doktorun varlığını doğrulamak için aşağıdaki servislere gRPC çağrısı yapar:

1. **`patient-management`:**
   - **Servis Adı:** `patient-management`
   - **Port:** `9090`
   - **Metotlar:** `existsById`, `getPatientContactInfo`
2. **`doctor-service`:**
   - **Servis Adı:** `doctor-service`
   - **Port:** `9005`
   - **Metot:** `existsById`

---

## 5. Görev Tamamlanma Kriterleri (Checklist)

- [x] `admission_db` veritabanı ve `admission_user` kullanıcısının `his-postgres` üzerinde aktif olduğu doğrulandı.
- [x] Redis bağlantısının (`redis:6379`) erişilebilir olduğu teyit edildi (`PONG`).
- [x] Kafka üzerinde `admission-bed-charge.v1`, `admission-discharged.v1`, `patient-events` ve `doctor-events` topic'lerinin mevcut olduğu doğrulandı.
- [x] `patient-management` (port `9090`) ve `doctor-service` (port `9005`) gRPC servislerinin Kubernetes üzerinde sağlıklı çalıştığı teyit edildi.

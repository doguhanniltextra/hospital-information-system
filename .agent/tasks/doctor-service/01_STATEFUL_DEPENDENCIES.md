# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu doküman, `doctor-service` mikroservisinin ihtiyaç duyduğu PostgreSQL veritabanı şeması, Apache Kafka mesajlaşma kuyrukları ve gRPC servis altyapısının hazırlanması adımlarını içerir.

---

## 1. PostgreSQL Konsolide Veritabanı ve Şema Yapısı

HIS mimarisinde konsolide PostgreSQL örneği (`his-postgres:5432`) kullanılmaktadır. `doctor-service` verileri `doctor_db` veritabanı altındaki `doctor_schema` şemasında tutulur.

### 1.1 Veritabanı Bağlantı ve İzolasyon Bilgileri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `postgres:5432` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Konsolide DB Container |
| **Database** | `doctor_db` | Servise özel mantıksal veritabanı |
| **Schema** | `doctor_schema` | Servise özel izole şema |
| **User** | `doctor_user` | İlgili DB ve şemaya tam yetkili kullanıcı |
| **Password** | `doctor_pass_123` | Vault üzerinden temin edilen parola |

### 1.2 Şema Tabloları

`doctor_schema` altında aşağıdaki CQRS tabloları yer alır:
1. `doctors` - Doktor ana profil kayıtları, uzmanlık, kota ve aktiflik bilgileri.
2. `doctor_summary` - CQRS Read Model özet tablosu.
3. `shifts` - Doktor nöbet ve vardiya çizelgeleri.
4. `leave_absences` - Doktor izin ve mazeret talepleri/onayları.
5. `doctor_lab_orders` - Doktor tarafından istenen laboratuvar test siparişleri.
6. `doctor_notifications` - Doktora iletilen sistem bildirimleri.
7. `doctor_outbox_events` - Güvenilir Transactional Outbox event kuyruğu.

### 1.3 Tablo Doğrulama Komutu

```bash
docker exec -i his-postgres psql -U doctor_user -d doctor_db -c "\dt doctor_schema.*"
```

---

## 2. Apache Kafka Mesajlaşma Altyapısı

`doctor-service`, hastane olay güdümlü mimarisinde (EDA) hem Event Publisher (Üretici) hem de Event Consumer (Tüketici) rolündedir.

### 2.1 İlgili Kafka Topic Listesi

| Topic Adı | Rol | Payload / Açıklama |
| :--- | :---: | :--- |
| **`lab-order-placed.v1`** | **Producer** | Doktor laboratuvar testi talep ettiğinde üretilir. |
| **`lab-result-completed.v1`** | **Consumer** | `support-service` laboratuvar sonucunu tamamladığında doktora bildirim oluşturur. |
| **`patient-created.v1`** | **Consumer** | Yeni hasta kaydı yapıldığında doktorun baktığı hasta sayısını/kotasını günceller. |
| **`doctor-events`** | **Producer (Outbox)** | Doktor oluşturma, güncelleme, nöbet ve izin değişikliklerini yayınlar. |

### 2.2 Kafka Topic Oluşturma Komutu

```bash
# Gerekli topic'lerin Kafka üzerinde oluşturulması:
docker exec -i kafka kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic lab-order-placed.v1 --partitions 3 --replication-factor 1
docker exec -i kafka kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic lab-result-completed.v1 --partitions 3 --replication-factor 1
docker exec -i kafka kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic patient-created.v1 --partitions 3 --replication-factor 1
docker exec -i kafka kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic doctor-events --partitions 3 --replication-factor 1
```

---

## 3. gRPC Servis Altyapısı

`doctor-service`, randevu ve yatış servislerinin doktor uygunluğunu anlık ve düşük gecikmeyle sorgulaması için **gRPC** sunucusu barındırır.

* **gRPC Port:** `9005`
* **Proto Dosyası:** `doctor-service/src/main/proto/doctor_service.proto`
* **Sunucu Sınıfı:** `com.project.service.DoctorGrpcService`

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] `doctor_db` veritabanı ve `doctor_user` kullanıcısının `his-postgres` üzerinde aktif olduğu doğrulandı.
- [x] Kafka üzerinde `lab-order-placed.v1`, `lab-result-completed.v1`, `patient-created.v1` ve `doctor-events` topic'lerinin mevcut olduğu doğrulandı.
- [x] gRPC port `9005` yapılandırmasının Service manifestine dahil edilmek üzere hazırlandığı teyit edildi.

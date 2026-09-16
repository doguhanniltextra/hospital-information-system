# Task 01: Durumsal Bağımlılıkların Hazırlanması (Stateful Dependencies)

Bu doküman, `support-service` mikroservisinin ihtiyaç duyduğu PostgreSQL veritabanı şeması, Redis önbellek altyapısı ve Apache Kafka mesajlaşma kuyruklarının hazırlanması adımlarını içerir.

---

## 1. PostgreSQL Konsolide Veritabanı ve Şema Yapısı

HIS mimarisinde konsolide PostgreSQL örneği (`his-postgres:5432`) kullanılmaktadır. `support-service` verileri `support_db` veritabanı altındaki `support_schema` şemasında tutulur.

### 1.1 Veritabanı Bağlantı ve İzolasyon Bilgileri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `postgres:5432` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Konsolide DB Container |
| **Database** | `support_db` | Servise özel mantıksal veritabanı |
| **Schema** | `support_schema` | Servise özel izole şema |
| **User** | `support_user` | İlgili DB ve şemaya tam yetkili kullanıcı |
| **Password** | `support_pass_123` | Vault üzerinden temin edilen parola |

### 1.2 Şema Tabloları

`support_schema` altında aşağıdaki tablolar yer alır:
1. `lab_orders` - Laboratuvar tetkik istemleri (`PENDING`, `COMPLETED`, `CANCELLED`).
2. `lab_results` - Tetkik sonuçları, değerler, referans aralıkları ve doktor notları.
3. `inventory_items` - Tıbbi malzeme, ilaç ve stok kalemleri (`item_code`, `quantity`, `unit`, `min_threshold`).
4. `inventory_transactions` - Stok giriş/çıkış ve tüketim hareketleri.
5. `patient_identities` - Hasta kimlik ve authorization eşleme tablosu.
6. `support_outbox_events` - Güvenilir Transactional Outbox kuyruğu (`LAB_RESULT_COMPLETED`).
7. `support_processed_events` - Idempotency ve yinelenen event tüketimini engelleme tablosu.

### 1.3 Tablo Doğrulama Komutu

```bash
docker exec -i his-postgres psql -U support_user -d support_db -c "\dt support_schema.*"
```

---

## 2. Redis Önbellek Altyapısı (Cache Layer)

`support-service`, sık sorgulanan stok kalemleri ve laboratuvar tetkik kataloglarını Redis üzerinde önbelleğe alır.

### 2.1 Bağlantı Parametreleri

| Parametre | Değer | Açıklama |
| :--- | :--- | :--- |
| **Host / Port** | `redis:6379` *(K8s hostAlias $\rightarrow$ Minikube Gateway)* | Redis Standalone Server |
| **Cache Names** | `inventoryItems`, `labOrders`, `patientIdentities` | Spring Cache yönetilen bölgeleri |

---

## 3. Apache Kafka Mesajlaşma Altyapısı

`support-service`, laboratuvar ve stok tüketim olaylarını dinler; sonuç tamamlandığında dış servisler için olay üretir.

### 3.1 İlgili Kafka Topic Listesi

| Topic Adı | Rol | Payload / Açıklama |
| :--- | :---: | :--- |
| **`lab-order-placed.v1`** | **Consumer** | Randevu veya yatış sırasında hekim tarafından istenen tahlil olayları. |
| **`inventory-item-consumed.v1`** | **Consumer** | Klinik işlemler sırasında tüketilen malzeme/ilaç olayları. |
| **`patient-updated.v1`** | **Consumer** | Hasta kimlik güncellemeleri ve silme senkronizasyonu. |
| **`lab-result-completed.v1`** | **Producer (Outbox)** | Tahlil sonuçlandığında bildirim ve faturalama servisleri için üretilen event. |

### 3.2 Kafka Topic Oluşturma Komutu

```bash
# Gerekli topic'lerin Kafka üzerinde oluşturulması:
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic lab-order-placed.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic inventory-item-consumed.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic patient-updated.v1 --partitions 3 --replication-factor 1
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic lab-result-completed.v1 --partitions 3 --replication-factor 1
```

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] `support_db` veritabanı ve `support_user` kullanıcısının `his-postgres` üzerinde aktif olduğu doğrulandı.
- [x] Redis bağlantısının (`redis:6379`) erişilebilir olduğu teyit edildi (`PONG`).
- [x] Kafka üzerinde `lab-order-placed.v1`, `inventory-item-consumed.v1`, `patient-updated.v1` ve `lab-result-completed.v1` topic'lerinin mevcut olduğu doğrulandı.

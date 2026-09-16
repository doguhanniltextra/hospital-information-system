# Task 01: Stateful Bağımlılıklar ve Yerel Ön Koşullar

Bu görev, `patient-management` mikroservisinin sağlıklı çalışabilmesi için gerekli olan harici stateful bileşenlerin (PostgreSQL CQRS Write & Read Veritabanları, Apache Kafka ve gRPC altyapısı) yerel Docker ve Minikube ortamlarında yapılandırılmasını, ağ izolasyonunu, olası tuzaklara (gotchas) karşı önlemleri ve uçtan uca doğrulama adımlarını kapsar.

---

## 1. Bağımlılık Mimarisi ve Servis Kapsamı

`patient-management`, hastane bilgi yönetim sisteminin ana veri (Master Data) sağlayıcısı ve hasta yaşam döngüsünün merkezidir. Servis işlevlerini yerine getirebilmek için şu bileşenlere bağımlıdır:

1. **PostgreSQL Dual Database (CQRS Mimarisi):**
   * **Write DB (`patient-write-db`):** Hasta kaydı oluşturma, güncelleme, silme ve Outbox tabloları için ana yazma veritabanı.
   * **Read DB (`patient-read-db`):** Hasta listeleme, arama ve filtreleme sorguları için okuma veritabanı.
   * **İzole Şema:** Her iki veritabanında da `patient_schema` şeması kullanılır.
2. **Apache Kafka (`kafka`):**
   * **Üretilen Konular (Produced Topics):**
     - `patient-created.v1` (Yeni hasta oluşturulduğunda Outbox pattern ile Kafka'ya basılır; `auth-service` bu eventi dinleyerek otomatik auth kullanıcısı açar).
   * **Tüketilen Konular (Consumed Topics):**
     - `user-provisioned.v1` (`auth-service` kullanıcısı açıldığında hasta kaydını doğrular).
     - `lab-result-completed.v1` (`support-service` laboratuvar tahlil sonuçlarını iletir).
3. **gRPC Sunucusu (Port 9090):**
   * `admission-service`, `appointment-service` ve `notification-service` gibi diğer mikroservislerin senkron olarak *"Bu hasta sistemde var mı?"* sorgusunu (`patient_query.proto`) yapabilmesi için gRPC portu açık ve erişilebilir olmalıdır.

---

## 2. PostgreSQL CQRS Yapılandırması (`patient-write-db` & `patient-read-db`)

### 2.1 Konteyner Parametreleri

| Parametre | Write DB (`patient-write-db`) | Read DB (`patient-read-db`) |
| :--- | :--- | :--- |
| **Compose Dosyası** | `patient-management/docker-compose.yml` | `patient-management/docker-compose.yml` |
| **Imaj** | `postgres:15-alpine` | `postgres:15-alpine` |
| **Host Portu** | **`5432`** | **`5433`** (Konteyner içi: `5432`) |
| **Veritabanı Adı** | `patient_db` | `patient_db` |
| **Kullanıcı / Parola** | `patient_user` / `patient_pass_123` | `patient_user` / `patient_pass_123` |
| **Şema Başlatma** | `patient-management/init/write-schema.sql` | `patient-management/init/read-schema.sql` |
| **Docker Volume** | `patient-write-data` | `patient-read-data` |
| **Bağlı Docker Ağı** | `patient-global-network` | `patient-global-network` |

---

### 2.2 Port İzolasyonu ve Ön Kontroller (Port 5432 ve 5433)
Konteynerleri başlatmadan önce host üzerinde `5432` ve `5433` portlarının durumunu kontrol edin:

```bash
ss -tulpn | grep -E "5432|5433" || echo "Portlar serbest."
```

---

### 2.3 Şema Başlatma Scriptleri

#### Write DB Şeması (`patient-management/init/write-schema.sql`):
```sql
CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user;
GRANT ALL ON SCHEMA patient_schema TO patient_user;
ALTER USER patient_user SET search_path = patient_schema;
```

#### Read DB Şeması (`patient-management/init/read-schema.sql`):
```sql
CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user;
GRANT ALL ON SCHEMA patient_schema TO patient_user;
ALTER USER patient_user SET search_path = patient_schema;
```

---

### 2.4 Kritik Tuzak: "Stale Docker Volume" Davranışı (Gotcha)
PostgreSQL resmi Docker imajı `/docker-entrypoint-initdb.d/` altındaki `.sql` scriptlerini **yalnızca data volume ilk kez oluşturulurken** çalıştırır.

* **Problem:** Eğer önceden kalmış `patient-write-data` veya `patient-read-data` volume'ları varsa, konteynerler ayağa kalksa bile `init.sql` **çalıştırılmaz**. Bu durumda `patient_schema` oluşmaz ve uygulama `PSQLException: schema "patient_schema" does not exist` hatasıyla çöker.
* **Güvenli Başlatma Prosedürü:**
  ```bash
  cd /home/doguhan/SoftwareProjects/hospital-information-system/patient-management

  # 1. Ortak ağın varlığından emin olun
  docker network inspect patient-global-network >/dev/null 2>&1 || docker network create patient-global-network

  # 2. Veritabanı konteynerlerini başlatın
  docker compose up -d patient-write-db patient-read-db

  # 3. Write DB üzerinde şema kontrolü (Yoksa manuel uygulayın)
  docker exec -i patient-write-db psql -U patient_user -d patient_db -c "\dn" | grep -q "patient_schema" || \
  docker exec -i patient-write-db psql -U patient_user -d patient_db -c "CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user; GRANT ALL ON SCHEMA patient_schema TO patient_user;"

  # 4. Read DB üzerinde şema kontrolü (Yoksa manuel uygulayın)
  docker exec -i patient-read-db psql -U patient_user -d patient_db -c "\dn" | grep -q "patient_schema" || \
  docker exec -i patient-read-db psql -U patient_user -d patient_db -c "CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user; GRANT ALL ON SCHEMA patient_schema TO patient_user;"
  ```

---

## 3. Apache Kafka Yapılandırması ve Ağ Çözümü

### 3.1 Konteyner Parametreleri
* **Compose Dosyası:** `infrastructure/docker-compose.yml`
* **Konteyner Adı:** `kafka`
* **Host Portu:** `9092`
* **Mod:** KRaft modu (Zookeeper gerektirmez)

### 3.2 Minikube & Pod Ağ İletişimi (`hostAliases`)
Minikube içerisindeki `patient-management` pod'unun Docker host'undaki Kafka ve PostgreSQL'e erişebilmesi için `deployment.yaml` manifestine `hostAliases` tanımlanacaktır:

```yaml
spec:
  hostAliases:
    - ip: "192.168.49.1" # Minikube host gateway IP
      hostnames:
        - "kafka"
        - "patient-write-db"
        - "patient-read-db"
        - "host.minikube.internal"
```

### 3.3 Kafka Başlatma ve Topic Kontrolü
```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system/infrastructure
docker compose up -d kafka

# Broker sağlığını test edin
docker exec -it kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

---

## 4. Hibernate `ddl-auto=update` ve Çoklu Pod Yarış Durumu (Race Condition)

### 4.1 Risk Analizi
* `patient-management` CQRS Write DB üzerinde Hibernate `ddl-auto=update` kullanır.
* Kubernetes Deployment `replicas: 2` olarak planlanmıştır.
* **Risk:** İlk dağıtımda 2 pod aynı anda ayağa kalkarsa eşzamanlı `CREATE TABLE` / `ALTER TABLE` komutları çalıştırarak PostgreSQL seviyesinde deadlock veya constraint hatasına yol açabilir.

### 4.2 Önlem Stratejisi
1. **İlk Dağıtım:** Manifest başlangıçta `replicas: 1` ile uygulanır.
2. İlk pod tabloları (`patients`, `outbox_events` vb.) oluşturup `Running` durumuna geçtikten sonra `replicas: 2` değerine ölçeklenir:
   ```bash
   kubectl scale deployment patient-management --replicas=2
   ```

---

## 5. Hibrit Ağ ve Minikube Bağlantı Koordinatları

| Bileşen | Konteyner | Host Portu | Minikube Pod Erişim Adresi |
| :--- | :--- | :--- | :--- |
| **Write DB** | `patient-write-db` | 5432 | `jdbc:postgresql://patient-write-db:5432/patient_db?currentSchema=patient_schema` |
| **Read DB** | `patient-read-db` | 5433 | `jdbc:postgresql://patient-read-db:5432/patient_db?currentSchema=patient_schema` *(HostAliases ile)* |
| **Apache Kafka** | `kafka` | 9092 | `kafka:9092` *(HostAliases ile `192.168.49.1`)* |

---

## 6. Uçtan Uca Kontrol ve Doğrulama Scripti

Tüm ön koşulları tek seferde test etmek için aşağıdaki komutları çalıştırın:

```bash
# 1. Konteyner durumlarını kontrol et
docker ps --filter "name=patient-write-db" --filter "name=patient-read-db" --filter "name=kafka" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 2. Write DB ve Read DB şema kontrolü
docker exec -i patient-write-db psql -U patient_user -d patient_db -c "\dn"
docker exec -i patient-read-db psql -U patient_user -d patient_db -c "\dn"

# 3. Kafka broker topic listesi
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## 7. Görev Tamamlanma Kriterleri (Checklist)

- [x] `patient-global-network` Docker ağı faal.
- [x] `patient-write-db` (PostgreSQL 15) konteyneri ayakta ve `5432` portunda dinliyor (healthy).
- [x] `patient-read-db` (PostgreSQL 15) konteyneri ayakta ve `5433` portunda dinliyor (healthy).
- [x] Her iki veritabanında da `patient_schema` şeması tanımlı ve `patient_user` kullanıcısına ait.
- [x] `kafka` (3.9.0) konteyneri ayakta ve `9092` portunda hazır (`patient-created.v1`, `user-provisioned.v1`, `lab-result-completed.v1`).
- [x] Minikube `hostAliases` IP eşlemesi planlandı.

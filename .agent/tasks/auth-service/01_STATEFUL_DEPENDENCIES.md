# Task 01: Stateful Bagimliliklar ve Yerel On Kosullar

Bu gorev, `auth-service` mikroservisinin saglikli calisabilmesi icin gerekli olan harici stateful bilesenlerin (PostgreSQL ve Apache Kafka) yerel Docker ortaminda baslatilmasini, ag yapilandirmasini, olasi tuzaklara (gotchas) karsi onlemleri ve dogrulama adimlarini kapsar.

Dokuman, gorevi devralacak baska bir yapay zeka ajaninin veya sistem muhendisinin hibrit mimarideki (Docker Compose + Minikube) tum ag ve veri iletisimini sorunsuz yonetebilecegi ayrintida hazirlanmistir.

---

## 1. Bagimlilik Mimarisi ve Servis Kapsami

`auth-service`, kimlik dogrulama, kullanici yetkilendirme ve token uretimi yapan durumsuz (stateless) bir mikroservistir. Ancak islevlerini yerine getirebilmek icin su iki stateful bilesene bagimlidir:

1. **PostgreSQL (`auth-db`):**
   * Kullanici tablolari, roller, yetkiler, parola hashleri ve metaverileri saklar.
   * `auth_schema` izole semasini kullanir.
2. **Apache Kafka (`kafka`):**
   * Asenkron olay iletisimi ve merkezi loglama icin kullanilir.
   * Dinlenen Konu: `patient-created.v1` (Yeni hasta olusturuldugunda kullanici acilmasi icin).
   * Uretilen Konu: `user-provisioned.v1` (Kullanici olusturuldugunda diger servislere bildirim icin).
   * Loglama: `logback-spring.xml` loglari Kafka brokerina asenkron aktarir.
3. **Kullanilmayan Servisler:**
   * `auth-service` tarafindan **Redis** veya **MongoDB** kullanilmaz; bu servislerin calistirilmasina gerek yoktur.

---

## 2. PostgreSQL (`auth-db`) Yapilandirmasi

### 2.1 Konteyner Parametreleri
* **Compose Dosyasi:** `auth-service/docker-compose.yml`
* **Konteyner Adi:** `auth-db`
* **Imaj:** `postgres:15-alpine`
* **Host Erisim Portu:** `5438` (Konteyner ici `5432` portu host'a `5438` olarak acilmistir)
* **Veritabani Adi:** `auth_db`
* **Kullanici / Parola:** `auth_user` / `auth_pass_123`
* **Bagli Docker Agi:** `patient-global-network`

### 2.2 Port Izolasyonu ve On Kontrol (Port 5438)
Monorepo icerisindeki her servis kendi PostgreSQL konteynerine sahiptir (`patient: 5432`, `doctor: 5433`, `appointment: 5434`, vb.). `auth-service` icin `5438` portu tahsis edilmistir.

Konteyneri baslatmadan once host uzerinde `5438` portunun bos oldugu dogrulanmalidir:
```bash
ss -tulpn | grep 5438 || echo "Port 5438 serbest."
```

### 2.3 Sema Baslatma Scripti (`auth-service/init/init.sql`)
PostgreSQL veritabani ilk acildiginda `auth_user` kullanicisina ait `auth_schema` semasini olusturur:
```sql
CREATE SCHEMA IF NOT EXISTS auth_schema AUTHORIZATION auth_user;
GRANT ALL ON SCHEMA auth_schema TO auth_user;
ALTER USER auth_user SET search_path = auth_schema;
```

### 2.4 Kritik Tuzak: "Stale Docker Volume" Davranisi (Gotcha)
PostgreSQL resmi Docker imaji `/docker-entrypoint-initdb.d/` altindaki `.sql` scriptlerini **yalnizca data volume ilk kez olusturulurken** calistirir.
* **Problem:** Eger onceden kalmis bir `auth-service_auth-db-data` volume'u varsa, `docker compose up -d` komutu calissa bile `init.sql` **calistirilmaz**. Bu durumda `auth_schema` semasi olusmaz ve uygulama `PSQLException: schema "auth_schema" does not exist` hatasiyla coker.
* **Cozum ve Guvenli Baslatma Proseduru:**
  Volume'un temiz oldugundan emin olun veya semanin varligini elle sorgulayarak yoksa olusturun:
  ```bash
  cd /home/doguhan/SoftwareProjects/hospital-information-system/auth-service

  # Eger sifirdan temiz kurulum isteniyorsa:
  # docker compose down -v auth-db

  # Konteyneri baslatin
  docker compose up -d auth-db

  # Semanin varligini teyit edin, yoksa elle uygulayin
  docker exec -i auth-db psql -U auth_user -d auth_db -c "\dn" | grep -q "auth_schema" || \
  docker exec -i auth-db psql -U auth_user -d auth_db -f /docker-entrypoint-initdb.d/init.sql
  ```

---

## 3. Apache Kafka Yapilandirmasi ve Ag Cozumu

### 3.1 Konteyner Parametreleri
* **Compose Dosyasi:** `infrastructure/docker-compose.yml`
* **Konteyner Adi:** `kafka`
* **Imaj:** `apache/kafka:3.9.0`
* **Host Portu:** `9092`
* **Protokol:** KRaft modu (Zookeeper gerektirmez)

### 3.2 Kritik Tuzak: "Kafka Advertised Listeners" ve DNS Cozumleme Hatasi (Gotcha)
Hibrit mimaride (Docker uzerinde Kafka + Minikube uzerinde Pod) en sik karsilasilan sorun:
* `infrastructure/docker-compose.yml` icerisinde su tanim yer alir:
  `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092`
* **Problem:** Minikube pod'u ilk baglantiyi `host.minikube.internal:9092` uzerinden basariyla kurar. Ancak Kafka broker'i pod'a cluster metadata'sini donerken kendi advertised listener adresini (`kafka:9092`) iletir. Pod, mesaj gondermek veya dinlemek icin bir sonraki TCP soketini actiginda `kafka` adresini DNS'te cozmeye calisir ve basarisiz olur:
  `DNS resolution failed for kafka:9092`
* **Cozum Yontemleri:**
  * **Yontem A (Kubernetes HostAliases - Onerilen):**
    `kubernetes/base/apps/auth-service/deployment.yaml` manifestine pod seviyesinde `hostAliases` eklenir. Boylece pod icinde `kafka` alan adi otomatik olarak `host.minikube.internal` IP'sine cozumlenir:
    ```yaml
    spec:
      hostAliases:
        - ip: "192.168.49.1" # host.minikube.internal IP adresi
          hostnames:
            - "kafka"
    ```
  * **Yontem B (Kafka Dual Listener):**
    `infrastructure/docker-compose.yml` icindeki Kafka konfigurasyonuna harici baglantilar icin ayri bir listener (orn. `EXTERNAL://0.0.0.0:9094`) eklenmesi.

### 3.3 Kafka Baslatma Komutlari
```bash
# 1. Ortak agin mevcut oldugundan emin olun
docker network inspect patient-global-network >/dev/null 2>&1 || \
docker network create patient-global-network

# 2. infrastructure dizininden Kafka'yi baslatin
cd /home/doguhan/SoftwareProjects/hospital-information-system/infrastructure
docker compose up -d kafka

# 3. Kafka broker sagligini ve topic listesini dogrulayin
docker exec -it kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

---

## 4. Hibernate `ddl-auto=update` ve Coklu Pod Yaris Durumu (Race Condition)

### 4.1 Risk Analizi
* `application.properties` icinde `spring.jpa.hibernate.ddl-auto=update` tanimlidir.
* Kubernetes Deployment `replicas: 2` olarak planlanmistir.
* **Risk:** Eger veritabani henuz bostur ve 2 adet pod ayni anda ilk kez ayaga kalkarsa, her iki pod da eszamanli olarak `CREATE TABLE` ve `ALTER TABLE` komutlari calistirmaya kalkisir. Bu durum PostgreSQL seviyesinde `deadlock` veya `duplicate key constraint` hatalarina yol acabilir.

### 4.2 Onlem Stratejisi
1. **Ilk Dagitim Stratejisi:**
   Yeni kurulan ortamlarda `deployment.yaml` dosyasindaki replica sayisi baslangicta `1` olarak dagitilir. Ilk pod basariyla calisip tablolari olusturduktan sonra `replicas: 2` degerine olceklenir:
   ```bash
   # Tablolar olustuktan sonra olcekleme
   kubectl scale deployment auth-service --replicas=2
   ```
2. **Uretim Ortami (AWS EKS) Stratejisi:**
   Uretim ortaminda `ddl-auto` kesinlikle `validate` veya `none` moduna alinmali, sema degisiklikleri Liquibase veya Flyway Kubernetes Job'i ile tekil olarak yonetilmelidir.

---

## 5. Hibrit Ag ve Minikube Baglanti Haritasi

Minikube pod'larinin Docker host'undaki servislere ulasim koordinatlari:

| Bilesen | Docker Konteyner | Host Portu | Minikube Pod Erisim Adresi |
| :--- | :--- | :--- | :--- |
| **PostgreSQL** | `auth-db` | 5438 | `jdbc:postgresql://host.minikube.internal:5438/auth_db?currentSchema=auth_schema` |
| **Apache Kafka** | `kafka` | 9092 | `host.minikube.internal:9092` (HostAlias ile: `kafka:9092`) |

---

## 6. Uctan Uca Kontrol ve Dogrulama Scripti

Tum on kosullarin saglandigini tek komutla teyit etmek icin asagidaki adimlari calistirin:

```bash
# 1. Konteyner durumlarini kontrol et
docker ps --filter "name=auth-db" --filter "name=kafka" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 2. PostgreSQL baglanti ve sema testi
docker exec -i auth-db psql -U auth_user -d auth_db -c "\dt auth_schema.*"

# 3. Kafka broker yanit testi
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## 7. Gorev Tamamlanma Kriterleri
- [ ] `patient-global-network` Docker agi faal.
- [ ] `auth-db` (PostgreSQL 15) konteyneri ayakta ve `5438` portunda dinliyor.
- [ ] `auth_db` veritabaninda `auth_schema` semasi tanimli ve `auth_user` kullanicisina ait.
- [ ] `kafka` (3.9.0) konteyneri ayakta ve `9092` portunda hazir.
- [ ] Kafka Advertised Listeners tuzagi icin cozum plani (`hostAliases`) belirlendi.
- [ ] Hibernate eszamanli DDL yarisi onlemi dokumante edildi.

# Task 02: Kod ve Konfigürasyon Üretim Hazırlığı (Code & Configuration Readiness)

Bu doküman, `support-service` mikroservisinin [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine tam uyumlu hale getirilmesi için gerekli konfigürasyon, sağlık kontrolü, Redis önbellek ve sonlandırma parametrelerini tanımlar.

---

## 1. Konfigürasyon Ayrımı (12-Factor App & CHECK_FILE Uyum)

Tüm hassas olmayan konfigürasyonlar `ConfigMap` üzerinden, hassas sırlar (veritabanı şifresi, JWT anahtarı, iç token) ise `Vault / ExternalSecret` üzerinden çevre değişkeni olarak enjekte edilmelidir.

### 1.1 Temel Çevre Değişkenleri Matrisi

| Değişken Adı | Kaynak | Örnek / Varsayılan Değer | Açıklama |
| :--- | :--- | :--- | :--- |
| `SERVER_PORT` | ConfigMap | `8085` | HTTP API dinleme portu |
| `SPRING_DATASOURCE_WRITE_JDBC_URL` | ConfigMap | `jdbc:postgresql://postgres:5432/support_db?currentSchema=support_schema` | Write DB JDBC URL |
| `SPRING_DATASOURCE_READ_JDBC_URL` | ConfigMap | `jdbc:postgresql://postgres:5432/support_db?currentSchema=support_schema` | Read DB JDBC URL |
| `SPRING_DATA_REDIS_HOST` | ConfigMap | `redis` | Redis önbellek hostu |
| `SPRING_DATA_REDIS_PORT` | ConfigMap | `6379` | Redis önbellek portu |
| `KAFKA_BOOTSTRAP_SERVERS` | ConfigMap | `kafka:9092` | Kafka Broker adresi |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | ConfigMap | `update` *(Dev)* / `validate` *(Prod)* | Hibernate DDL modu |
| `SPRING_DATASOURCE_WRITE_USERNAME` | Vault Secret | `support_user` | Veritabanı kullanıcı adı |
| `SPRING_DATASOURCE_WRITE_PASSWORD` | Vault Secret | `support_pass_123` | Veritabanı kullanıcı parolası |
| `APP_SECRET` | Vault Secret | *(256-bit Hex Key)* | Ortak JWT doğrulama anahtarı |
| `INTERNAL_SERVICE_TOKEN` | Vault Secret | *(Shared Token)* | Dahili servis yetkilendirme anahtarı |

---

## 2. Graceful Shutdown (Zarif Kapanma)

Kubernetes pod sonlandırma sinyali (`SIGTERM`) aldığında, `support-service` mevcut HTTP ve Outbox işlemlerini kesmeden tamamlamalıdır:

```properties
# Graceful shutdown ayarları
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

* Deployment manifestindeki `terminationGracePeriodSeconds: 45` değeri ile uyumludur.

---

## 3. Kubernetes Sağlık Probları (Health & Readiness Probes)

Spring Boot Actuator üzerinden Kubernetes liveness ve readiness probları aktif edilmelidir:

```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=support-service
```

* **Liveness Probe Endpoint:** `http://<pod-ip>:8085/actuator/health/liveness`
* **Readiness Probe Endpoint:** `http://<pod-ip>:8085/actuator/health/readiness`

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] `application.properties` dosyasında graceful shutdown ve actuator probe parametrelerinin hazır olduğu doğrulandı.
- [x] Veritabanı, Redis ve Kafka bağlantı adreslerinin çevre değişkenleri ile ezilebilir formatta olduğu teyit edildi.
- [x] JWT ve `X-Internal-Token` güvenlik filtrelerinin Vault'tan gelecek çevre değişkenleriyle çalıştığı doğrulandı.

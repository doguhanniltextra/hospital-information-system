# Task 02: Kod ve Yapılandırma Hazırlığı (Code & Configuration Readiness)

Bu görev, `notification-service` mikroservisinin Spring Boot yapılandırmasını Kubernetes ve [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim standartlarına (Graceful shutdown, Actuator health probes, HikariCP havuz yönetimi, çevre değişkeni esnekliği) tam uyumlu hale getirmeyi kapsar.

---

## 1. Görev Kapsamı ve Hedefler

1. **Graceful Shutdown Yapılandırması:**
   * `server.shutdown=graceful`
   * `spring.lifecycle.timeout-per-shutdown-phase=30s`
2. **Kubernetes Actuator Health Probes:**
   * `management.endpoint.health.probes.enabled=true`
   * `management.health.livenessstate.enabled=true`
   * `management.health.readinessstate.enabled=true`
   * `management.endpoints.web.exposure.include=health,info,prometheus,metrics`
3. **Veritabanı ve HikariCP Bağlantı Ayarları:**
   * `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_WRITE_URL`, `SPRING_DATASOURCE_READ_URL` esnekliği.
   * `spring.jpa.hibernate.ddl-auto=validate` (üretim güvenliği).
   * HikariCP maksimum havuz boyutu ve zaman aşımı süreleri.
4. **gRPC ve Redis İstemci Ayarları:**
   * `grpc.client.patient-service.address: ${PATIENT_GRPC_ADDRESS:static://patient-management:9090}`
   * `spring.data.redis.host: ${REDIS_HOST:redis}`
   * `spring.data.redis.port: ${REDIS_PORT:6379}`

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: `application.yml` Dosyasını Güncellemek

`notification-service/src/main/resources/application.yml` dosyasını aşağıdaki gibi yapılandırın:

```yaml
server:
  port: 8090
  shutdown: graceful

spring:
  application:
    name: notification-service
  lifecycle:
    timeout-per-shutdown-phase: 30s
  
  datasource:
    write:
      jdbc-url: ${SPRING_DATASOURCE_WRITE_JDBC_URL:${SPRING_DATASOURCE_WRITE_URL:${SPRING_DATASOURCE_URL:jdbc:postgresql://his-postgres:5432/notification_db?currentSchema=notification_schema&reWriteBatchedInserts=true}}}
      username: ${SPRING_DATASOURCE_USERNAME:notification_user}
      password: ${SPRING_DATASOURCE_PASSWORD:notification_pass_123}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
        idle-timeout: 30000
        connection-timeout: 20000
        max-lifetime: 1800000
    read:
      jdbc-url: ${SPRING_DATASOURCE_READ_JDBC_URL:${SPRING_DATASOURCE_READ_URL:${SPRING_DATASOURCE_URL:jdbc:postgresql://his-postgres:5432/notification_db?currentSchema=notification_schema&reWriteBatchedInserts=true}}}
      username: ${SPRING_DATASOURCE_USERNAME:notification_user}
      password: ${SPRING_DATASOURCE_PASSWORD:notification_pass_123}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
        idle-timeout: 30000
        connection-timeout: 20000
        max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        default_schema: notification_schema
        dialect: org.hibernate.dialect.PostgreSQLDialect

  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour
      cache-null-values: false

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      observation-enabled: true
    listener:
      observation-enabled: true

grpc:
  client:
    patient-service:
      address: ${PATIENT_GRPC_ADDRESS:static://patient-management:9090}
      negotiation-type: plaintext

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
  tracing:
    sampling:
      probability: 1.0

ops:
  alert:
    emails: ${OPS_ALERT_EMAILS:admin@hospital.com}

services:
  patient-service:
    url: ${PATIENT_SERVICE_URL:http://patient-management:8080}

logging:
  level:
    root: INFO
    com.project.notification_service: INFO
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] Graceful shutdown (30s) yapılandırıldı.
- [x] Liveness ve Readiness actuator prob endpoint'leri etkinleştirildi.
- [x] Veritabanı ve Redis bağlantı parametreleri Kubernetes ortamına uyumlu hale getirildi.
- [x] `ddl-auto: validate` üretim moduna alındı.

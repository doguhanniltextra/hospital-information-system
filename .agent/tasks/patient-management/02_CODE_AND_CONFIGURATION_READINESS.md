# Task 02: Kod ve Konfigürasyon Hazırlığı (Code & Config Readiness)

Bu görev, `patient-management` mikroservisinin Kubernetes ortamında kesintisiz (zero-downtime) ve güvenli çalışabilmesi için Spring Boot konfigürasyonlarının, Actuator problarının, graceful shutdown ve AES şifreleme mekanizmasının hazır hale getirilmesini kapsar.

---

## 1. Konfigürasyon Gereksinimleri (`application.properties`)

### 1.1 Kubernetes & Graceful Shutdown
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
server.forward-headers-strategy=framework

# Actuator Health Probes (K8s Liveness & Readiness)
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
management.endpoints.web.exposure.include=health,info,prometheus,metrics
```

### 1.2 CQRS Veritabanı & HikariCP Pool
```properties
# Write DB
spring.datasource.write.url=${PATIENT_WRITE_DB_URL:jdbc:postgresql://patient-write-db:5432/patient_db?currentSchema=patient_schema}
spring.datasource.write.username=${SPRING_DATASOURCE_USERNAME:patient_user}
spring.datasource.write.password=${SPRING_DATASOURCE_PASSWORD:patient_pass_123}

# Read DB
spring.datasource.read.url=${PATIENT_READ_DB_URL:jdbc:postgresql://patient-read-db:5432/patient_db?currentSchema=patient_schema}
spring.datasource.read.username=${SPRING_DATASOURCE_USERNAME:patient_user}
spring.datasource.read.password=${SPRING_DATASOURCE_PASSWORD:patient_pass_123}
```

### 1.3 AES Hasta Veri Şifreleme (PII Encryption)
```properties
app.security.encryption-key=${APP_SECURITY_ENCRYPTION_KEY:MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=}
app.secret=${APP_SECRET:mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong}
```

---

## 2. Görev Tamamlanma Kriterleri

- [x] `application.properties` içerisinde graceful shutdown (`30s`), gRPC portu (`9090`) ve Actuator probes (`liveness`/`readiness`) aktif edildi.
- [x] `JwtAuthFilter` ve `EncryptionService` ortam değişkenlerini (`APP_SECRET`, `APP_SECURITY_ENCRYPTION_KEY`) doğru şekilde parametrik olarak alıyor.
- [x] Proje yerel derlemede (`./mvnw compile -DskipTests`) hatasız derlendi (`BUILD SUCCESS`).

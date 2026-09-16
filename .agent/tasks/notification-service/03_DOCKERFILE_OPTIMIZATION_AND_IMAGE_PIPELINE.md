# Task 03: Dockerfile Optimizasyonu ve İmaj Pipeline'ı (Dockerfile Optimization)

Bu görev, `notification-service` mikroservisinin Docker imajını [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine (Multi-stage build, non-root `appuser` UID 1000, JRE minimal runtime, JVM cgroup bellek bayrakları) uygun olarak optimize etmeyi ve Minikube ortamına `1.0.0` versiyonu ile aktarmayı kapsar.

---

## 1. Görev Kapsamı ve Standartlar

1. **Multi-Stage Build:**
   * Derleme aşamasında Maven ve JDK 21 kullanılır; çalışma (runtime) aşamasına yalnızca JAR dosyası taşınır.
2. **Kullanıcı Güvenliği (Non-Root User):**
   * Pod Security Standards (PSS) Restricted profili için konteyner `appuser` (UID: 1000, GID: 1000) ile çalıştırılır.
3. **JVM Optimizasyonları:**
   * `-XX:MaxRAMPercentage=75.0`
   * `-XX:InitialRAMPercentage=50.0`
   * `-XX:+UseG1GC`
   * `-XX:+ExitOnOutOfMemoryError`
4. **Port ve Entrypoint:**
   * `EXPOSE 8090`
   * Exec form `ENTRYPOINT ["java", ...]`

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: `Dockerfile` Güncelleme

`notification-service/Dockerfile` dosyasını aşağıdaki gibi yapılandırın:

```dockerfile
# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /app/target/notification-service-*.jar /app/app.jar

USER appuser

EXPOSE 8090

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "/app/app.jar"]
```

### 2.2 Adım 2: İmajı Derleme ve Minikube'e Yükleme

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system
docker build -t doguhannilt/notification-service:1.0.0 ./notification-service
minikube image load doguhannilt/notification-service:1.0.0
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [ ] Dockerfile multi-stage ve non-root (UID 1000) olarak yapılandırıldı.
- [ ] JVM cgroup parametreleri ve port 8090 tanımlandı.
- [ ] `doguhannilt/notification-service:1.0.0` imajı başarıyla derlendi ve Minikube içine aktarıldı.

# Task 03: Dockerfile Optimizasyonu ve İmaj Pipeline (Dockerfile & Image Pipeline)

Bu görev, `billing-service` için [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) güvenlik ve performans gereksinimlerine (multi-stage build, non-root `appuser`, JVM bellek ayarları, PSS Restricted uyumluluğu) tam uyumlu bir `Dockerfile` hazırlanmasını ve `1.0.0` sürüm imajının derlenip Minikube ortamına yüklenmesini kapsar.

---

## 1. Üretim Standardı `Dockerfile`

Dosya Yolu: `billing-service/Dockerfile`

```dockerfile
# Stage 1: Build & Package
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Runtime Container
FROM eclipse-temurin:21-jre-alpine AS runner

# PSS Restricted Non-root user (UID:GID 1000)
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

# Copy built artifact
COPY --from=builder --chown=appuser:appgroup /app/target/billing-service-*.jar app.jar

# Switch to non-root user
USER 1000:1000

# Expose HTTP port
EXPOSE 8081

# Container JVM flags and Entrypoint
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
```

---

## 2. `.dockerignore` Dosyası

Dosya Yolu: `billing-service/.dockerignore`

```text
target/
.git/
.idea/
*.iml
.mvn/
*.log
.env
```

---

## 3. Derleme ve Minikube Ortamına Yükleme Komutları

```bash
# 1. Proje kök dizinine geçin
cd /home/doguhan/SoftwareProjects/hospital-information-system

# 2. İmajı 1.0.0 sürüm etiketi ile derleyin
docker build -t doguhannilt/billing-service:1.0.0 ./billing-service

# 3. İmajı Minikube cluster içine aktarın
minikube image load doguhannilt/billing-service:1.0.0

# 4. Yüklenen imajı doğrulayın
minikube image ls | grep billing-service
```

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [x] `billing-service/Dockerfile` multi-stage, non-root UID 1000 ve JVM bayrakları içerecek şekilde düzenlendi.
- [x] `billing-service/.dockerignore` optimize edildi.
- [x] `doguhannilt/billing-service:1.0.0` imajı başarıyla derlendi ve Minikube içerisine aktarıldı.

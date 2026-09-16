# Task 03: Dockerfile Optimizasyonu ve İmaj Derleme

Bu görev, `patient-management` mikroservisi için çok aşamalı (multi-stage) Dockerfile optimizasyonunu, non-root kullanıcı güvenlik yapılandırmasını ve Docker imajının derlenmesini kapsar.

---

## 1. Multi-Stage Dockerfile Standardı

Maven bağımlılıklarını önbelleğe alan (caching layer) ve üretimde hafif JRE kullanan optimize `Dockerfile`:

```dockerfile
# ── Stage 1: Build & Dependencies Cache ──
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Maven Wrapper & POM önbellekleme
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Kaynak kod kopyalama & derleme
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime Minimal JRE ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root güvenlik kullanıcısı
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Derlenen JAR dosyasını kopyalama
COPY --from=builder /build/target/*.jar app.jar

# HTTP 8080 (REST) ve 9090 (gRPC) portları
EXPOSE 8080 9090

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

---

## 2. İmaj Derleme ve Doğrulama Komutları

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system
docker build -t doguhannilt/patient-management:latest ./patient-management
```

---

## 3. Görev Tamamlanma Kriterleri

- [x] Multi-stage Dockerfile oluşturuldu ve optimize edildi.
- [x] Non-root `appuser` kullanıcısı ile çalıştığı doğrulandı.
- [x] `doguhannilt/patient-management:latest` imajı başarıyla derlendi, Docker Hub'a pushlandı ve Minikube'a yüklendi.

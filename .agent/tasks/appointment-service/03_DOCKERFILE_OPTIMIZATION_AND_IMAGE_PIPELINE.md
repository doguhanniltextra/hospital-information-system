# Task 03: Dockerfile Optimizasyonu ve İmaj Pipeline (Dockerfile & Image Pipeline)

Bu doküman, `appointment-service` mikroservisinin Docker imajının [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) güvenlik ve performans gereksinimlerine göre derlenmesi, non-root kullanıcıyla çalıştırılması ve Minikube ortamına yüklenmesi adımlarını tanımlar.

---

## 1. Dockerfile Standardı (Multi-Stage & Non-Root)

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/appointment-service-1.0-SNAPSHOT.jar ./app.jar

USER appuser

EXPOSE 8084

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
```

### Güvenlik ve Performans Özellikleri:
- **Multi-Stage Build:** Derleme araçları (Maven, JDK) nihai çalışma zamanı (runtime) imajından çıkarılmıştır.
- **Non-Root Kullanıcı:** `appuser` (UID/GID non-zero) ile çalışarak `runAsNonRoot: true` PSS kuralını destekler.
- **JVM Container Optimizasyonu:** `MaxRAMPercentage=75.0` ve `ExitOnOutOfMemoryError` ile Kubernetes memory limitlerine uyum sağlar.
- **Port:** HTTP API için `8084`.

---

## 2. İmaj Derleme ve Minikube Yükleme Komutları

```bash
# 1. İmajı 1.0.0 versiyon etiketi ile derle
cd /home/doguhan/SoftwareProjects/hospital-information-system
docker build -t doguhannilt/appointment-service:1.0.0 ./appointment-service

# 2. Minikube cluster içine yükle
minikube image load doguhannilt/appointment-service:1.0.0
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] Dockerfile'ın multi-stage ve non-root kullanıcı içerdiği doğrulandı.
- [x] `doguhannilt/appointment-service:1.0.0` imajı başarıyla derlendi.
- [x] İmaj Minikube cluster'a `minikube image load` ile aktarıldı.

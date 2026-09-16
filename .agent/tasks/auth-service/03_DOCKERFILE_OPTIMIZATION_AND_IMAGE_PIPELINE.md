# Task 03: Dockerfile Optimizasyonu ve Imaj Politikasi

Bu gorev, `auth-service` uygulamasinin uretim standardinda (production-ready), guvenli, kucuk boyutlu ve CI/CD sureclerinde hizli derlenebilir (katman onbellekli) bir Docker imajina donusturulmesini kapsar.

---

## 1. Mevcut Durum ve Problem

Mevcut `auth-service/Dockerfile`:
```dockerfile
COPY . .
RUN mvn clean package -Dmaven.test.skip=true
```
* **Problem:** Uygulama kaynak kodunda tek bir satir degistiginde bile Docker tum katmani gecersiz saymakta ve `pom.xml` icindeki tum bagimliliklari sifirdan indirmektedir. Bu durum CI/CD surelerini dakikalarca uzatir.

---

## 2. Optimize Edilmis Cok Asamali (Multi-Stage) Dockerfile

* **Dosya Yolu:** `auth-service/Dockerfile`

```dockerfile
# ===================================================================
# Asama 1: Derleme Katmani (Builder)
# ===================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Oncelikle sadece pom.xml kopyalanarak bagimliliklar onbellek katmanina alinir
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Kaynak kod kopyalanip birim testler haric tutularak JAR paketi olusturulur
COPY src ./src
RUN mvn clean package -DskipTests

# ===================================================================
# Asama 2: Calisma Zamani Katmani (Runner)
# ===================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Kurumsal Guvenlik: Root yetkileri kaldirilir, non-root kullanici tanimlanir
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Derlenen JAR dosyasini calisma katmanina tasi ve yetkileri ayarla
COPY --from=builder /app/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar
RUN chown -R appuser:appgroup /app

# Guvenlik prensibi geregi appuser kullanicisina gecilir
USER appuser

EXPOSE 8089

# JVM Konteyner Bellek ve Performans Ayarlari
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
```

---

## 3. Imaj Etiketleme (Tagging) Standardi

* **Depo Adi:** `doguhannilt/auth-service`
* **Etiket Formatlari:**
  1. `doguhannilt/auth-service:latest`: En guncel stabil surum.
  2. `doguhannilt/auth-service:sha-<7-karakter-sha>`: Ilgili git commit'ine karsilik gelen imaj (GitOps takibi icin zorunlu).
  3. `doguhannilt/auth-service:build-<ci-run-no>`: GitHub Actions calisma numarasi.

---

## 4. Yerel Derleme ve Test Komutlari

```bash
# 1. auth-service dizinine gidin
cd /home/doguhan/SoftwareProjects/hospital-information-system/auth-service

# 2. Imaji yerel olarak derleyin
docker build -t doguhannilt/auth-service:latest -t doguhannilt/auth-service:test .

# 3. Katman onbellegini test edin (src icinde onemsiz bir dosya degistirip tekrar derleyin; Maven paketlerinin tekrar indirilmedigini gorun)
docker build -t doguhannilt/auth-service:latest .

# 4. Docker Hub'a gonderim (Gerektiginde)
# docker login
# docker push doguhannilt/auth-service:latest
```

---

## 5. Gorev Tamamlanma Kriterleri
- [x] `Dockerfile` iki asamali (builder / runner) yapiya gecirildi.
- [x] `pom.xml` ve `dependency:go-offline` ile katman onbellegi optimize edildi.
- [x] `appuser:appgroup` (non-root) kullanicisi tanimlandi.
- [x] JVM bellek bayraklari (`MaxRAMPercentage=75.0`) eklendi.
- [x] Imaj basariyla derlendi (doguhannilt/auth-service:latest ve :test).


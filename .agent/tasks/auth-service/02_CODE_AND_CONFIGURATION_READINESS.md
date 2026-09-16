# Task 02: Kaynak Kod ve Konfigurasyon Hazirliklari

Bu gorev, `auth-service` uygulamasinin Kubernetes pod yasam dongusune (pod lifecycle), ingress yonlendirmelerine ve sifir kesintili (zero-downtime) yuvarlanan guncellemelere (rolling updates) uyumlu hale getirilmesi icin gereken konfigurasyon duzenlemelerini kapsar.

---

## 1. Duzenlenecek Dosya

* **Dosya Yolu:** `auth-service/src/main/resources/application.properties`

---

## 2. Eklenecek Konfigurasyonlar ve Gerekceleri

### 2.1 Zarif Kapanma (Graceful Shutdown)
Kubernetes bir pod'u sonlandirirken (`SIGTERM` sinyali gonderdiginde), pod'un o an islemekte oldugu JWT dogrulama veya veritabani islemlerini aniden kesmemesi gerekir.

```properties
# Pod kapatma sinyali geldiginde yeni istek kabulunu durdurur, mevcut istekleri tamamlar
server.shutdown=graceful

# Mevcut isteklerin sonlanmasi icin taninan maksimum sure (30 saniye)
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### 2.2 Reverse Proxy ve Ingress Baslik Destegi
API Gateway veya Nginx Ingress arkasinda calisirken, istemcinin gercek IP adresi ve protokol bilgisi (`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`) basliklariyla iletilir. Spring WebMVC'nin bu basliklari dogru islemesi icin:

```properties
server.forward-headers-strategy=framework
```

### 2.3 Kubernetes Actuator Saglik Problari
Kubernetes `kubelet` araci, pod'un yasayip yasamadigini ve trafik almaya hazir olup olmadigini ozel state endpoint'leri uzerinden denetler:

```properties
# Kubernetes liveness ve readiness problarini aktif eder
management.endpoint.health.probes.enabled=true

# /actuator/health/liveness endpoint'ini sunar
management.health.livenessstate.enabled=true

# /actuator/health/readiness endpoint'ini sunar
management.health.readinessstate.enabled=true
```

### 2.4 HikariCP Baglanti Havuzu Dayanikliligi
Ilk acilista veritabani pod'dan birkac saniye sonra hazir hale gelse bile uygulamanin `BeanCreationException` ile aninda cokmesini (CrashLoop) onlemek icin:

```properties
spring.datasource.hikari.initialization-fail-timeout=0
```

---

## 3. `application.properties` Dosyasinin Nihai Sekli

Dosyanin sonuna eklenecek blok su sekildedir:

```properties
# ===================================================================
# Kubernetes Readiness, Liveness & Graceful Shutdown
# ===================================================================
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
server.forward-headers-strategy=framework

management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

---

## 4. Dogrulama Adimlari

Yapilan konfigurasyon degisikliklerinin mevcut birim ve entegrasyon testlerini kirmadigini dogrulamak icin Maven test paketi calistirilmalidir:

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system/auth-service
./mvnw clean test
```

Beklenen sonuc: Tum testler (controller, helper, job, service paketlerindekiler) basariyla tamamlanmalidir (`BUILD SUCCESS`).

---

## 5. Gorev Tamamlanma Kriterleri
- [x] `application.properties` dosyasina graceful shutdown ayarlari eklendi.
- [x] `server.forward-headers-strategy=framework` tanimlandi.
- [x] Actuator Kubernetes probe endpoint'leri aktif edildi.
- [x] Maven test paketi hatasiz calisti (16/16 test gecti, BUILD SUCCESS).


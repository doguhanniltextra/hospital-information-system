# Task 06: Dagitim, Dogrulama ve Sorun Giderme Kilavuzu

Bu gorev, tum hazirliklar tamamlandiktan sonra `auth-service` mikroservisinin yerel Minikube kumesine dagitilmasi, calisma sagliginin dogrulanmasi, API Gateway uzerinden ucuca test edilmesi ve karsilasabilecek muhtemel hatalarin cozumunu kapsar.

---

## 1. Dagitim Oncesi Kontrol Listesi

Dagitima baslamadan once su adimlarin tamamlandigini teyit edin:
- [ ] Task 01: `auth-db` (Port 5438) ve `kafka` (Port 9092) Docker uzerinde calisiyor.
- [ ] Task 02: `application.properties` dosyasina Actuator problari ve graceful shutdown eklendi.
- [ ] Task 03: `Dockerfile` katman onbellekli hale getirildi ve `doguhannilt/auth-service:latest` imaji derlendi.
- [ ] Task 04: `kubernetes/base/apps/auth-service/` altindaki tum manifestler olusturuldu.

---

## 2. Minikube Kumesine Dagitim Adimlari

### 2.1 Yerel Imaj Yukleme
Eger imaj Docker Hub'a gonderilmeden yerel Minikube kumesinde test edilecekse, Minikube Docker daemon ortaminda derlenmeli veya imaj Minikube'e aktarilmalidir:

```bash
# Secenek A: Minikube icine dogrudan yukleme
minikube image load doguhannilt/auth-service:latest

# Secenek B: Minikube Docker daemon'unu kullanarak dogrudan derleme
eval $(minikube docker-env)
cd /home/doguhan/SoftwareProjects/hospital-information-system/auth-service
docker build -t doguhannilt/auth-service:latest .
eval $(minikube docker-env -u)
```

### 2.2 Manifestlerin Kümeye Uygulanmasi
```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system
kubectl apply -k kubernetes/base/apps/auth-service/
```

### 2.3 Rollout Durumunun Takibi
```bash
kubectl rollout status deployment/auth-service --timeout=180s
```

---

## 3. Saglik ve Calisma Dogrulamasi

### 3.1 Pod ve Servis Durumunu Sorgulama
```bash
kubectl get pods -l app.kubernetes.io/name=auth-service -o wide
kubectl get svc -l app.kubernetes.io/name=auth-service
```
Beklenen cikti:
* 2 adet pod'un `1/1 Running` durumunda olmasi.
* `auth-service` ClusterIP servisinin port `8089` ile tanimli olmasi.

### 3.2 Liveness ve Readiness Problarini Test Etme
```bash
# Ilk auth-service pod adini bulun
POD_NAME=$(kubectl get pods -l app.kubernetes.io/name=auth-service -o jsonpath='{.items[0].metadata.name}')

# Pod icinden liveness probe test edin
kubectl exec -it $POD_NAME -- wget -qO- http://localhost:8089/actuator/health/liveness

# Pod icinden readiness probe test edin (Veritabani baglantisi kontrol edilir)
kubectl exec -it $POD_NAME -- wget -qO- http://localhost:8089/actuator/health/readiness
```

Beklenen cikti:
`{"status":"UP"}`

### 3.3 API Gateway ve Ingress Uzerinden Ucuca Test
`api-gateway` uzerinden auth-service rotasina istek gondererek trafigin gectigini teyit edin:

```bash
# Ingress adresi his.local uzerinden auth uclarini sorgulayin
curl -i http://his.local/actuator/health
```

---

## 4. Sik Karsilasilan Sorunlar ve Cozumleri (Troubleshooting)

### 4.1 Pod `CrashLoopBackOff` Durumunda Kalirsa
* **Muhtemel Neden 1: PostgreSQL Baglanti Hatasi**
  * Hata: `org.postgresql.util.PSQLException: Connection to host.minikube.internal:5438 refused`
  * Cozum: Host makinede `auth-db` konteynerinin calistigini ve 5438 portunun acik oldugunu teyit edin (`docker ps | grep auth-db`). Minikube'un host network erisimini test edin:
    ```bash
    kubectl run netshoot --rm -i --tty --image nicolaka/netshoot -- nc -zv host.minikube.internal 5438
    ```
* **Muhtemel Neden 2: Schema Yetki Hatasi**
  * Hata: `permission denied for schema auth_schema`
  * Cozum: `init.sql` scriptinin calistigini dogrulayin (`GRANT ALL ON SCHEMA auth_schema TO auth_user;`).

### 4.2 Pod `Running` Fakat `0/1 Ready` Durumunda Kalirsa
* **Muhtemel Neden: Readiness Probe Timeout**
  * HikariCP havuzu baglanti kurmaya calisirken timeout yiyorsa `startupProbe` failureThreshold degerini artirin veya veritabani yanit suresini kontrol edin.

### 4.3 Kafka Baglanti Uyari Loglari
* Pod calisir durumda olsa da loglarda Kafka uyarilari gorulebilir (`DNS resolution failed for kafka` veya `broker disconnected`).
* `infrastructure` altindaki `kafka` konteynerinin calistigindan emin olun (`docker compose up -d kafka`).

---

## 5. Gorev Tamamlanma Kriterleri
- [ ] 2 replica pod `1/1 Running` durumuna ulasti.
- [ ] `/actuator/health/readiness` ve `liveness` uclari `{"status":"UP"}` dondurdu.
- [ ] `api-gateway` uzerinden rota erisimi saglandi.

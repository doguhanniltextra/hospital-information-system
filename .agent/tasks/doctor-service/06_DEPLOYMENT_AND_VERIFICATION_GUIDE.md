# Task 06: Kubernetes Dağıtımı ve Uçtan Uca Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu doküman, `doctor-service` mikroservisinin Kubernetes kümesine dağıtılması, ExternalSecret senkronizasyonu, sağlık kontrolleri, gRPC port doğrulaması, REST API işlemleri ve Kafka olay akışının test edilmesine yönelik çalıştırılabilir operasyonel komutları içerir.

---

## 1. Kubernetes Kümesine Dağıtım (Deployment)

```bash
# 1. Manifestoları uygula
kubectl apply -k kubernetes/base/apps/doctor-service

# 2. ExternalSecret senkronizasyonunu doğrula
kubectl get externalsecret doctor-service-secrets

# 3. Rollout durumunu izle
kubectl rollout status deployment/doctor-service --timeout=90s
```

---

## 2. Pod Durumu ve Sağlık Kontrolleri (Health Checks)

```bash
# 1. Pod durumunu ve IP adresini sorgula
kubectl get pods -l app.kubernetes.io/name=doctor-service -o wide

# 2. Port-forward başlat
kubectl port-forward svc/doctor-service 8083:8083 9005:9005 &
PID=$!
sleep 2

# 3. Liveness ve Readiness Problarını sorgula
curl -s http://localhost:8083/actuator/health/liveness | jq .
curl -s http://localhost:8083/actuator/health/readiness | jq .

# 4. Genel sistem sağlığını sorgula
curl -s http://localhost:8083/actuator/health | jq .
```

---

## 3. PostgreSQL Konsolide Veritabanı ve Şema Doğrulaması

```bash
# PostgreSQL doctor_db içerisindeki tabloların otomatik oluştuğunu doğrula
docker exec -i his-postgres psql -U doctor_user -d doctor_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema='doctor_schema';"
```

---

## 4. API Gateway Üzerinden REST API Doğrulaması

### 4.1 Doktor Kaydı Oluşturma (`POST /api/doctors`)

```bash
curl -X POST http://localhost:8080/api/doctors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dr. Ayse Yilmaz",
    "email": "ayse.yilmaz@hospital.com",
    "number": "5551234567",
    "specialization": "CARDIOLOGY",
    "yearsOfExperience": 10,
    "hospitalName": "Merkez Hastanesi",
    "department": "Kardiyoloji",
    "licenseNumber": 12345,
    "available": true,
    "patientCount": 0,
    "maxPatientCount": 20,
    "authUserId": "a0000000-0000-0000-0000-000000000001"
  }' | jq .
```

### 4.2 Doktor Listeleme ve Arama (`GET /api/doctors`)

```bash
curl -s http://localhost:8080/api/doctors | jq .
```

---

## 5. Kafka Olay Akışı ve Outbox Doğrulaması

Doktor oluşturulduğunda Transactional Outbox tablosu üzerinden Kafka'ya event gönderildiğini doğrulayın:

```bash
# Outbox tablosundaki kayıtları kontrol et
docker exec -i his-postgres psql -U doctor_user -d doctor_db -c "SELECT id, event_type, status, created_at FROM doctor_schema.doctor_outbox_events LIMIT 5;"
```

---

## 6. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `doctor-service` pod'unun `1/1 Running` durumunda olduğu teyit edildi.
- [ ] `doctor-service-secret` ExternalSecret kaynağının `SecretSynced: True` olduğu doğrulandı.
- [ ] `/actuator/health/liveness` ve `/actuator/health/readiness` uçlarının `UP` döndüğü doğrulandı.
- [ ] `doctor_schema` altındaki tabloların başarıyla yüklendiği ve sorgulanabildiği görüldü.
- [ ] Doktor kayıt ve listeleme API isteklerinin başarılı olduğu test edildi.

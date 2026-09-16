# Task 06: Kubernetes Dağıtımı ve Uçtan Uca Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu doküman, `appointment-service` mikroservisinin Kubernetes kümesine dağıtılması, ExternalSecret senkronizasyonu, sağlık kontrolleri, gRPC bağımlılıkları, REST API işlemleri ve Kafka olay akışının test edilmesine yönelik çalıştırılabilir operasyonel komutları içerir.

---

## 1. Kubernetes Kümesine Dağıtım (Deployment)

```bash
# 1. Manifestoları uygula
kubectl apply -k kubernetes/base/apps/appointment-service

# 2. ExternalSecret senkronizasyonunu doğrula
kubectl get externalsecret appointment-service-vault-secret

# 3. Rollout durumunu izle
kubectl rollout status deployment/appointment-service --timeout=90s
```

---

## 2. Pod Durumu ve Sağlık Kontrolleri (Health Checks)

```bash
# 1. Pod durumunu ve IP adresini sorgula
kubectl get pods -l app.kubernetes.io/name=appointment-service -o wide

# 2. Port-forward başlat
kubectl port-forward svc/appointment-service 8084:8084 &
PF_PID=$!
sleep 2

# 3. Liveness ve Readiness Problarını sorgula
curl -s http://localhost:8084/actuator/health/liveness | jq .
curl -s http://localhost:8084/actuator/health/readiness | jq .

# 4. Genel sistem sağlığını sorgula
curl -s http://localhost:8084/actuator/health | jq .
```

---

## 3. PostgreSQL Konsolide Veritabanı ve Şema Doğrulaması

```bash
# PostgreSQL appointment_db içerisindeki tabloların otomatik oluştuğunu doğrula
docker exec -i his-postgres psql -U appointment_user -d appointment_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema='appointment_schema';"
```

---

## 4. API Gateway Üzerinden REST API Doğrulaması (Saga Akışı)

### 4.1 Randevu Oluşturma (`POST /api/appointments`)

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "p0000000-0000-0000-0000-000000000001",
    "doctorId": "d0000000-0000-0000-0000-000000000001",
    "appointmentDate": "2026-10-15T10:30:00",
    "serviceType": "EXAMINATION",
    "notes": "Rutin kontrol muayenesi"
  }' | jq .
```

### 4.2 Randevu Listeleme (`GET /api/appointments`)

```bash
curl -s http://localhost:8080/api/appointments | jq .
```

---

## 5. Kafka Olay Akışı ve Outbox Doğrulaması

Randevu oluşturulduğunda Transactional Outbox tablosu üzerinden Kafka'ya event gönderildiğini doğrulayın:

```bash
# Outbox tablosundaki kayıtları kontrol et
docker exec -i his-postgres psql -U appointment_user -d appointment_db -c "SELECT id, event_type, status, created_at FROM appointment_schema.appointment_outbox_events LIMIT 5;"
```

---

## 6. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `appointment-service` pod'unun `1/1 Running` durumunda olduğu teyit edildi.
- [ ] `appointment-service-vault-secret` ExternalSecret kaynağının `SecretSynced: True` olduğu doğrulandı.
- [ ] `/actuator/health/liveness` ve `/actuator/health/readiness` uçlarının `UP` döndüğü doğrulandı.
- [ ] `appointment_schema` altındaki tabloların başarıyla yüklendiği ve sorgulanabildiği görüldü.
- [ ] Randevu oluşturma Saga akışının (gRPC hasta ve doktor kontrolü ile) başarılı olduğu test edildi.

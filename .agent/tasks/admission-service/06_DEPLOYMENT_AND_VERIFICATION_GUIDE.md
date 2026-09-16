# Task 06: Kubernetes Dağıtımı ve Uçtan Uca Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu doküman, `admission-service` mikroservisinin Kubernetes kümesine dağıtılması, ExternalSecret senkronizasyonu, sağlık kontrolleri, Redis önbellek, gRPC bağımlılıkları, REST API işlemleri ve Kafka olay akışının test edilmesine yönelik çalıştırılabilir operasyonel komutları içerir.

---

## 1. Kubernetes Kümesine Dağıtım (Deployment)

```bash
# 1. Manifestoları uygula
kubectl apply -k kubernetes/base/apps/admission-service

# 2. ExternalSecret senkronizasyonunu doğrula
kubectl get externalsecret admission-service-vault-secret

# 3. Rollout durumunu izle
kubectl rollout status deployment/admission-service --timeout=90s
```

---

## 2. Pod Durumu ve Sağlık Kontrolleri (Health Checks)

```bash
# 1. Pod durumunu ve IP adresini sorgula
kubectl get pods -l app.kubernetes.io/name=admission-service -o wide

# 2. Port-forward başlat
kubectl port-forward svc/admission-service 8086:8086 &
PF_PID=$!
sleep 2

# 3. Liveness ve Readiness Problarını sorgula
curl -s http://localhost:8086/actuator/health/liveness | jq .
curl -s http://localhost:8086/actuator/health/readiness | jq .

# 4. Genel sistem sağlığını sorgula
curl -s http://localhost:8086/actuator/health | jq .
```

---

## 3. PostgreSQL Konsolide Veritabanı ve Şema Doğrulaması

```bash
# PostgreSQL admission_db içerisindeki tabloların otomatik oluştuğunu doğrula
docker exec -i his-postgres psql -U admission_user -d admission_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema='admission_schema';"
```

---

## 4. API Gateway Üzerinden REST API Doğrulaması

### 4.1 Hasta Yatışı Yapma (`POST /api/admissions/admit`)

```bash
curl -X POST http://localhost:8080/api/admissions/admit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" \
  -d '{
    "patientId": "p0000000-0000-0000-0000-000000000001",
    "doctorId": "d0000000-0000-0000-0000-000000000001",
    "wardId": "w0000000-0000-0000-0000-000000000001"
  }' | jq .
```

### 4.2 Aktif Yatışları Listeleme (`GET /api/admissions/active`)

```bash
curl -s http://localhost:8080/api/admissions/active \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" | jq .
```

### 4.3 Hasta Taburcu Etme (`PUT /api/admissions/{id}/discharge`)

```bash
curl -X PUT http://localhost:8080/api/admissions/<ADMISSION_ID>/discharge \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" | jq .
```

---

## 5. Kafka Olay Akışı ve Outbox Doğrulaması

Hasta taburcu edildiğinde veya gece yatak ücretlendirmesi çalıştığında Transactional Outbox tablosu üzerinden Kafka'ya event gönderildiğini doğrulayın:

```bash
# Outbox tablosundaki kayıtları kontrol et
docker exec -i his-postgres psql -U admission_user -d admission_db -c "SELECT id, event_type, status, created_at FROM admission_schema.admission_outbox_events LIMIT 5;"
```

---

## 6. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `admission-service` pod'unun `1/1 Running` durumunda olduğu teyit edildi.
- [ ] `admission-service-vault-secret` ExternalSecret kaynağının `SecretSynced: True` olduğu doğrulandı.
- [ ] `/actuator/health/liveness` ve `/actuator/health/readiness` uçlarının `UP` döndüğü doğrulandı.
- [ ] `admission_schema` altındaki tabloların başarıyla yüklendiği ve sorgulanabildiği görüldü.
- [ ] Hasta yatış ve taburcu işlemlerinin (gRPC hasta ve doktor kontrolü ile) başarılı olduğu test edildi.
- [ ] Outbox mekanizmasının eventleri Kafka'ya başarıyla aktardığı teyit edildi.

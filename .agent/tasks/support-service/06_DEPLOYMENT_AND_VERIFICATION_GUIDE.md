# Task 06: Kubernetes Dağıtımı ve Uçtan Uca Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu doküman, `support-service` mikroservisinin Kubernetes kümesine dağıtılması, ExternalSecret senkronizasyonu, sağlık kontrolleri, Redis önbellek, REST API işlemleri ve Kafka olay akışının test edilmesine yönelik çalıştırılabilir operasyonel komutları içerir.

---

## 1. Kubernetes Kümesine Dağıtım (Deployment)

```bash
# 1. Manifestoları uygula
kubectl apply -k kubernetes/base/apps/support-service

# 2. ExternalSecret senkronizasyonunu doğrula
kubectl get externalsecret support-service-vault-secret

# 3. Rollout durumunu izle
kubectl rollout status deployment/support-service --timeout=90s
```

---

## 2. Pod Durumu ve Sağlık Kontrolleri (Health Checks)

```bash
# 1. Pod durumunu ve IP adresini sorgula
kubectl get pods -l app.kubernetes.io/name=support-service -o wide

# 2. Port-forward başlat
kubectl port-forward svc/support-service 8085:8085 &
PF_PID=$!
sleep 2

# 3. Liveness ve Readiness Problarını sorgula
curl -s http://localhost:8085/actuator/health/liveness | jq .
curl -s http://localhost:8085/actuator/health/readiness | jq .

# 4. Genel sistem sağlığını sorgula
curl -s http://localhost:8085/actuator/health | jq .
```

---

## 3. PostgreSQL Konsolide Veritabanı ve Şema Doğrulaması

```bash
# PostgreSQL support_db içerisindeki tabloların otomatik oluştuğunu doğrula
docker exec -i his-postgres psql -U support_user -d support_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema='support_schema';"
```

---

## 4. API Gateway Üzerinden REST API Doğrulaması

### 4.1 Stok Kalemi Tanımlama (`POST /api/inventory/items`)

```bash
curl -X POST http://localhost:8080/api/inventory/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" \
  -d '{
    "itemCode": "MED-PARACETAMOL-500",
    "name": "Paracetamol 500mg Tablet",
    "category": "MEDICATION",
    "quantity": 500,
    "unit": "TABLET",
    "minThreshold": 50
  }' | jq .
```

### 4.2 Stok Listesini Sorgulama (`GET /api/inventory/items`)

```bash
curl -s http://localhost:8080/api/inventory/items \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" | jq .
```

### 4.3 Laboratuvar Sonucu Girişi (`POST /api/lab/results`)

```bash
curl -X POST http://localhost:8080/api/lab/results \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>" \
  -d '{
    "orderId": "<LAB_ORDER_ID>",
    "testName": "Tam Kan Sayımı (Hemogram)",
    "resultValue": "14.2 g/dL",
    "referenceRange": "12.0 - 16.0",
    "unit": "g/dL",
    "technicianNotes": "Parametreler normal sınırlarda."
  }' | jq .
```

---

## 5. Kafka Olay Akışı ve Outbox Doğrulaması

Tahlil sonucu tamamlandığında Transactional Outbox tablosu üzerinden `lab-result-completed.v1` konusuna event aktarıldığını doğrulayın:

```bash
# Outbox tablosundaki kayıtları kontrol et
docker exec -i his-postgres psql -U support_user -d support_db -c "SELECT id, event_type, status, created_at FROM support_schema.support_outbox_events LIMIT 5;"
```

---

## 6. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `support-service` pod'unun `1/1 Running` durumunda olduğu teyit edildi.
- [ ] `support-service-vault-secret` ExternalSecret kaynağının `SecretSynced: True` olduğu doğrulandı.
- [ ] `/actuator/health/liveness` ve `/actuator/health/readiness` uçlarının `UP` döndüğü doğrulandı.
- [ ] `support_schema` altındaki tabloların başarıyla yüklendiği ve sorgulanabildiği görüldü.
- [ ] Envanter ve Laboratuvar REST API uçlarının başarıyla test edildiği teyit edildi.
- [ ] Outbox mekanizmasının eventleri Kafka'ya başarıyla aktardığı teyit edildi.

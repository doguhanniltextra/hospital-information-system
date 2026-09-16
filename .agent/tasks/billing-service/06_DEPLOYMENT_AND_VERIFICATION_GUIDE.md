# Task 06: Dağıtım, Entegrasyon ve Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu kılavuz, `billing-service` mikroservisinin Kubernetes kümesine dağıtılması, HashiCorp Vault sırlarının senkronizasyonu, Actuator sağlık kontrolleri ve Kafka event-driven faturalama akışlarının uçtan uca test edilmesi adımlarını içerir.

---

## 1. Kubernetes Kümesine Dağıtım (Deployment)

```bash
# 1. Manifestleri uygulayın
kubectl apply -k kubernetes/base/apps/billing-service

# 2. ExternalSecret senkronizasyonunu denetleyin
kubectl get externalsecrets billing-service-vault-secret

# 3. Pod rollout durumunu izleyin
kubectl rollout status deployment/billing-service --timeout=90s

# 4. Pod durumunu kontrol edin
kubectl get pods -l app.kubernetes.io/name=billing-service -o wide
```

---

## 2. Sağlık ve Metrik Kontrolleri (Health & Metrics)

Pod içerisinden Actuator uç noktalarını sorgulayın:

```bash
# Genel Sağlık (DB, Liveness, Readiness)
kubectl exec -i deployment/billing-service -- wget -qO- http://localhost:8081/actuator/health

# Prometheus Metrikleri
kubectl exec -i deployment/billing-service -- wget -qO- http://localhost:8081/actuator/prometheus | grep jvm_memory
```

* **Beklenen Çıktı:**
  - `status: UP`
  - `db.status: UP`
  - `livenessState: UP`
  - `readinessState: UP`

---

## 3. Veritabanı ve Şema Doğrulaması

Konsolide PostgreSQL üzerinde `billing_schema` tablolarını sorgulayın:

```bash
docker exec -i his-postgres psql -U billing_user -d billing_db -c "
SELECT table_name FROM information_schema.tables WHERE table_schema='billing_schema';
"
```

* **Beklenen Tablolar:**
  - `invoices`
  - `claims`
  - `unbilled_charges`
  - `billing_outbox_events`
  - `invoice_summaries`

---

## 4. Uçtan Uca Event-Driven Faturalama Testi

### 4.1 Test 1: Lab Siparişi Eventi Simülasyonu (`lab-order-placed.v1`)

Kafka üzerinden faturalandırılmamış bir tahlil harcaması eventi gönderin:

```bash
docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic lab-order-placed.v1 << 'EOF'
{"patientId":"a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d","orderId":"b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e","orderTotal":"450.00"}
EOF
```

`unbilled_charges` tablosuna kaydın düştüğünü doğrulayın:

```bash
docker exec -i his-postgres psql -U billing_user -d billing_db -c "
SELECT * FROM billing_schema.unbilled_charges WHERE patient_id='a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d';
"
```

---

### 4.2 Test 2: Randevu Ödeme Eventi Simülasyonu (`appointment-payment-updates.v1`)

Randevu tamamlanma ve fatura oluşturma eventini tetikleyin:

```bash
docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic appointment-payment-updates.v1 << 'EOF'
{"patientId":"a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d","doctorId":"c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f","totalAmount":800.00,"patientOwes":160.00,"insuranceOwes":640.00}
EOF
```

Fatura ve Outbox tablosunu doğrulayın:

```bash
docker exec -i his-postgres psql -U billing_user -d billing_db -c "
SELECT invoice_id, patient_id, total_amount, patient_owes, insurance_owes FROM billing_schema.invoices;
"
```

---

## 5. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `billing-service` podu `1/1 Running` ve `SecretSynced: True` olarak kümede çalışıyor.
- [ ] `/actuator/health` tüm bileşenleriyle `UP` durumunda.
- [ ] `billing_schema` altındaki 5 tablo başarıyla doğrulandı.
- [ ] Kafka eventleri (`lab-order-placed.v1`, `appointment-payment-updates.v1`) ile faturalama ve unbilled charge akışları test edildi.

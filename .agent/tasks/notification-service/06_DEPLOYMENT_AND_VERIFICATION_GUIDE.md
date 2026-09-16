# Task 06: Dağıtım ve Doğrulama Kılavuzu (Deployment & Verification Guide)

Bu kılavuz, `notification-service` mikroservisinin Kubernetes kümesine dağıtılmasından sonra yapılacak olan uçtan uca fonksiyonel ve operasyonel doğrulama adımlarını içerir.

---

## 1. Doğrulama Senaryoları

1. **Pod Durumu ve Actuator Sağlık Kontrolü:**
   * Liveness & Readiness prob kontrolü.
   * PostgreSQL (`notification_db`) bağlantı doğrulaması.
   * Redis bağlantı doğrulaması.
2. **gRPC Bağlantı Doğrulaması:**
   * `patient-management:9090` servisine gRPC üzerinden hasta iletişim bilgisi (`PatientQueryServiceGrpc.findById`) sorgulaması.
3. **Kafka Olay Tüketimi (Event Consumption) ve İdempotency Doğrulaması:**
   * `user-provisioned.v1`, `lab-result-completed.v1`, `appointment-scheduled.v1`, `patient-discharged.v1`, `inventory-low-stock.v1` olaylarının tüketilmesi.
   * `processed_events` tablosunda idempotency kaydının oluşması.
   * `notification_history` tablosunda gönderim kaydının `SENT` olarak yer alması.

---

## 2. Doğrulama Komutları

```bash
# 1. Pod Sağlığı
kubectl get pods -l app.kubernetes.io/name=notification-service

# 2. Actuator Endpoint
kubectl exec -i deploy/notification-service -- wget -qO- http://localhost:8090/actuator/health

# 3. Log İncelemesi
kubectl logs -l app.kubernetes.io/name=notification-service --tail=50

# 4. Veritabanı Kayıt Kontrolü
docker exec -i his-postgres psql -U postgres -d notification_db -c "SELECT * FROM notification_schema.notification_history LIMIT 5;"
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `notification-service` pod'u 1/1 Running durumuna geçti.
- [ ] Actuator sağlık durumu `UP` olarak doğrulandı.
- [ ] Kafka consumer'ları topic'lere bağlandı.
- [ ] Bildirim gönderim geçmişi ve idempotency mekanizması başarıyla test edildi.

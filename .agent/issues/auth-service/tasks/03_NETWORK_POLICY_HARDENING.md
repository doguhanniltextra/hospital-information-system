# Task 03: Ağ Politikası Sıkılaştırması (Granular Egress)

Bu görev, `auth-service` NetworkPolicy dosyasındaki geniş `0.0.0.0/0` egress kuralının daraltılarak sadece ihtiyaç duyulan iç ve dış servislere (CoreDNS 53, PostgreSQL 5432/5438, Kafka 9092, Vault 8200) erişim izni verilmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 5)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L54-L64)
- **Öncelik:** 🟠 P2 (Ağ İzolasyonu)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/auth-service/network-policy.yaml` Güncellemesi

```yaml
  egress:
    # 1. DNS Çözümleme (CoreDNS)
    - ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    # 2. PostgreSQL Veritabanı Erişimi (auth-db)
    - ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 5438
    # 3. Apache Kafka Broker
    - ports:
        - protocol: TCP
          port: 9092
    # 4. HashiCorp Vault Erişimi
    - ports:
        - protocol: TCP
          port: 8200
```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [ ] `network-policy.yaml` dosyasından `0.0.0.0/0` kuralı çıkarıldı ve port bazlı kurallar eklendi.
- [ ] NetworkPolicy cluster'a uygulandı (`kubectl apply -f ...`).
- [ ] `auth-service` pod'unun DB, Kafka ve Vault ile sorunsuz haberleştiği doğrulandı.

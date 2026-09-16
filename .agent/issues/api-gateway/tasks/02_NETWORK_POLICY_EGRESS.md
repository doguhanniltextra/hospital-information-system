# Task 02: NetworkPolicy Kafka Egress Portu (9092) Eklemesi

Bu görev, `api-gateway` mikroservisinin `logback-spring.xml` üzerinden `his-audit-logs` Kafka topic'ine denetim loglarını iletebilmesi için NetworkPolicy egress kurallarına TCP 9092 (Kafka) portunun eklenmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 4)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L45-L55)
- **Öncelik:** 🔴 P1 (Ağ İzolasyonu & Loglama)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/api-gateway/network-policy.yaml` Güncellemesi

Egress kurallarına Kafka broker portu eklenir:

```yaml
  egress:
    # Allow CoreDNS resolution
    - ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    # Allow Redis for rate limiting
    - ports:
        - protocol: TCP
          port: 6379
    # Allow Kafka broker for audit logging
    - ports:
        - protocol: TCP
          port: 9092
    # Allow downstream microservices
    - ports:
        - protocol: TCP
          port: 8080 # Patient Service
        - protocol: TCP
          port: 8083 # Doctor Service
        - protocol: TCP
          port: 8084 # Appointment Service
        - protocol: TCP
          port: 8085 # Support Service
        - protocol: TCP
          port: 8086 # Admission Service
        - protocol: TCP
          port: 8089 # Auth Service
```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] `network-policy.yaml` dosyasına TCP 9092 portu egress kuralı olarak eklendi.
- [x] NetworkPolicy cluster'a uygulandı (`kubectl apply -f ...`).
- [x] Gateway pod'undan Kafka'ya audit log gönderiminin engellenmediği doğrulandı.

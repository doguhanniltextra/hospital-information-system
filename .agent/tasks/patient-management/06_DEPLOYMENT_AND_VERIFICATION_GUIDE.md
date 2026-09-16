# Task 06: Dağıtım, Güvenlik, Kafka ve Uçtan Uca Doğrulama Kılavuzu

Bu kılavuz, `patient-management` mikroservisinin Kubernetes cluster'ında ayağa kaldırılması, pod güvenlik bağlamı (PSS Restricted, non-root, readOnlyRootFilesystem), Actuator sağlık/metrik denetimleri, kaynak kullanımı, CQRS Dual DB yazma/okuma ayrımı, veritabanı AES-256 PII şifreleme doğrulaması ve API Gateway $\rightarrow$ `patient-management` $\rightarrow$ Kafka $\rightarrow$ `auth-service` uçtan uca akışının doğrulanmasını kapsar.

---

## 🎯 CHECK_FILE.md Doğrulama Adımları Özeti

1. **Manifest Dağıtımı & Secret Senkronizasyonu:** `ExternalSecret` ve Vault SecretStore doğrulama.
2. **Pod Güvenlik & Runtime Sözleşmesi:** `runAsNonRoot: true`, `readOnlyRootFilesystem: true`, `seccompProfile`, `/tmp` `emptyDir` mount ve probe kontrolleri.
3. **Kaynak & QoS Sınıfı:** Gerçek kaynak tüketimlerinin `ephemeral-storage`, `memory` ve `cpu` sınırları içinde kalması.
4. **Dayanıklılık & Ağ İzolasyonu:** `PDB`, `HPA` ve `NetworkPolicy` kurallarının doğrulanması.
5. **Uçtan Uca Fonksiyonel Doğrulama:**
   - Gateway üzerinden `POST /api/patients` ile hasta kaydı.
   - PostgreSQL Write DB'de AES-256 ciphertext doğrulaması (`national_id`, `phone_number`).
   - Gateway üzerinden `GET /api/patients/{id}` ile deşifre edilmiş verinin okunması.
   - Kafka `patient-created.v1` eventinin üretilmesi ve `auth-service` tarafından consume edilerek kullanıcı hesabı açılması.

---

## 1. Dağıtım ve Pod Sağlık Kontrolleri

### 1.1 Manifestleri Cluster'a Uygulama
```bash
kubectl apply -k kubernetes/base/apps/patient-management
```

### 1.2 Secret Senkronizasyon Kontrolü
```bash
# Vault SecretStore ve ExternalSecret durumu:
kubectl get secretstore patient-management-vault-store
kubectl get externalsecret patient-management-vault-secret

# Oluşturulan Kubernetes Secret içeriği (anahtarlar):
kubectl get secret patient-management-secret -o jsonpath='{.data}'
```

### 1.3 Pod Durumları ve Rollout Kontrolü
```bash
# Rollout tamamlanma durumu:
kubectl rollout status deployment/patient-management --timeout=90s

# Pod listesi ve hazır olma durumu (2/2 replica):
kubectl get pods -l app.kubernetes.io/name=patient-management -o wide
```

### 1.4 Actuator Sağlık & Metrik Probları
```bash
# Liveness probe kontrolü:
minikube ssh "curl -s http://\$(kubectl get svc patient-management -o jsonpath='{.spec.clusterIP}'):8080/actuator/health/liveness"

# Readiness probe kontrolü:
minikube ssh "curl -s http://\$(kubectl get svc patient-management -o jsonpath='{.spec.clusterIP}'):8080/actuator/health/readiness"

# Prometheus metrik endpoint kontrolü:
minikube ssh "curl -s http://\$(kubectl get svc patient-management -o jsonpath='{.spec.clusterIP}'):8080/actuator/prometheus | head -n 20"
```

---

## 2. Pod Güvenlik (PSS Restricted) ve Kaynak Doğrulaması

### 2.1 Salt Okunur Dosya Sistemi ve `/tmp` Yazma Testi
```bash
# Pod adını alma:
POD_NAME=$(kubectl get pods -l app.kubernetes.io/name=patient-management -o jsonpath='{.items[0].metadata.name}')

# Kök dizine yazma denemesi (Read-only file system hatası vermelidir):
kubectl exec $POD_NAME -- touch /test_root.txt 2>&1 || echo "✅ Root filesystem is strictly read-only!"

# /tmp dizinine yazma denemesi (emptyDir mount sayesinde başarılı olmalıdır):
kubectl exec $POD_NAME -- touch /tmp/test_scratch.txt && echo "✅ /tmp emptyDir is writable!"
```

### 2.2 Kullanıcı Yetkisi ve Seccomp Kontrolü
```bash
# Non-root UID 1000 doğrulaması:
kubectl exec $POD_NAME -- id
```

### 2.3 Kaynak ve HPA / PDB Durumları
```bash
kubectl top pod -l app.kubernetes.io/name=patient-management
kubectl get hpa patient-management-hpa
kubectl get pdb patient-management-pdb
```

---

## 3. Uçtan Uca Fonksiyonel ve Güvenlik Doğrulaması

### 3.1 Kimlik Doğrulama Token'ı Alma (Auth Service)
```bash
TOKEN=$(minikube ssh 'curl -s -X POST http://'$(kubectl get svc api-gateway -o jsonpath="{.spec.clusterIP}")':4004/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"admin\", \"password\": \"Admin123!\"}"' | jq -r '.token // .accessToken')
```

### 3.2 API Gateway Üzerinden Hasta Kaydı (`POST /api/patients`)
```bash
CREATE_RESP=$(minikube ssh 'curl -s -X POST http://'$(kubectl get svc api-gateway -o jsonpath="{.spec.clusterIP}")':4004/api/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer '$TOKEN'" \
  -d "{
    \"name\": \"Mehmet Demir\",
    \"email\": \"mehmet.demir@hospital.com\",
    \"nationalId\": \"12345678901\",
    \"phoneNumber\": \"+905559876543\",
    \"birthDate\": \"1990-05-15\",
    \"gender\": \"MALE\"
  }"')

echo "Response: $CREATE_RESP"
PATIENT_ID=$(echo $CREATE_RESP | jq -r '.id')
```

### 3.3 PostgreSQL PII Veri Şifreleme (AES-256) Denetimi
Veritabanına bağlanıp `national_id` ve `phone_number` kolonlarının düz metin olarak değil, AES şifreli (Base64 ciphertext) saklandığı doğrulanır:
```bash
docker exec -it patient-write-db psql -U patient_user -d patient_db -c "SELECT id, name, email, national_id, phone_number FROM patient_schema.patients WHERE id = '$PATIENT_ID';"
```
*(Beklenen Çıktı: `national_id` ve `phone_number` alanları Base64 şifreli dizgi görünümündedir).*

### 3.4 CQRS Read Database Senkronizasyonu ve `GET /api/patients/{id}`
Gateway üzerinden yapılan sorguda verinin otomatik çözülerek (decrypted) orijinal haliyle döndüğü test edilir:
```bash
minikube ssh 'curl -s http://'$(kubectl get svc api-gateway -o jsonpath="{.spec.clusterIP}")':4004/api/patients/'$PATIENT_ID' -H "Authorization: Bearer '$TOKEN'"'
```

### 3.5 Apache Kafka Eventi ve `auth-service` Otomatik Hesap Açılışı
1. `patient-management` pod loglarında `patient-created.v1` eventinin basıldığı teyit edilir:
   ```bash
   kubectl logs -l app.kubernetes.io/name=patient-management --tail=50 | grep -i "patient-created"
   ```
2. `auth-service` pod loglarında eventin yakalandığı ve hastaya ait kullanıcı kaydının otomatik açıldığı doğrulanır:
   ```bash
   kubectl logs -l app.kubernetes.io/name=auth-service --tail=50 | grep -i "user-provisioned\|patient"
   ```

---

## 4. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `patient-management` pod'ları (2 replica) `1.0.0` imajıyla `1/1 Running` ve Ready durumunda.
- [ ] Pod içinde `readOnlyRootFilesystem: true`, `seccompProfile: RuntimeDefault` ve UID `1000` aktif.
- [ ] Gateway üzerinden hasta kaydı (`POST /api/patients`) 201 Created döndü.
- [ ] PostgreSQL'de `national_id` ve `phone_number` AES-256 ciphertext olarak saklanıyor.
- [ ] `GET /api/patients/{id}` isteğinde Vault `ENCRYPTION_KEY` ile çözülmüş veri dönüyor.
- [ ] Kafka `patient-created.v1` eventi `auth-service` tarafından consume edildi ve kullanıcı kaydı oluşturuldu.
- [ ] `HPA` ve `PDB` kuralları başarıyla doğrulandı.

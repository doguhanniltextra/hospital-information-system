# Task 04.2: Kubernetes Manifestleri ve Üretim Dağıtımı (Kubernetes Manifests)

Bu görev, `notification-service` mikroservisinin Kubernetes manifest dosyalarını [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine %100 uyumlu olarak (`kubernetes/base/apps/notification-service/` altında) hazırlamayı, ExternalSecrets ile Vault entegrasyonunu sağlamayı ve Minikube kümesine dağıtmayı kapsar.

---

## 1. Hazırlanacak Manifest Listesi (`kubernetes/base/apps/notification-service/`)

1. **`serviceaccount.yaml`**: `notification-service-sa` (`automountServiceAccountToken: true` Vault auth için)
2. **`secret-store.yaml`**: `notification-service-vault-backend` (Vault Kubernetes auth)
3. **`external-secret.yaml`**: `notification-service-vault-secret` (DB, Redis, JWT, Internal Token, Ops Alert Emails)
4. **`configmap.yaml`**: Ortam değişkenleri (`SERVER_PORT: "8090"`, `KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"`, `PATIENT_GRPC_ADDRESS: "static://patient-management:9090"`, `REDIS_HOST: "redis"`)
5. **`service.yaml`**: ClusterIP Service (Port 8090)
6. **`deployment.yaml`**:
   * PSS Restricted güvenlik parametreleri (`runAsNonRoot: true`, `runAsUser: 1000`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, `capabilities: drop: ["ALL"]`, `seccompProfile: RuntimeDefault`).
   * Kaynak limitleri: `requests: { cpu: "100m", memory: "256Mi" }`, `limits: { cpu: "500m", memory: "512Mi" }`.
   * `emptyDir` mount (`/tmp`, `/app/.`).
   * `hostAliases`: `his-postgres`, `redis`, `kafka` için minikube IP `192.168.49.1`.
   * Liveness & Readiness Probes (`/actuator/health/liveness`, `/actuator/health/readiness`).
7. **`hpa.yaml`**: HorizontalPodAutoscaler (Min 1, Max 3, CPU %80, Memory %80)
8. **`pdb.yaml`**: PodDisruptionBudget (`maxUnavailable: 1`)
9. **`network-policy.yaml`**: Egress (Kube-DNS, PostgreSQL 5432, Redis 6379, Kafka 9092, Patient gRPC 9090, Vault 8200) ve Ingress (Gateway / Actuator) kuralları.
10. **`kustomization.yaml`**: Tüm kaynakları toplayan Kustomize manifesti.

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: Manifest Dosyalarını Oluşturmak

`kubernetes/base/apps/notification-service/` dizini altında 10 manifest dosyasını eksiksiz oluşturun.

### 2.2 Adım 2: ExternalSecret ve Kümeye Dağıtım

```bash
kubectl apply -k kubernetes/base/apps/notification-service
kubectl rollout status deployment/notification-service --timeout=90s
```

### 2.3 Adım 3: Sağlık ve Senkronizasyon Doğrulaması

```bash
# ExternalSecret senkronizasyon durumu:
kubectl get externalsecrets notification-service-vault-secret

# Pod durumu:
kubectl get pods -l app.kubernetes.io/name=notification-service

# Actuator sağlık kontrolü:
kubectl exec -i deploy/notification-service -- wget -qO- http://localhost:8090/actuator/health
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `kubernetes/base/apps/notification-service/` altındaki tüm manifestler CHECK_FILE standartlarına uygun oluşturuldu.
- [x] `ExternalSecret` Vault sırlarını başarıyla senkronize etti (`SecretSynced: True`).
- [x] Pod `1/1 Running` durumuna ulaştı ve `/actuator/health` çıktısı `UP` verdi.
- [x] NetworkPolicy ve PSS Restricted kısıtlamaları altında sorunsuz çalıştı.

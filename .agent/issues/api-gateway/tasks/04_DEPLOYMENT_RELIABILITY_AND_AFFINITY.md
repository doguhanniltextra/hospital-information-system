# Task 04: Dağıtım Güvenilirliği (MinReadySeconds, RevisionHistory & PodAntiAffinity)

Bu görev, `api-gateway` mikroservisinin sıfır kesintiyle (zero-downtime) güncellenmesini, yeni replica'ların trafiği karşılamadan önce stabilizasyon süresi tanımasını ve replica'ların aynı Kubernetes node'una kümelenmesini önleyen anti-affinity kurallarının tanımlanmasını kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 6)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L68-L77)
- **Öncelik:** 🟠 P2 (Yüksek Erişilebilirlik & Güvenilirlik)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/api-gateway/deployment.yaml` Güncellemesi

1. **Deployment Spec:**
   ```yaml
   spec:
     replicas: 2
     minReadySeconds: 5
     revisionHistoryLimit: 5
     strategy:
       type: RollingUpdate
       rollingUpdate:
         maxSurge: 1
         maxUnavailable: 0
   ```

2. **Pod Template Spec (Affinity):**
   ```yaml
       affinity:
         podAntiAffinity:
           preferredDuringSchedulingIgnoredDuringExecution:
             - weight: 100
               podAffinityTerm:
                 labelSelector:
                   matchExpressions:
                     - key: app.kubernetes.io/name
                       operator: In
                       values:
                         - api-gateway
                 topologyKey: "kubernetes.io/hostname"
   ```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] `deployment.yaml` dosyasına `minReadySeconds: 5` ve `revisionHistoryLimit: 5` eklendi.
- [x] Pod spec içine `podAntiAffinity` kuralı eklendi.
- [x] Rolling update sırasında yeni pod'ların 5 saniye beklemeden sonra trafiği devraldığı doğrulandı.

# Task 05: İmaj Versiyonlama ve Standart Kubernetes Etiketleri

Bu görev, `auth-service` dağıtımında `:latest` etiketi yerine sabit/semantik versiyon etiketlerinin (`1.0.0`) kullanılması ve `kustomization.yaml` içerisindeki standart `app.kubernetes.io/*` etiketlerinin tamamlanmasını kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 1 ve 8)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L12-L20) ve [Bulgu 8](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L88-L96)
- **Öncelik:** 🔴 P1 (Sürüm Kontrolü) / 🟡 P3 (Metadeta)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/auth-service/kustomization.yaml` Güncellemesi

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

labels:
  - pairs:
      app.kubernetes.io/name: auth-service
      app.kubernetes.io/part-of: hospital-information-system
      app.kubernetes.io/component: identity-provider
      app.kubernetes.io/version: "1.0.0"
      app.kubernetes.io/managed-by: kustomize

images:
  - name: doguhannilt/auth-service
    newTag: "1.0.0"

resources:
  - serviceaccount.yaml
  - configmap.yaml
  - external-secret.yaml
  - service.yaml
  - deployment.yaml
  - hpa.yaml
  - pdb.yaml
  - network-policy.yaml
```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] `kustomization.yaml` içine `app.kubernetes.io/version: "1.0.0"` ve `app.kubernetes.io/managed-by: kustomize` etiketleri eklendi.
- [x] `images` bloğu ile deterministik imaj versiyonlama (`1.0.0`) tanımlandı.
- [x] `kubectl kustomize` çıktısında ve canlı Deployment üzerinde tüm kaynakların bu etiketleri ve imaj tag'ini doğru aldığı doğrulandı.

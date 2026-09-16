# Task 05: İmaj Versiyonlama ve Standart Kubernetes Etiketleri

Bu görev, `api-gateway` dağıtımında `:latest` etiketi yerine sabit/semantik versiyon etiketlerinin kullanılması ve `kustomization.yaml` içerisindeki standart `app.kubernetes.io/*` etiketlerinin tamamlanmasını kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 1 ve 7)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L12-L20) ve [Bulgu 7](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L79-L87)
- **Öncelik:** 🔴 P1 (Sürüm Kontrolü) / 🟡 P3 (Metadeta & Gözlemlenebilirlik)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/api-gateway/kustomization.yaml` Güncellemesi

Standart K8s etiketleri ve imaj transformer yapılandırması:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

labels:
  - pairs:
      app.kubernetes.io/name: api-gateway
      app.kubernetes.io/part-of: hospital-information-system
      app.kubernetes.io/component: edge-gateway
      app.kubernetes.io/version: "1.0.0"
      app.kubernetes.io/managed-by: kustomize

images:
  - name: doguhannilt/api-gateway
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
- [x] `kubectl kustomize` çıktısında ve canlı Deployment'ta tüm kaynakların bu etiketleri ve `1.0.0` imaj tag'ini doğru aldığı doğrulandı.

# Task 01: Konteyner Güvenlik Sıkılaştırması (Read-Only FS, EmptyDir & Seccomp)

Bu görev, `api-gateway` pod'unun Kubernetes Restricted Pod Security Standard (PSS) ile tam uyumlu hale getirilmesi için dosya sisteminin salt okunur (`readOnlyRootFilesystem: true`) yapılması, gerekli geçici dizinlerin (`/tmp`) `emptyDir` ile bağlanması ve varsayılan seccomp profilinin eklenmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 2 ve 3)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L21-L35)
- **Öncelik:** 🔴 P1 (Kritik Güvenlik)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `kubernetes/base/apps/api-gateway/deployment.yaml` Güncellemesi

1. **Pod SecurityContext:**
   ```yaml
   securityContext:
     runAsNonRoot: true
     runAsUser: 1000
     runAsGroup: 1000
     fsGroup: 1000
     seccompProfile:
       type: RuntimeDefault
   ```

2. **Container SecurityContext & VolumeMount:**
   ```yaml
   containers:
     - name: api-gateway
       securityContext:
         allowPrivilegeEscalation: false
         readOnlyRootFilesystem: true
         capabilities:
           drop:
             - ALL
       volumeMounts:
         - name: tmp-volume
           mountPath: /tmp
   ```

3. **Volumes Tanımı:**
   ```yaml
   volumes:
     - name: tmp-volume
       emptyDir:
         sizeLimit: 100Mi
   ```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] Pod `securityContext` içine `seccompProfile.type: RuntimeDefault` eklendi.
- [x] Container `securityContext` içinde `readOnlyRootFilesystem: true` yapıldı.
- [x] `/tmp` dizini için `emptyDir` (sizeLimit: 100Mi) volume ve volumeMount eklendi.
- [x] Pod'un salt okunur dosya sistemiyle başarıyla ayağa kalktığı ve log/actuator endpoint'lerinin çalıştığı doğrulandı.

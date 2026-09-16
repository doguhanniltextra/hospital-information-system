# Task 04: Kaynak Yönetimi ve Ephemeral Storage Sınırları

Bu görev, `auth-service` pod'unun çalışma anındaki gerçek disk ve bellek kullanımının ölçülerek `ephemeral-storage`, CPU ve bellek sınırlarının optimize edilmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 6)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L66-L75)
- **Öncelik:** 🟠 P2 (Kaynak Yönetimi)

---

## 📊 Yapılan Tüketim Testleri ve Ölçüm Verileri

1. **Pod İçi Ephemeral Disk Ölçümü (`du -sh`):**
   - `/app` (Uygulama JAR): `88 MB`
   - `/tmp` (`emptyDir` geçici buffer'lar): `28 KB`
   - `/var/log`: `8.0 KB`
   - **Toplam:** `~89 MB` $\rightarrow$ `requests.ephemeral-storage: 100Mi`, `limits: 500Mi` olarak belirlendi.

2. **Cgroups Gerçek Bellek Ölçümü (`cgroup memory.current`):**
   - Aktif Tüketim: `340,791,296 bytes` (`~325 MB`)
   - **Karar:** `requests.memory: 384Mi` ve `limits.memory: 768Mi` (G1GC %75 MaxRAM ile 576MB heap) olarak yapılandırıldı.

---

## 🛠️ Yapılan Değişiklikler

### 1. `kubernetes/base/apps/auth-service/deployment.yaml` Güncellemesi

```yaml
          resources:
            requests:
              cpu: 250m
              memory: 384Mi
              ephemeral-storage: 100Mi
            limits:
              cpu: 1000m
              memory: 768Mi
              ephemeral-storage: 500Mi
```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] Pod içinde `du -sh` ve cgroups memory ölçümleri gerçekleştirildi.
- [x] `deployment.yaml` içinde container `requests.ephemeral-storage: 100Mi` eklendi.
- [x] `deployment.yaml` içinde container `limits.ephemeral-storage: 500Mi` eklendi.
- [x] Manifest uygulandı ve pod'un kaynak sınırlarıyla sorunsuz çalıştığı doğrulandı.

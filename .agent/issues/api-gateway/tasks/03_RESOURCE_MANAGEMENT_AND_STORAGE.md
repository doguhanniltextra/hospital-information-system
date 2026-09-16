# Task 03: Kaynak Yönetimi ve Ephemeral Storage Sınırları

Bu görev, `api-gateway` pod'unun çalıştırıldığı Kubernetes worker node'larının disk taşmalarını ve `DiskPressure` durumlarını engellemek amacıyla deneysel ölçümlere dayalı bellek, CPU ve `ephemeral-storage` istek ve limitlerinin tanımlanmasını kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 5)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L57-L66)
- **Öncelik:** 🟠 P2 (Kaynak Yönetimi)

---

## 📊 Yapılan Tüketim Testleri ve Ölçüm Verileri

1. **Pod İçi Ephemeral Disk Ölçümü (`du -sh`):**
   - `/app` (Uygulama JAR): `74 MB`
   - `/tmp` (`emptyDir` geçici dosyalar / buffer): `4.0 KB`
   - `/var/log`: `8.0 KB`
   - **Toplam:** `~75 MB` $\rightarrow$ `requests.ephemeral-storage: 100Mi`, `limits: 500Mi` olarak belirlendi.

2. **Cgroups Gerçek Bellek Ölçümü (`cgroup memory.current`):**
   - Aktif Tüketim: `~309 MB` (Spring WebFlux + Netty + Redis Client + Logback Kafka Appender)
   - **Karar:** Eski `requests.memory: 256Mi` dar kaldığından `requests.memory: 384Mi` ve `limits.memory: 768Mi` olarak güncellendi.

---

## 🛠️ Yapılan Değişiklikler

### 1. `kubernetes/base/apps/api-gateway/deployment.yaml` Güncellemesi

Container `resources` bloğu güncellendi:

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

- [x] Pod içinde `du -sh` ve cgroups memory ölçümleri yapıldı.
- [x] `deployment.yaml` içinde `requests.ephemeral-storage: 100Mi` ve `requests.memory: 384Mi` eklendi.
- [x] `deployment.yaml` içinde `limits.ephemeral-storage: 500Mi` ve `limits.memory: 768Mi` eklendi.
- [x] Manifest uygulandı ve pod'ların yeni kaynak sınırlarıyla 1/1 Running ve Ready durumuna ulaştığı doğrulandı.

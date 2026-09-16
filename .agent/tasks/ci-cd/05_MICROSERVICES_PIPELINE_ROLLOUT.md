# 🚀 Görev 05: 9 Mikroservis İçin CI/CD İş Akışlarının Entegrasyonu

Bu görev; hazırlanan Reusable Pipeline altyapısını Hospital Information System (HIS) projesindeki **9 mikroservisin tamamına** uygulamak için gerekli `.github/workflows/<service>.yml` dosyalarının oluşturulmasını ve monorepo `paths` filtrelerinin tanımlanmasını içerir.

---

## 🎯 Kapsamdaki 9 Mikroservis Listesi

| Sıra | Mikroservis Adı | Servis Dizini (`service_path`) | Docker Image (`image_name`) | Workflow Dosya Yolu |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `api-gateway` | `api-gateway` | `doguhannilt/api-gateway` | `.github/workflows/api-gateway.yml` |
| **2** | `auth-service` | `auth-service` | `doguhannilt/auth-service` | `.github/workflows/auth-service.yml` |
| **3** | `patient-management` | `patient-management` | `doguhannilt/patient-management` | `.github/workflows/patient-management.yml` |
| **4** | `doctor-service` | `doctor-service` | `doguhannilt/doctor-service` | `.github/workflows/doctor-service.yml` |
| **5** | `appointment-service` | `appointment-service` | `doguhannilt/appointment-service` | `.github/workflows/appointment-service.yml` |
| **6** | `admission-service` | `admission-service` | `doguhannilt/admission-service` | `.github/workflows/admission-service.yml` |
| **7** | `support-service` | `support-service` | `doguhannilt/support-service` | `.github/workflows/support-service.yml` |
| **8** | `billing-service` | `billing-service` | `doguhannilt/billing-service` | `.github/workflows/billing-service.yml` |
| **9** | `notification-service` | `notification-service` | `doguhannilt/notification-service` | `.github/workflows/notification-service.yml` |

---

## 📝 Örnek Caller Workflow Şablonu

Her bir mikroservis için `.github/workflows/<service-name>.yml` formatında aşağıdaki yapı oluşturulur:

```yaml
name: Doctor Service - DevSecOps CI/CD

on:
  push:
    branches: [ main, master ]
    paths:
      - 'doctor-service/**'
      - 'kubernetes/base/apps/doctor-service/**'
  pull_request:
    branches: [ main, master ]
    paths:
      - 'doctor-service/**'
      - 'kubernetes/base/apps/doctor-service/**'
  workflow_dispatch:

concurrency:
  group: doctor-service-${{ github.ref }}
  cancel-in-progress: true

jobs:
  pipeline:
    uses: ./.github/workflows/reusable-service-ci.yml
    with:
      service_name: "doctor-service"
      service_path: "doctor-service"
      image_name: "doguhannilt/doctor-service"
      k8s_overlay_path: "kubernetes/overlays/prod"
    secrets:
      DOCKERHUB_USERNAME: ${{ secrets.DOCKERHUB_USERNAME }}
      DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
```

---

## 🔑 Gerekli GitHub Secrets

Repository düzeyinde aşağıdaki secret'ların tanımlanmış olması gerekir:
- `DOCKERHUB_USERNAME` (Docker Hub kullanıcı adı)
- `DOCKERHUB_TOKEN` (Docker Hub access token)

---

## ✅ Görev Tamamlanma Kriterleri

- [ ] 9 mikroservisin her biri için `.github/workflows/` altında tetikleyici workflow dosyaları oluşturuldu.
- [ ] Monorepo `paths` filtreleri doğru yapılandırıldı (her servis yalnızca kendi dizini değiştiğinde tetikleniyor).
- [ ] GitHub Secrets (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`) tanımlandı.
- [ ] Bir serviste commit atılarak Self-Hosted Runner üzerinde uçtan uca pipeline'ın başarıyla çalıştığı doğrulandı.

# Task 05: CI/CD Pipeline ve GitOps İş Akışları (CI/CD Workflows)

Bu görev, `notification-service` mikroservisi için GitHub Actions üzerinde çalışacak olan modüler CI/CD iş akışlarını (`.github/workflows/notification-service/` altında) yapılandırmayı kapsar.

---

## 1. Hazırlanacak İş Akışları (Workflows)

1. **`01_pull_request_validation.yml`**: Maven derleme, birim testler, kod formatı kontrolü.
2. **`02_build_and_publish.yml`**: Multi-stage Docker imaj derlemesi, versiyon etiketleme ve GitHub Container Registry (GHCR) veya Docker Hub'a push.
3. **`03_code_coverage.yml`**: JaCoCo test kapsamı analizi ve raporlama.
4. **`04_security_scan.yml`**: Trivy konteyner CVE güvenlik taraması ve Kubeconform Kubernetes schema doğrulaması.
5. **`05_gitops_promote.yml`**: Başarılı derlemede `kubernetes/base/apps/notification-service/deployment.yaml` üzerindeki imaj etiketini güncelleyen GitOps adımı.

---

## 2. Görev Tamamlanma Kriterleri (Checklist)

- [ ] `.github/workflows/notification-service/` altında 5 adet modüler GitHub Actions iş akışı tanımlandı.
- [ ] Kubeconform, Trivy ve JaCoCo adımları doğrulandı.

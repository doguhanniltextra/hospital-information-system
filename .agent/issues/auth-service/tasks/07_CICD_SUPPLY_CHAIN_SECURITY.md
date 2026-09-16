# Task 07: CI/CD Tedarik Zinciri ve Trivy İmaj Güvenlik Taraması

Bu görev, `auth-service` Docker imajının GitHub Actions CI pipeline'ında her derleme/push sonrasında Trivy aracı ile güvenlik açıklarına (CVE) karşı otomatik taranmasını ve `CRITICAL` seviyesindeki zafiyetlerde dağıtımın engellenmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 9)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L98-L106)
- **Öncelik:** 🟠 P2 (Tedarik Zinciri Güvenliği)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `.github/workflows/auth-service/04_security_scan.yml` İş Akışı Tanımı

```yaml
name: Auth Service Security Scan

on:
  push:
    paths:
      - 'auth-service/**'
  workflow_dispatch:

jobs:
  trivy-scan:
    name: Trivy Container Vulnerability Scan
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Build Local Image
        run: |
          docker build -t doguhannilt/auth-service:scan auth-service/

      - name: Run Trivy Vulnerability Scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'doguhannilt/auth-service:scan'
          format: 'table'
          exit-code: '1'
          ignore-unfixed: true
          vuln-type: 'os,library'
          severity: 'CRITICAL,HIGH'
```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [ ] GitHub Actions Trivy güvenlik taraması iş akışı tanımlandı.
- [ ] `CRITICAL` seviyesindeki güvenlik açıklarında pipeline'ın hata vererek dağıtımı durdurduğu kuralı belirlendi.

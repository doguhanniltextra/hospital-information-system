# Task 06: CI/CD Tedarik Zinciri ve Trivy İmaj Güvenlik Taraması

Bu görev, `api-gateway` Docker imajının GitHub Actions CI pipeline'ında her derleme/push sonrasında Trivy aracı ile güvenlik açıklarına (CVE) karşı otomatik taranmasını ve `CRITICAL` seviyesindeki zafiyetlerde dağıtımın engellenmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 8)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/api-gateway/logs.md#L89-L97)
- **Öncelik:** 🟠 P2 (Tedarik Zinciri Güvenliği)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `.github/workflows/api-gateway/04_security_scan.yml` İş Akışı Tanımı

```yaml
name: API Gateway Security Scan

on:
  push:
    paths:
      - 'api-gateway/**'
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
          docker build -t doguhannilt/api-gateway:scan api-gateway/

      - name: Run Trivy Vulnerability Scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'doguhannilt/api-gateway:scan'
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

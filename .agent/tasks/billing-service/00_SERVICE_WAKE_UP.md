# Task 00: Servis Uyandırma, Test ve İmaj Hazırlığı (Service Wake-Up)

Bu görev, `billing-service` mikroservisinin Kubernetes ve Vault entegrasyonuna başlamadan önce kod kalitesini, birim (unit) testlerinin çalışma durumunu denetlemeyi ve Docker imajının varlığını kontrol edip gerekirse derleyerek etiketleyip (tag) yüklemeyi kapsar.

---

## 1. Görev Kapsamı ve Hedefler

1. **Birim Testlerinin (Unit Tests) Çalıştırılması ve Doğrulanması:**
   * `billing-service` altındaki Komut Servisleri (`BillingCommandServiceTest`), Sigorta Hesaplama Stratejileri (`InsuranceFactoryTest`, `InsuranceStrategiesTest`) testlerini bağımsız olarak çalıştırmak.
   * Tüm testlerin yeşil (PASSED) olduğunu ve sıfır hata/kırılma olduğunu teyit etmek.
2. **Docker İmaj Varlığının Kontrol Edilmesi:**
   * Yerel ortamda `doguhannilt/billing-service:1.0.0` imajının varlığını sorgulamak.
3. **İmaj Derleme, Etiketleme ve Yükleme (Build, Tag & Minikube Load):**
   * Multi-stage Dockerfile üzerinden imajı derlemek, `doguhannilt/billing-service:1.0.0` olarak etiketlemek ve Minikube cluster içine yüklemek.

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: Birim Testlerini Koşmak
`billing-service` dizini içerisinde Maven ile testleri çalıştırın:

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system/billing-service
mvn clean test -B
```

* **Beklenen Çıktı:**
  - `Tests run: X, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

### 2.2 Adım 2: Docker İmaj Durumunu Kontrol Etmek
Mevcut imajları sorgulayın:

```bash
docker images | grep billing-service || echo "İmaj bulunamadı, derleme yapılacak."
```

### 2.3 Adım 3: Docker İmajını Derlemek, Etiketlemek ve Minikube'e Yüklemek

```bash
# Proje kök dizininden derleme:
cd /home/doguhan/SoftwareProjects/hospital-information-system
docker build -t doguhannilt/billing-service:1.0.0 ./billing-service

# Minikube cluster'a yükleme:
minikube image load doguhannilt/billing-service:1.0.0
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `billing-service` birim testleri (`mvn clean test`) çalıştırıldı ve tüm testlerin `BUILD SUCCESS` (9 test, 0 failure, 0 error) ile geçtiği doğrulandı.
- [x] Docker imajı (`doguhannilt/billing-service:1.0.0`) multi-stage Dockerfile ile başarıyla derlendi.
- [x] İmaj Minikube ortamına (`minikube image load doguhannilt/billing-service:1.0.0`) başarıyla aktarıldı.

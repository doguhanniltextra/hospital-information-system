# Task 00: Servis Uyandırma, Test ve İmaj Hazırlığı (Service Wake-Up)

Bu görev, `patient-management` mikroservisinin Kubernetes ve Vault entegrasyonuna başlamadan önce kod kalitesini, birim (unit) testlerinin çalışma durumunu denetlemeyi ve Docker imajının varlığını kontrol edip gerekirse derleyerek etiketleyip (tag) yayınlamayı (push) kapsar.

---

## 1. Görev Kapsamı ve Hedefler

1. **Birim Testlerinin (Unit Tests) Çalıştırılması ve Doğrulanması:**
   * `patient-management` modülü altındaki Command, Query ve Helper katmanlarının testlerini bağımsız olarak çalıştırmak.
   * Tüm testlerin yeşil (PASSED) olduğunu ve sıfır hata/kırılma olduğunu teyit etmek.
2. **Docker İmaj Varlığının Kontrol Edilmesi:**
   * Yerel ortamda `doguhannilt/patient-management:latest` imajının varlığını sorgulamak.
3. **İmaj Derleme, Etiketleme ve Yayınlama (Build, Tag & Push):**
   * İmaj yoksa veya kodda değişiklik yapılmışsa multi-stage Dockerfile üzerinden yeni imajı derlemek, `doguhannilt/patient-management:latest` olarak etiketlemek ve kayıt defterine (registry) göndermek.

---

## 2. Adım Adım Uygulama Prosedürü

### 2.1 Adım 1: Birim Testlerini Koşmak
`patient-management` dizini içerisindeki Maven Wrapper ile testleri çalıştırın:

```bash
cd /home/doguhan/SoftwareProjects/hospital-information-system/patient-management
./mvnw clean test -B
```

* **Beklenen Çıktı:**
  - `Tests run: X, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

### 2.2 Adım 2: Docker İmaj Durumunu Kontrol Etmek
Mevcut imajları sorgulayın:

```bash
docker images | grep patient-management || echo "İmaj bulunamadı, derleme yapılacak."
```

### 2.3 Adım 3: Docker İmajını Derlemek, Etiketlemek ve Pushlamak
Eğer imaj yoksa veya güncel değilse:

```bash
# Proje kök dizininden derleme:
cd /home/doguhan/SoftwareProjects/hospital-information-system
docker build -t doguhannilt/patient-management:latest ./patient-management

# Gerekirse versiyon etiketi ekleme:
docker tag doguhannilt/patient-management:latest doguhannilt/patient-management:1.0.0

# Docker Hub / Registry'ye pushlama (Giriş yapılmışsa):
docker push doguhannilt/patient-management:latest || echo "Lokal Minikube kullanımı için hazırlandı."
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `patient-management` birim testleri (`./mvnw clean test`) çalıştırıldı ve tüm testlerin `BUILD SUCCESS` (10 test, 0 failure) ile geçtiği doğrulandı.
- [x] `docker images` çıktısında `doguhannilt/patient-management:latest` imajının mevcut olduğu doğrulandı.
- [x] İmaj derleme (`docker build`) tamamlandı ve Minikube ortamına (`minikube image load`) başarıyla yüklendi.

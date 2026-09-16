# Kubernetes Mimari ve Dağıtım Planı (00_KUBERNETES_PLAN.md)

Bu doküman, Hospital Information System (HIS) mikroservis ekosisteminin Kubernetes ortamına taşınma vizyonunu, mimari kararlarını, Kustomize stratejisini ve uçtan uca trafik akışını detaylandırmaktadır.

---

## 1. Vizyon ve Hedef: Biz Nasıl Bir Şey Hayal Ediyoruz?

Geliştirmekte olduğumuz sistem; 9 bağımsız mikroservis, olay güdümlü (event-driven) mesajlaşma mimarisi (Kafka), yüksek performanslı sorgu katmanı (gRPC) ve çoklu veritabanı (Postgres, Redis, MongoDB) yapısına sahip dağıtık bir sağlık bilgi sistemidir.

### Temel Hedefler:
1. **Sıfır Kesinti (Zero-Downtime Deployment)**: RollingUpdate stratejileri, `startupProbe`/`readinessProbe` ve `preStop` yaşam döngüsü kancaları (lifecycle hooks) ile yeni bir sürüm çıktığında tek bir kullanıcının bile bağlantısının kopmaması.
2. **Ortam Ayrımı ve Taşınabilirlik (Dev/Prod Parity)**: Yazılımcının Minikube üzerinde çalıştırdığı manifestler ile AWS EKS prodüksiyon ortamında çalışan manifestlerin %95 oranında aynı olması (Twelve-Factor App ilkeleri).
3. **Sıfır Güven (Zero-Trust) Ağ Güvenliği**: Dış dünyadan gelen trafiğin yalnızca `api-gateway` üzerinden içeri girmesi; mikroservislerin izole iç ağda (private subnet) çalışması ve NetworkPolicy kurallarıyla doğrudan veritabanı/kuyruk erişimlerinin kısıtlanması.
4. **Pragmatik Hibrit Veri Modeli**: Ağır ve veri kaybı riski yüksek stateful bileşenlerin (Postgres, Kafka, Redis) prodüksiyon ortamında Kubernetes pod'larında taşınmayıp yönetilen bulut servislerine (AWS RDS, ElastiCache, MSK) devredilmesi; yerelde ise Docker Compose ile hafif şekilde ayağa kaldırılması.

---

## 2. Kustomize Felsefesi: Bize Ne Sağlıyor ve Neden Kustomize?

Kubernetes dünyasında iki ana paketleme yaklaşımı bulunur: **Helm** ve **Kustomize**. Bu projede bilinçli olarak **Kustomize** seçilmiştir.

### Kustomize'ın Avantajları:
1. **Şablon Karmaşasının (Template Hell) Önlenmesi**:
   Helm gibi araçlar YAML dosyalarının içerisine karmaşık `{{ if .Values.enabled }}` blokları koyar ve YAML'ın okunabilirliğini bozar. Kustomize ise **saf, standart YAML** kullanır.
2. **Base ve Overlays Ayrımı (DRY Prensibi)**:
   * **`base/` (Temel Katman)**: Bir mikroservisin doğası gereği ihtiyaç duyduğu değişmez kuralları barındırır (port 4004, sağlık kontrol yolları, etiketleme standartları, kaynak sınırları).
   * **`overlays/` (Ortam Katmanı)**: Sadece o ortama özgü farkları (yama - patch) içerir:
     * `overlays/local/`: 1 replika, `host.minikube.internal` veritabanı adresleri, düşük CPU/RAM limitleri.
     * `overlays/prod/`: 3+ replika, yüksek erişilebilirlik (HA), AWS RDS / MSK endpoint'leri, AWS ECR imaj yolları.
3. **Yerel kubectl Desteği**:
   Ekstra hiçbir CLI aracına ihtiyaç duymadan doğrudan `kubectl apply -k` komutuyla çalışır.
4. **Modern GitOps Uyumluluğu**:
   ArgoCD ve FluxCD gibi kurumsal GitOps araçları Kustomize dizin yapısını yerel olarak destekler.

---

## 3. Uçtan Uca Trafik ve Güvenlik Mimarisi (Edge-to-Core)

Bir kullanıcının mobil cihazından veya tarayıcısından gelen bir isteğin pod'a ulaşma serüveni:

```text
[ 1. Kullanıcı ]
       │
       ▼
[ 2. DNS / Route 53 ] (api.hastanem.com -> Public IP Çözümleme)
       │
       ▼
[ 3. CloudFront & AWS Shield ] (DDoS Koruma Katmanı, Saldırıları Sınırda Engeller)
       │
       ▼
[ 4. AWS WAF ] (Web Application Firewall: SQLi, XSS filtreleme)
       │
       ▼
[ 5. AWS Application Load Balancer (ALB) ] (Public Subnet, SSL/HTTPS Sonlandırma)
       │
       ▼ (Private Subnet - EKS Düğümleri)
[ 6. Ingress Controller (Nginx / AWS Load Balancer Controller) ]
       │
       ▼
[ 7. api-gateway Pod (Port: 4004) ]
       │  - JWT İmza ve Süre Doğrulama (SecurityConfig)
       │  - Redis Tabanlı IP/Kullanıcı Hız Sınırı (Rate Limiting)
       │  - Rota Belirleme (/api/patients/** -> patient-management)
       │
       ├──(gRPC HTTP/2 - Port: 9090)──> [ patient-management ]
       ├──(REST HTTP/1.1 - Port: 8083)──> [ doctor-service ]
       └──(REST HTTP/1.1 - Port: 8084)──> [ appointment-service ]
                                                   │
                                                   ▼
                                          [ AWS RDS Postgres ]
```

### Güvenlik Sınırları:
* **Dışarıya Açık Servisler**: Yalnızca `api-gateway` Ingress üzerinden erişilebilirdir.
* **İç Servisler**: `patient-management`, `doctor-service` vb. servislerin `ClusterIP` dışında dış dünyaya hiçbir açık portu yoktur.
* **NetworkPolicy İzolasyonu**: `api-gateway` yalnızca downstream servislere, Redis'e ve CoreDNS'e bağlanabilir. Doğrudan veritabanına erişimi ağ düzeyinde engellenmiştir.

---

## 4. Veri ve Stateful Katman Stratejisi

### Neden Veritabanlarını Kubernetes Pod'unda Çalıştırmıyoruz?
1. **Depolama Gecikmesi (CSI Disk Latency)**: Bulut blok depolama üniteleri (EBS vb.) ağ üzerinden bağlandığı için yüksek I/O gerektiren veritabanlarında performans kaybına yol açabilir.
2. **Failover (Düğüm Çökmesi) Süresi**: Bir Kubernetes düğümü çöktüğünde diskin çözülüp başka bir düğüme bağlanması 5-6 dakikayı bulabilir. AWS Aurora/RDS ise saniyeler içinde multi-AZ yedek sunucuya geçer.
3. **Day-2 Operasyonel Yük**: Otomatik nokta-zamanlı yedekleme (PITR), replikasyon ve sürüm yükseltme işlemlerini pod içinde yönetmek yüksek risk ve mühendislik eforu gerektirir.

### Çözümümüz (Pragmatik Model):
* **Lokal Minikube**: `infrastructure/docker-compose.yml` kullanılarak Postgres, Redis, Kafka ve Mongo host makinede çalıştırılır. Minikube pod'ları bu servislere `host.minikube.internal` üzerinden bağlanır.
* **Production AWS**: AWS Aurora PostgreSQL, AWS ElastiCache Redis, AWS MSK Kafka kullanılır.

---

## 5. Dizin Yapısı ve Standartlaştırma

Tüm mikroservisler için standartlaştırılmış dizin modeli:

```text
kubernetes/
├── platform/                               # [LAYER 0] Küme genelinde geçerli kaynaklar
│   ├── rbac/                               # ServiceAccount ve Rol tanımları
│   └── network-policies/                   # Varsayılan kapatma (default-deny) kuralları
│
├── base/                                   # [LAYER 1-3] Ortamdan bağımsız ana iskelet
│   ├── apps/                               # 9 Bağımsız mikroservis
│   │   ├── api-gateway/                    # (Örnek standart paket)
│   │   │   ├── kustomization.yaml          # Manifest birleştirici
│   │   │   ├── deployment.yaml             # Pod şablonu, probelar, kaynaklar
│   │   │   ├── service.yaml                # ClusterIP servis
│   │   │   ├── configmap.yaml              # Çevre değişkenleri ve servis URL'leri
│   │   │   ├── secret.yaml                 # Lokal test anahtarları (.gitignore'da)
│   │   │   ├── secret.template.yaml        # Secret şablonu
│   │   │   ├── hpa.yaml                    # Yatay pod ölçekleyici (CPU/RAM)
│   │   │   ├── pdb.yaml                    # Pod kesinti bütçesi (minAvailable: 1)
│   │   │   ├── serviceaccount.yaml         # K8s API erişimi kapalı SA
│   │   │   ├── network-policy.yaml         # Ağ kısıtlama kuralları
│   │   │   └── hack.md                     # Test ve doğrulama curl komutları
│   │   │
│   │   ├── patient-management/             # REST (8080) + gRPC (9090)
│   │   ├── auth-service/
│   │   ├── doctor-service/
│   │   ├── appointment-service/
│   │   ├── admission-service/
│   │   ├── billing-service/
│   │   ├── notification-service/
│   │   └── support-service/
│   │
│   ├── ingress/
│   │   ├── kustomization.yaml
│   │   └── ingress.yaml                    # Ingress yönlendirme kuralları (his.local)
│   │
│   └── observability/                      # Prometheus, Grafana, Logstash
│
└── overlays/                               # [LAYER 4] Ortam yamaları (patches)
    ├── local/                              # Minikube ayarları (host.minikube.internal)
    ├── staging/
    └── prod/                               # AWS EKS ayarları (RDS, ECR imajları, 3 replika)
```

---

## 6. Mevcut Durum ve Yol Haritası

### Tamamlanan Aşamalar:
1. `api-gateway` kaynak kodu Kubernetes gereksinimlerine göre uyarlandı (graceful shutdown, actuator probe uç noktaları, Ingress uyumlu X-Forwarded-For IP çözümleme, HTTP istemci zaman aşımları).
2. `api-gateway` Dockerfile imaj katmanları önbellek optimizasyonu ile yapılandırıldı.
3. İmaj build edildi ve Docker Hub'a yüklendi (`docker.io/doguhannilt/api-gateway:latest`).
4. `kubernetes/base/apps/api-gateway/` altındaki 8 temel manifest oluşturuldu ve dry-run doğrulaması tamamlandı.
5. `minikube` üzerinde ingress addon aktif edildi ve Ingress üzerinden `api-gateway` yönlendirmesi başarıyla test edildi (`HTTP 200 UP`).
6. Test ve operasyonel doğrulama rehberi (`hack.md`) hazırlandı.

### Sıradaki Adımlar:
1. **İkinci Servis**: `auth-service` ve `patient-management` servislerinin K8s manifestlerinin hazırlanması.
2. **gRPC Desteği**: `patient-management` servisi için çift portlu (`8080 REST` ve `9090 gRPC`) Kubernetes Service tanımı.
3. **Overlays Katmanı**: `overlays/local` altında tüm mikroservisleri tek komutla (`kubectl apply -k kubernetes/overlays/local`) Minikube'e basacak orkestrasyonun tamamlanması.

# Kapsamlı Rehber: Ingress, Ingress Controller ve Nginx Mimarisi (01_INGRESS_CONTROLLER_AND_NGINX.md)

Bu doküman; Kubernetes ağ dünyasının en temel ve en sık karıştırılan bileşenleri olan **Ingress**, **Ingress Controller** ve **Nginx** kavramlarını derinlemesine, tarihsel evrimi ve kurumsal mimarilerdeki rolüyle bir kitap niteliğinde ele almaktadır.

---

## BÖLÜM 1: Temel Problem ve Tarihsel Evrim (Neden Ingress Var?)

Kubernetes dünyasına ilk adım atan geliştiricilerin karşılaştığı ilk şok şudur:
*"Uygulamamı bir pod içine koydum, ayağa kalktı ama tarayıcımdan `http://localhost:8080` yazınca neden ulaşamıyorum?"*

Bu sorunun cevabı, Kubernetes'in temel tasarım felsefesinde gizlidir.

### 1.1 Pod'ların Doğası: Geçici (Ephemeral) ve İzole Yaşam
Kubernetes'te pod'lar ölümlüdür. Yeniden başlatıldıklarında veya başka bir sunucuya taşındıklarında IP adresleri değişir. Ayrıca bu IP adresleri cluster'ın içindeki sanal ağa (`overlay network`) aittir. Dış dünyadaki fiziksel bilgisayarlar bu iç IP adreslerine doğrudan erişemez.

Bu izolasyonu kırmak için zamanla üç farklı servis tipi geliştirilmiştir:

#### Aşama 1: ClusterIP (Sadece İç Ağ)
* Yalnızca küme içindeki pod'ların birbirini bulması için sabit bir sanal IP sağlar.
* **Sorun**: Dış dünyadan tamamen yalıtılmıştır.

#### Aşama 2: NodePort (İlk Dışa Açılma Girişimi)
* Cluster'daki her bir fiziksel sunucu (Node) üzerinde `30000-32767` aralığında rastgele bir port açılır.
* İstek `http://sunucu-ip:31893` adresine geldiğinde hedef pod'a iletilir.
* **Neden Yetmedi?**:
  1. Port aralığı kısıtlıdır ve standart dışıdır (Kimse `https://hastanem.com:31893` yazmak istemez, standart `80` ve `443` istenir).
  2. Sunucuların IP adresleri değiştiğinde istemcilerin adresi de bozulur.
  3. URL veya domain adına göre akıllı yönlendirme (Layer 7 routing) yapamaz.

#### Aşama 3: LoadBalancer (Maliyet Krizi)
* Bulut sağlayıcısına (AWS, GCP) emir verilerek harici bir yük dengeleyici açılır.
* **Neden Yetmedi? (Maliyet Çıkmazı)**:
  Eğer 20 mikroservisiniz varsa ve her birine `type: LoadBalancer` derseniz, AWS hesabınızda **20 ayrı fiziksel Application Load Balancer** açılır. Her bir ALB için ayda ~25-30 dolar taban ücret + trafik parası ödenir. Bu hem devasa bir maliyet faturasıdır hem de mimari bir israftır.

#### Kurtarıcı: Ingress Felsefesi
Ingress işte bu maliyet ve karmaşa krizine çözüm olarak doğmuştur:
> *"Cluster'ın kapısına tek bir akıllı kapıcı (Load Balancer) koyalım. Tüm trafik 80/443 portlarından bu kapıcıya gelsin. Kapıcı gelen isteğin domain adına (`Host: api.hastanem.com`) veya yoluna (`Path: /api/patients`) baksın ve trafiği arka plandaki onlarca ClusterIP servisine içeriden dağıtsın."*

---

## BÖLÜM 2: İki Farklı Kavramın Ayrımı (En Büyük Kafa Karışıklığı)

Kubernetes literatüründe **Ingress** ile **Ingress Controller** birbirine çok karıştırılır. Ancak bu iki kavram tamamen farklı katmanlardadır.

### 2.1 Ingress (Bir Kural / Bir Şartname)
Ingress, Kubernetes API'sinde tanımlanmış saf bir **veri modelidir (YAML)**.
* Kendi başına çalışan bir süreç (process), bir konteyner veya bir sunucu **değildir**.
* Yalnızca bir kurallar bütünüdür: *"Eğer `his.local` adresine `/api` yoluyla bir istek gelirse, bunu `api-gateway` isimli servisin `4004` portuna yolla."*
* Bir cluster'a sadece `ingress.yaml` uygularsanız hiçbir şey çalışmaz; çünkü o kuralı okuyup uygulayacak bir yazılım yoktur.

### 2.2 Ingress Controller (İşçi / Yönlendirici Motor)
Ingress Controller, cluster içinde çalışan gerçek bir **uygulamadır (Pod / DaemonSet)**.
* Kubernetes API Server'ı kesintisiz olarak dinler (watch mekanizması).
* Siz yeni bir `ingress.yaml` uyguladığınızda veya bir pod açılıp kapandığında bunu anında fark eder.
* Kendi içindeki yönlendirme motorunun (örneğin Nginx) konfigürasyonunu günceller ve trafiği yönetir.

> **Analoji**:
> * **Ingress**: Yazılı bir kanun maddesidir (Anayasa / Sözleşme).
> * **Ingress Controller**: O kanunu sahada uygulayan polis memurudur. Kanun metni tek başına suçluyu yakalayamaz; memur olmadan kanun anlamsızdır, kanun olmadan memur ne yapacağını bilemez.

---

## BÖLÜM 3: Nginx Nedir ve Ingress Dünyasındaki Rolü Nedir?

### 3.1 Nginx'in Kökeni ve C10k Problemi
2000'li yılların başında web sunucuları (özellikle Apache), her gelen bağlantı için işletim sisteminde yeni bir iş parçacığı (thread) veya süreç (process) açıyordu. Bu durum sunucuya aynı anda 10.000 kullanıcı bağlandığında (C10k problemi) sunucuların bellek yetersizliğinden çökmesine neden oluyordu.

Igor Sysoev, bu sorunu çözmek için **Nginx**'i geliştirdi. Nginx, iş parçacığı açmak yerine **olay güdümlü (event-driven, non-blocking)** tek bir döngü üzerinde çalışır. Bu sayede çok az RAM ve CPU kullanarak yüz binlerce eşzamanlı HTTP bağlantısını yönetebilir. Bugün dünyanın en popüler ters vekil (reverse proxy) sunucusudur.

### 3.2 Nginx Ingress Controller Nasıl Çalışır?

Bir Nginx Ingress Controller pod'unun içine girip bakacak olursanız iki ana bileşen görürsünüz:

```text
┌─────────────────────────────────────────────────────────────┐
│              Ingress-Nginx Controller Pod'u                 │
│                                                             │
│  ┌────────────────────────┐      ┌───────────────────────┐  │
│  │   Go Controller Süreci │      │      Nginx Süreci     │  │
│  │                        │      │                       │  │
│  │ - K8s API'yi dinler    │─────>│ - nginx.conf dosyasını│  │
│  │ - Endpoint'leri izler  │      │   dinamik günceller   │  │
│  │ - Lua ile rota yönetir │      │ - 80 ve 443'ü dinler  │  │
│  └────────────────────────┘      └───────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

1. **Go Programı (Controller)**: Kubernetes dünyasıyla konuşur. Hangi pod öldü, hangi yeni Ingress tanımlandı takip eder.
2. **Nginx Master/Worker Süreçleri**: Ağ trafiğini karşılayan gerçek C motorudur.

#### Güncelleme Döngüsü:
Siz `kubectl apply -f ingress.yaml` dediğinizde:
1. Go denetleyicisi Kubernetes API'sinden bildirimi alır.
2. Kuralı analiz eder: `his.local -> api-gateway:4004`.
3. Arka planda `api-gateway` pod'larının o anki IP adreslerini (`Endpoints`) toplar.
4. Bu bilgileri Nginx'in anlayacağı bir `upstream` bloğuna çevirir.
5. Modern Nginx Ingress Controller, pod IP'leri her değiştiğinde Nginx'i baştan başlatmak (reload) yerine, **Lua dinamik yönlendirme modülü** sayesinde bellek içindeki yönlendirme tablosunu sıfır milisaniye gecikmeyle günceller.

---

## BÖLÜM 4: Uçtan Uca Paket Yolculuğu (Packet Lifecycle)

Bir kullanıcının `http://his.local/api/patients` çağırmasından yanıt almasına kadar paketlerin fiziksel akışı:

```text
[ 1. İstemci (Browser) ]
       │  (Host: his.local, GET /api/patients)
       ▼
[ 2. DNS Çözümleme ] (his.local -> LoadBalancer / Node IP)
       │
       ▼ (TCP Port 80/443)
[ 3. Nginx Ingress Controller ]
       │
       ├── a) SSL/TLS Sonlandırma: Şifreli trafiği çözer.
       ├── b) Host Eşleştirme: "Host başlığı his.local mi?" -> Evet.
       ├── c) Path Eşleştirme: "Yol / ile mi başlıyor?" -> Evet.
       ├── d) Upstream Seçimi: api-gateway pod'unun IP'si (10.244.0.29:4004).
       │
       ▼ (Cluster İçi HTTP)
[ 4. api-gateway Pod (Netty) ]
       │
       ├── a) Güvenlik Filtresi: JWT geçerli mi?
       ├── b) Rate Limiter: Redis üzerinde IP kotası var mı?
       ├── c) Rota Eşleme: /api/patients/** -> patient-management:8080.
       │
       ▼ (Cluster İçi HTTP)
[ 5. patient-management Pod ]
       │  (Veritabanı sorgusu, JSON üretimi)
       │
       ▼ (Geriye Dönüş Zinciri)
[ patient-management ] ──> [ api-gateway ] ──> [ Ingress ] ──> [ İstemci ]
```

---

## BÖLÜM 5: Neden Hem Ingress Hem API Gateway Kullanıyoruz? (Birbirinin Rakibi mi?)

En sık sorulan mimari soru:
*"Zaten Spring Cloud Gateway'imiz var, neden önüne bir de Nginx Ingress koyduk? İkisi de yönlendirme yapmıyor mu?"*

Hayır, birbirlerinin rakibi değil, **tamamlayıcısıdırlar**. Görev alanları farklı katmanlardadır:

| Özellik | Nginx Ingress Controller | Spring Cloud (API) Gateway |
| :--- | :--- | :--- |
| **Odak Katmanı** | **Altyapı & Küme Sınırı (Infrastructure/Edge)** | **Uygulama & İş Mantığı (Application Logic)** |
| **Teknoloji** | C / Nginx / Go | Java / Spring Boot / Project Reactor |
| **Temel Görevi** | SSL sonlandırma, domain yönlendirme, dış dünyadan içeriye trafik alma. | JWT doğrulama, kullanıcı bazlı iş kuralı filtreleri, header mutasyonu. |
| **Geliştirici Müdahalesi** | DevOps / SRE ekipleri yönetir; küme genelinde 1 adet kurulur. | Yazılım ekibi yönetir; projenin kod tabanının bir parçasıdır. |
| **Performans Odaklılık**| Statik trafik, kaba yük dağıtımı, çok düşük kaynak tüketimi. | Dinamik iş kuralları, mikroservis kimlik bağlama. |

**Mükemmel İş Bölümü**:
Nginx Ingress dışarıdaki kaba trafiği, SSL şifrelerini ve alan adlarını karşılar. Temizlenmiş trafiği tek bir güvenli kapıdan `api-gateway`'e verir. `api-gateway` ise iş mantığına (JWT rolleri, hasta hakları, servis rotaları) bakar.

---

## BÖLÜM 6: Alternatifler ve Modern Trendler

Tek Ingress çözümü Nginx değildir; sektörde ihtiyaca göre kullanılan popüler alternatifler şunlardır:

1. **Traefik**: Go ile yazılmıştır, mikroservis odaklıdır, otomatik Let's Encrypt SSL yönetimiyle öne çıkar.
2. **Envoy / Istio / Emissary**: C++ ile yazılmış modern bir proxy'dir. Service Mesh yapılarında mikroservisler arası mTLS için endüstri standardıdır.
3. **AWS Load Balancer Controller**: Nginx gibi bir ara katman yerine, doğrudan AWS ALB'nin pod IP'lerine (Target Group Binding) trafik göndermesini sağlar.
4. **Kubernetes Gateway API (Geleceğin Standardı)**:
   Ingress yapısı 2015'ten kalmadır ve üreticiye özgü `annotation` bağımlılığı yüksektir. Kubernetes topluluğu, Ingress'in yerini alması için **Gateway API** (`GatewayClass`, `Gateway`, `HTTPRoute`) standardını geliştirmiştir.

---

## BÖLÜM 7: Bizim Projemizdeki Uygulamanın Özeti

Projemizde şu an çalışan sistem:
* **Controller**: Minikube üzerinde çalışan resmi `ingress-nginx-controller`.
* **Kural Dosyası**: `kubernetes/base/ingress/ingress.yaml` dosyası ile `his.local` adresine gelen tüm istekler `api-gateway:4004` servisine bağlanmıştır.
* **Tünel İhtiyacı**: Minikube Docker motoru içinde izole bir sanal ağda yaşadığından, yerel makinenin bu Nginx Ingress'in 80 portuna erişebilmesi için `minikube tunnel` köprüsü kullanılmaktadır.

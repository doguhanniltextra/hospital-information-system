# Auth-Service Kubernetes Uyumlandirma, Konteyner Optimizasyonu ve CI/CD Gorev Dokumani

Bu dokuman, Hospital Information System (HIS) projesinin merkezi kimlik dogrulama, yetkilendirme ve token uretim bileseni olan `auth-service` mikroservisinin Kubernetes ortaminda calistirilmasi, Docker imaj optimizasyonu ve GitOps/CI-CD is akislarinin kurulmasi surecini moduler gorev belgelerine ayirarak yonetir.

Detayli gorev belgeleri `.agent/tasks/auth-service/` dizini altinda asamali olarak hazirlanmistir.

---

## Moduler Gorev Belgeleri Haritasi

1. **[Task 01: Stateful Bagimliliklar ve Yerel On Kosullar](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/01_STATEFUL_DEPENDENCIES.md)**
   * PostgreSQL (`auth-db`, port 5438, `auth_schema`, `init.sql`) ve Apache Kafka (`kafka`, port 9092) servislerinin yerel Docker'da baslatilmasi ve baglanti kontrolleri.
   * Redis ve MongoDB'nin bu servis tarafindan kullanilmadiginin tespiti.

2. **[Task 02: Kaynak Kod ve Konfigurasyon Hazirliklari](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/02_CODE_AND_CONFIGURATION_READINESS.md)**
   * `auth-service/src/main/resources/application.properties` dosyasina zarif kapanma (graceful shutdown 30s), Actuator Kubernetes state problari (`liveness`/`readiness`) ve `server.forward-headers-strategy=framework` eklenmesi.

3. **[Task 03: Dockerfile Optimizasyonu ve Imaj Politikasi](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md)**
   * Cok asamali (multi-stage) Dockerfile ile Maven bagimlilik katman onbelleginin kurulmasi (`dependency:go-offline`).
   * Alpine tabanli JRE calisma katmani, non-root `appuser:appgroup` guvenlik kullanicisi ve JVM bellek optimizasyonlari.
   * `doguhannilt/auth-service:latest`, `:sha-xxxx` etiketleme formati.

4. **[Task 04: Kubernetes Manifest Paketi](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/04_KUBERNETES_MANIFESTS.md)**
   * `kubernetes/base/apps/auth-service/` altinda 10 adet kurumsal K8s manifestinin (Deployment, Service, ConfigMap, Secret, Secret.template, HPA, PDB, ServiceAccount, NetworkPolicy, Kustomization) olusturulmasi.

5. **[Task 05: Moduler CI/CD ve GitOps Is Akislari](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/05_CICD_AND_GITOPS_WORKFLOWS.md)**
   * `.github/workflows/auth-service/` dizininde 5 bagimsiz is akisinin (`pr-check`, `unit-test`, `docker-build-and-push`, `k8s-validate`, `gitops-promote`) yazilmasi ve kok dizin sembolik baglantilari (symlinks).

6. **[Task 06: Dagitim, Dogrulama ve Sorun Giderme Kilavuzu](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md)**
   * Minikube kumesine dagitim adimlari, pod saglik kontrolleri, actuator uclari sorgulari, Ingress ucuca dogrulama ve olasi veritabani/Kafka baglanti hatalarini cozme kilavuzu.

---

Ayrintili yol haritasi icin [auth-service/README.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/README.md) dosyasini inceleyebilirsiniz.

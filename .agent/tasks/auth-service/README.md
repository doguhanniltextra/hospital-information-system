# Auth-Service Kubernetes ve CI/CD Gorev Plani (Gorev Haritasi)

Bu dizin, Hospital Information System (HIS) projesinin `auth-service` mikroservisini Kubernetes uretim ve yerel Minikube ortamlarina hazirlamak, Docker imajini optimize etmek ve moduler CI/CD/GitOps is akislarini kurmak icin gereken gorevleri asamali olarak icerir.

Her bir belge, gorevi gerceklestirecek baska bir yapay zeka ajaninin veya muhendisin baska bir kaynaga basvurmadan tek basina calistirabilecegi sekilde ayrintilandirilmistir.

---

## Gorev Listesi ve Uygulama Sirasi

| Sira | Gorev Dokumani | Kapsam | Durum |
| :---: | :--- | :--- | :---: |
| **01** | [01_STATEFUL_DEPENDENCIES.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/01_STATEFUL_DEPENDENCIES.md) | PostgreSQL (`auth-db`, port 5438, `auth_schema`) ve Apache Kafka (`kafka`, port 9092) servislerinin Docker uzerinde baslatilmasi ve baglanti dogrulamasi. | Hazir |
| **02** | [02_CODE_AND_CONFIGURATION_READINESS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/02_CODE_AND_CONFIGURATION_READINESS.md) | `application.properties` dosyasina zarif kapanma (graceful shutdown 30s), Actuator Kubernetes state problari (`liveness`/`readiness`) ve `X-Forwarded-*` desteğinin eklenmesi. | Hazir |
| **03** | [03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/03_DOCKERFILE_OPTIMIZATION_AND_IMAGE_PIPELINE.md) | Cok asamali (multi-stage) Dockerfile ile Maven bagimlilik katman onbelleginin kurulmasi, non-root `appuser` guvenligi ve `doguhannilt/auth-service:latest` imaj derlemesi. | Hazir |
| **04** | [04_KUBERNETES_MANIFESTS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/04_KUBERNETES_MANIFESTS.md) | `kubernetes/base/apps/auth-service/` altinda Deployment (2 replicas, startup/liveness/readiness problari, preStop sleep 5), Service, ConfigMap, Secret, HPA, PDB, NetworkPolicy ve Kustomization manifestlerinin yazilmasi. | Hazir |
| **05** | [05_CICD_AND_GITOPS_WORKFLOWS.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/05_CICD_AND_GITOPS_WORKFLOWS.md) | `.github/workflows/auth-service/` altinda moduler 5 is akisinin (PR Check, Unit Tests, Docker Build & Push, K8s Validate, GitOps Promote) olusturulmasi ve kok symlink baglantilari. | Hazir |
| **06** | [06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/tasks/auth-service/06_DEPLOYMENT_AND_VERIFICATION_GUIDE.md) | Minikube'e dagitim komutlari, pod saglik testleri, `/actuator/health/readiness` sorgulari ve API Gateway uzerinden ucuca test kilavuzu. | Hazir |

---

## Uygulama Kurali

Bir sonraki goreve gecmeden once mevcut gorevin dosya icindeki **Gorev Tamamlanma Kriterleri** bolumundeki tum maddelerin dogrulanmasi sarttir.

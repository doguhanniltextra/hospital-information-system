# 🧩 Görev 02: Reusable (Yeniden Kullanılabilir) CI/CD Pipeline Tasarımı

Bu görev; 9 mikroservisin tümü tarafından çağrılacak olan, kod tekrarını önleyen (DRY), `runs-on: [self-hosted, linux]` uyumlu merkezi GitHub Actions Reusable Workflow mimarisinin tasarlanmasını içerir.

---

## 🎯 Amaç
- Tek bir merkezi iş akışı dosyası (`.github/workflows/reusable-service-ci.yml`) oluşturmak.
- Servis adı (`service_name`), dizin yolu (`service_path`), imaj adı (`image_name`) ve overlay yolu (`k8s_overlay_path`) parametrelerini dışarıdan alarak dinamik çalıştırmak.
- Pipeline'da `runs-on: [self-hosted, linux]` kullanarak yerel kaynakları maksimum verimle çalıştırmak.

---

## 📐 1. Pipeline Sözleşmesi (Inputs & Secrets Contract)

Reusable Workflow aşağıdaki giriş parametrelerine ve gizli bilgilere (secrets) sahip olmalıdır:

### Girdiler (Inputs):
| Parametre | Tip | Zorunlu mu? | Varsayılan | Açıklama |
| :--- | :---: | :---: | :--- | :--- |
| `service_name` | `string` | Evet | - | Mikroservis adı (örn: `doctor-service`) |
| `service_path` | `string` | Evet | - | Servisin kök dizini (örn: `doctor-service`) |
| `image_name` | `string` | Evet | - | Docker Hub imaj adı (örn: `doguhannilt/doctor-service`) |
| `k8s_overlay_path` | `string` | Hayır | `kubernetes/overlays/prod` | Kustomize hedef overlay klasörü |
| `java_version` | `string` | Hayır | `21` | Hedef Java LTS sürümü |

### Gizli Değerler (Secrets):
| Secret Adı | Zorunlu mu? | Açıklama |
| :--- | :---: | :--- |
| `DOCKERHUB_USERNAME` | Evet | Docker Hub kullanıcı adı |
| `DOCKERHUB_TOKEN` | Evet | Docker Hub Read/Write Access Token |
| `GITOPS_PAT_TOKEN` | Hayır | (Varsa) Kustomize repo commit push yetkisi için Personal Access Token veya `GITHUB_TOKEN` |

---

## 🛠️ 2. Reusable İş Akışı İskeleti

Oluşturulacak dosya: [`.github/workflows/reusable-service-ci.yml`](file:///home/doguhan/SoftwareProjects/hospital-information-system/.github/workflows/reusable-service-ci.yml)

```yaml
name: Reusable Microservice DevSecOps & GitOps Pipeline

on:
  workflow_call:
    inputs:
      service_name:
        required: true
        type: string
      service_path:
        required: true
        type: string
      image_name:
        required: true
        type: string
      k8s_overlay_path:
        required: false
        type: string
        default: "kubernetes/overlays/prod"
      java_version:
        required: false
        type: string
        default: "21"
    secrets:
      DOCKERHUB_USERNAME:
        required: true
      DOCKERHUB_TOKEN:
        required: true

jobs:
  code-quality-and-test:
    name: 🧪 Quality & Tests (${{ inputs.service_name }})
    runs-on: [self-hosted, linux]
    # Görev 03 detayları buraya bağlanır
    
  docker-build-scan-push:
    name: 🐳 Docker Build & Security Gate
    needs: code-quality-and-test
    runs-on: [self-hosted, linux]
    # Görev 03 detayları buraya bağlanır

  gitops-update-and-k8s-validate:
    name: ☸️ GitOps Manifest & K8s Validation
    needs: docker-build-scan-push
    runs-on: [self-hosted, linux]
    # Görev 04 detayları buraya bağlanır
```

---

## ✅ Görev Tamamlanma Kriterleri

- [ ] `.github/workflows/reusable-service-ci.yml` dosyası oluşturuldu.
- [ ] Tüm iş adımlarında (jobs) `runs-on: [self-hosted, linux]` yapılandırıldı.
- [ ] Parametreler 9 mikroservisin tamamını kapsayacak esneklikte tanımlandı.

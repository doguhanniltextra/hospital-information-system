# 🛠️ Görev 01: GitHub Actions Self-Hosted Runner Kurulum ve Konfigürasyonu

Bu görev, yerel makinenizde veya özel sunucunuzda GitHub Actions işlerini güvenli, hızlı ve kurumsal standartlara uygun şekilde yürütecek **Self-Hosted Runner** ortamının hazırlanması adımlarını içerir.

---

## 🎯 Amaç
GitHub bulut runner'ları yerine kendi altyapınızı kullanarak:
- Yerel Docker daemon ve Maven yerel repository (`~/.m2/repository`) önbelleğini doğrudan kullanarak build sürelerini saniyeler seviyesine indirmek.
- Kubernetes CLI araçlarını (`kustomize`, `kubeconform`, `kubectl`) doğrudan yerel makinede yüksek hızla çalıştırmak.
- Ağ maliyetlerini ve GitHub Actions kota sınırlarını ortadan kaldırmak.

---

## 📦 1. Self-Hosted Runner Makine Gereksinimleri

Runner çalışacak makinede aşağıdaki araçların kurulu olması şarttır:

1. **Docker Engine & Buildx:** Docker daemon açık olmalı ve runner kullanıcısı `docker` grubunda yer almalıdır.
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   docker info
   ```
2. **Java JDK 21 & Maven:**
   ```bash
   java -version    # OpenJDK / Temurin 21+
   mvn -version     # Apache Maven 3.9+
   ```
3. **Kustomize (v5.3+):**
   ```bash
   curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
   sudo mv kustomize /usr/local/bin/
   kustomize version
   ```
4. **Kubeconform (Kubernetes OpenAPI Schema Validator):**
   ```bash
   curl -sL https://github.com/yannh/kubeconform/releases/latest/download/kubeconform-linux-amd64.tar.gz | tar xz
   sudo mv kubeconform /usr/local/bin/
   kubeconform -v
   ```
5. **Trivy (Security Vulnerability Scanner):**
   ```bash
   curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
   trivy --version
   ```

---

## 🚀 2. GitHub Self-Hosted Runner İndirme ve Kayıt

1. GitHub Repository -> **Settings** -> **Actions** -> **Runners** -> **New self-hosted runner** sayfasına gidin.
2. İşletim sistemi: `Linux`, Mimari: `x64` seçin.
3. İndirme ve kurulum komutlarını çalıştırın:
   ```bash
   # Runner dizini oluştur
   mkdir -p ~/actions-runner && cd ~/actions-runner
   
   # En güncel runner paketini indir
   curl -o actions-runner-linux-x64-2.321.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-linux-x64-2.321.0.tar.gz
   tar xzf ./actions-runner-linux-x64-2.321.0.tar.gz
   
   # GitHub token ile konfigüre et (Label: self-hosted, linux, x64, his-runner)
   ./config.sh --url https://github.com/<GITHUB_KULLANICI>/hospital-information-system --token <TOKEN> --labels self-hosted,linux,x64,his-runner --name his-local-runner --unattended
   ```

---

## 🛡️ 3. Runner'ı Arka Planda Servis Olarak Başlatma (Systemd)

Runner'ın terminal kapansa veya makine yeniden başlasa dahi kesintisiz çalışması için systemd servisi olarak kurulmalıdır:

```bash
cd ~/actions-runner
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

---

## ✅ Görev Tamamlanma Kriterleri

- [ ] Runner makinesinde `docker`, `mvn`, `java -version (21)`, `kustomize`, `kubeconform` ve `trivy` komutları terminalde global olarak çalışıyor.
- [ ] GitHub Repository **Settings -> Actions -> Runners** ekranında runner durumu **"Idle" (Yeşil)** olarak görünüyor.
- [ ] Runner üzerinde `self-hosted` ve `linux` etiketleri aktif.

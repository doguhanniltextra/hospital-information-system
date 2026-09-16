# 🏥 Hospital Information System - Platform Doğrulama ve Kanıt Çalıştırma Rehberi (Runbook)

Bu doküman; **Hospital Information System (HIS)** projesinde yer alan 9 mikroservisin, 8 PostgreSQL veritabanının, Kafka mesajlaşma kuyruğunun, Redis önbelleğinin, HashiCorp Vault gizli bilgi yönetiminin ve Kubernetes PSS (Pod Security Standards) Restricted güvenlik politikalarının eksiksiz çalıştığını kanıtlamak ve denetim raporu sunmak için hazırlanmıştır.

---

## 🎯 Hızlı Başlatma ve Tek Komutla Kanıt Üretimi

Sistemin tüm katmanlarını otomatik başlatmak ve tüm testleri çalıştırıp kanıt raporunu (`.agent/reports/PLATFORM_AUDIT_REPORT.md`) oluşturmak için iki temel script hazırlanmıştır:

### 1. Platformu Güvenli ve Kararlı Başlatma
```bash
./.agent/scripts/start-platform.sh
```
> **Bu komut ne yapar?**
> - `his-postgres`, `kafka` ve `redis` konteynerlerinin açık ve sağlıklı olduğunu doğrular.
> - Minikube kümesini 4GB bellek ve 4 CPU sınırı ile başlatır.
> - HashiCorp Vault KV-v2 gizli bilgilerini, ACL politikalarını ve Kubernetes Auth rollerini yükler (`init-vault.sh`).
> - Tüm servislerin **tam olarak 1 replica** ile çalışmasını garanti eder.

### 2. Kapsamlı Denetim ve Kanıt Raporu Üretme
```bash
./.agent/scripts/verify-platform.sh
```
> **Bu komut ne yapar?**
> - 8 PostgreSQL veritabanını, tablolarını ve bağlantılarını test eder.
> - Kafka 22 topic ve DLQ kuyruklarını denetler.
> - Vault ve External Secrets Operator senkronizasyonunu (`SecretSynced=True`) denetler.
> - PSS Restricted güvenlik kriterlerini (UID 1000, ReadOnly RootFS, Drop ALL Capabilities) denetler.
> - Tüm 9 mikroservisin Spring Boot `/actuator/health` uç noktalarını port-forward ile sorgular ve JSON durumlarını doğrular.
> - Sonuçları `.agent/reports/PLATFORM_AUDIT_REPORT.md` dosyasına tablo formatında yazar.

---

## 🔍 Manuel Denetim ve Kanıt Komutları Kataloğu

Herhangi bir sunumda veya teknik incelemede adım adım bağımsız kanıt sunmak için aşağıdaki komut sırasını takip edebilirsiniz:

---

### Adım 1: Stateful Altyapı ve Veritabanı Sağlamlık Kanıtları

#### 1.1 Docker Konteynerlerinin Durumu
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```
*Beklenen Çıktı:* `his-postgres` (Up), `kafka` (Up healthy), `redis` (Up).

#### 1.2 PostgreSQL 8 İzole Veritabanının ve Tablolarının Kanıtı
```bash
# Tüm 8 veritabanını ve bağlantı durumunu listele
for db in auth_db patient_db doctor_db appointment_db admission_db support_db billing_db notification_db; do
    echo "=== Veritabanı: $db ==="
    docker exec -i his-postgres psql -U postgres -d "$db" -c "\dt"
done
```

#### 1.3 Kafka Topic ve DLQ Kuyruklarının Kanıtı
```bash
docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```
*Beklenen Çıktı:* 22 iş kuyruğu ve 6 DLQ (Dead Letter Queue) listelenmelidir.

#### 1.4 Redis Bağlantı Kanıtı
```bash
docker exec -i redis redis-cli ping
```
*Beklenen Çıktı:* `PONG`

---

### Adım 2: HashiCorp Vault ve External Secrets Senkronizasyon Kanıtları

#### 2.1 Vault KV-v2 Secret'larının Okunabilirliği
```bash
kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root
echo '=== Shared Secrets ==='
vault kv get secret/hospital/shared/jwt
echo '=== Doctor Service DB Secret ==='
vault kv get secret/hospital/doctor-service/db
"
```

#### 2.2 ExternalSecrets Senkronizasyon Durumu (9/9 True)
```bash
kubectl get externalsecrets -o custom-columns=NAME:.metadata.name,STORE:.spec.secretStoreRef.name,READY:.status.conditions[0].status,REASON:.status.conditions[0].reason
```
*Beklenen Çıktı:* Tüm satırlarda `READY: True` ve `REASON: SecretSynced` olmalıdır.

#### 2.3 Kubernetes Native Secret Nesnelerinin Oluştuğunun Kanıtı
```bash
kubectl get secrets -l app.kubernetes.io/part-of=hospital-information-system
```

---

### Adım 3: Kubernetes Pod ve PSS Restricted Güvenlik Politikaları Kanıtları

#### 3.1 Tüm Pod'ların 1/1 Running Durumu
```bash
kubectl get pods -o wide
```
*Beklenen Çıktı:* 9 mikroservis pod'unun tamamı `1/1 Running` ve `0 CrashLoopBackOff` durumunda olmalıdır.

#### 3.2 PSS Restricted Güvenlik Standartları Kanıtı
```bash
# Non-root UID 1000, ReadOnly Root Filesystem, Drop ALL Capabilities Kontrolü
kubectl get pods -o custom-columns=\
NAME:.metadata.name,\
RUN_AS_USER:.spec.securityContext.runAsUser,\
READ_ONLY_ROOT_FS:.spec.containers[0].securityContext.readOnlyRootFilesystem,\
DROP_CAPS:.spec.containers[0].securityContext.capabilities.drop,\
PRIVILEGE_ESC:.spec.containers[0].securityContext.allowPrivilegeEscalation
```
*Beklenen Çıktı:*
- `RUN_AS_USER`: `1000`
- `READ_ONLY_ROOT_FS`: `true`
- `DROP_CAPS`: `[ALL]`
- `PRIVILEGE_ESC`: `false`

---

### Adım 4: Spring Boot Actuator Health (`/actuator/health`) Kanıtları

Her mikroservisin `/actuator/health` uç noktası doğrudan mikroservis içerisindeki veritabanı, disk ve önbellek bileşenlerinin canlılığını doğrular:

```bash
# Python ile tüm servisleri port-forward ederek health durumlarını JSON olarak doğrula
python3 -c "
import urllib.request, json, subprocess, time

services = {
    'api-gateway': 4004,
    'auth-service': 8089,
    'patient-management': 8080,
    'doctor-service': 8083,
    'appointment-service': 8084,
    'admission-service': 8086,
    'support-service': 8085,
    'billing-service': 8081,
    'notification-service': 8090
}

for svc, port in services.items():
    proc = subprocess.Popen(['kubectl', 'port-forward', f'svc/{svc}', f'{port}:{port}'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1.5)
    try:
        req = urllib.request.Request(f'http://localhost:{port}/actuator/health', headers={'User-Agent': 'AuditCheck'})
        with urllib.request.urlopen(req, timeout=4) as resp:
            data = json.loads(resp.read().decode())
            status = data.get('status')
            db_status = data.get('components', {}).get('db', {}).get('status', 'N/A')
            print(f'✅ {svc:<22} (Port {port}): STATUS = {status:<6} | DB = {db_status}')
    except Exception as e:
        print(f'❌ {svc:<22} (Port {port}): HATA ({e})')
    finally:
        proc.terminate()
"
```

---

## 📊 Özet Servis Port ve Envanter Tablosu

| Mikroservis Adı | K8s Port | Veritabanı | Vault Yolu | PSS Restricted | Actuator Uç Noktası |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`api-gateway`** | `4004` | - (Redis) | `secret/hospital/shared/jwt` | ✅ Uyumlu (UID 1000) | `http://localhost:4004/actuator/health` |
| **`auth-service`** | `8089` | `auth_db` | `secret/hospital/auth-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8089/actuator/health` |
| **`patient-management`** | `8080` (gRPC: 9090) | `patient_db` | `secret/hospital/patient-management/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8080/actuator/health` |
| **`doctor-service`** | `8083` (gRPC: 9005) | `doctor_db` | `secret/hospital/doctor-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8083/actuator/health` |
| **`appointment-service`** | `8084` | `appointment_db` | `secret/hospital/appointment-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8084/actuator/health` |
| **`admission-service`** | `8086` | `admission_db` | `secret/hospital/admission-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8086/actuator/health` |
| **`support-service`** | `8085` | `support_db` | `secret/hospital/support-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8085/actuator/health` |
| **`billing-service`** | `8081` | `billing_db` | `secret/hospital/billing-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8081/actuator/health` |
| **`notification-service`** | `8090` | `notification_db` | `secret/hospital/notification-service/db` | ✅ Uyumlu (UID 1000) | `http://localhost:8090/actuator/health` |

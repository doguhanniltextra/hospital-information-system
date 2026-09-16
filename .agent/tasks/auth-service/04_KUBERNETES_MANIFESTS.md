# Task 04: Kubernetes Manifest Paketi

Bu gorev, `auth-service` uygulamasinin Kubernetes ortaminda yuksek erisilebilirlik (HA), otomatik olcekleme (HPA), ag izolasyonu (NetworkPolicy) ve guvenli pod yasam dongusu standartlarina uygun calismasi icin gereken tum Kustomize manifestlerinin olusturulmasini kapsar.

---

## 1. Dizin Yapisi

Tum manifestler su dizinde yer alacaktir:
* **Dizin:** `kubernetes/base/apps/auth-service/`

---

## 2. Olusturulacak Manifest Dosyalari

### 2.1 `serviceaccount.yaml`
Kubernetes API erisim token'inin guvenlik gerekcesiyle otomatik baglanmasi engellenir.
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: auth-service-sa
automountServiceAccountToken: false
```

### 2.2 `configmap.yaml`
Uygulamanin gizli olmayan cevre degiskenlerini barindirir. Yerel Minikube ortaminda host'taki PostgreSQL ve Kafka adreslerine yonlendirilir.
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-service-config
data:
  SPRING_PROFILES_ACTIVE: "dev"
  SERVER_PORT: "8089"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://host.minikube.internal:5438/auth_db?currentSchema=auth_schema&reWriteBatchedInserts=true"
  KAFKA_BOOTSTRAP_SERVERS: "host.minikube.internal:9092"
```

### 2.3 `secret.yaml`
Hassas parolalari ve JWT anahtarlarini tutar (Yerel gelistirme icin).
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secret
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "auth_user"
  SPRING_DATASOURCE_PASSWORD: "auth_pass_123"
  APP_SECRET: "mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong"
  JWT_SECRET: "dev_jwt_secret_change_me"
  API_KEY: "auth-service-internal-api-key"
```

### 2.4 `secret.template.yaml`
Depoya acik sifre girmemek adina ornek sablon dosyasidir.
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secret
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "auth_user"
  SPRING_DATASOURCE_PASSWORD: "CHANGE_ME"
  APP_SECRET: "CHANGE_ME_AT_LEAST_256_BITS"
  JWT_SECRET: "CHANGE_ME_JWT_SECRET"
  API_KEY: "CHANGE_ME"
```

### 2.5 `service.yaml`
Kume icinde port `8089` uzerinden calisan ClusterIP servisidir.
```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  type: ClusterIP
  ports:
    - name: http
      port: 8089
      targetPort: 8089
      protocol: TCP
  selector:
    app.kubernetes.io/name: auth-service
```

### 2.6 `deployment.yaml`
Kritik ozellikler:
* **2 Replicas:** Yuksek erisilebilirlik.
* **RollingUpdate Stratejisi:** `maxSurge: 1`, `maxUnavailable: 0` (Sifir kesinti).
* **Startup Probe:** HikariCP ve Kafka ilk baglantilarinin kurulmasi icin pod'a 150 saniyeye kadar tolerans tanir (`failureThreshold: 30`, `periodSeconds: 5`).
* **Liveness & Readiness Probes:** Actuator state uclarini denetler.
* **PreStop Hook:** Pod sonlandirilmadan once 5 saniye bekleyerek kube-proxy endpoint guncellemelerini karsilar.
* **Non-Root Guvenlik:** `runAsUser: 1000`, `drop: [ALL]`.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app.kubernetes.io/name: auth-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: auth-service
    spec:
      serviceAccountName: auth-service-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
        - name: auth-service
          image: doguhannilt/auth-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8089
          envFrom:
            - configMapRef:
                name: auth-service-config
            - secretRef:
                name: auth-service-secret
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 5"]
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8089
            initialDelaySeconds: 15
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8089
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8089
            periodSeconds: 5
            failureThreshold: 2
          resources:
            requests:
              cpu: 250m
              memory: 384Mi
            limits:
              cpu: 1000m
              memory: 768Mi
          securityContext:
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
```

### 2.7 `hpa.yaml`
Trafik yukseldiginde pod sayisini otomatik 2'den 5'e cikarir.
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 75
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### 2.8 `pdb.yaml`
Node bakimlarinda veya tahliyelerinde (drain) en az 1 pod'un her zaman ayakta kalmasini garanti eder.
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: auth-service-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: auth-service
```

### 2.9 `network-policy.yaml`
Yalnizca `api-gateway` ve sistem ici servislerin HTTP portuna (8089) ulasmasina izin verir.
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: auth-service-netpol
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: auth-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
      ports:
        - protocol: TCP
          port: 8089
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/part-of: hospital-information-system
      ports:
        - protocol: TCP
          port: 8089
  egress:
    - to:
        - namespaceSelector: {}
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
```

### 2.10 `kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

labels:
  - pairs:
      app.kubernetes.io/name: auth-service
      app.kubernetes.io/part-of: hospital-information-system
      app.kubernetes.io/component: identity-provider

resources:
  - serviceaccount.yaml
  - configmap.yaml
  - secret.yaml
  - service.yaml
  - deployment.yaml
  - hpa.yaml
  - pdb.yaml
  - network-policy.yaml
```

---

## 3. Dogrulama Komutu

```bash
kubectl kustomize kubernetes/base/apps/auth-service/ > /dev/null && \
kubectl apply --dry-run=client -k kubernetes/base/apps/auth-service/
```

---

## 4. Gorev Tamamlanma Kriterleri
- [x] `kubernetes/base/apps/auth-service/` dizininde 10 manifest dosyasi olusturuldu.
- [x] Problar, kaynak limitleri ve `preStop` hook'lari eksiksiz yazildi.
- [x] Kustomize derlemesi ve dry-run client testi basariyla gecti.


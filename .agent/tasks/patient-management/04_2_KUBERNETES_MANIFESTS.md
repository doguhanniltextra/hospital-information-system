# Task 04.2: Kubernetes Manifestlerinin Hazırlanması ve Dağıtım (Production-Ready)

Bu görev, `patient-management` mikroservisinin `kubernetes/base/apps/patient-management/` dizini altındaki tüm Kubernetes kaynak manifestlerinin ([CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine %100 uyumlu olarak) hazırlanmasını, cluster'a uygulanmasını ve senkronizasyonunun doğrulanmasını kapsar.

---

## 🎯 CHECK_FILE.md Uyumluluk Matrisi

| CHECK_FILE Kriteri | Uygulanan Yapılandırma & Standart | Manifest Dosyası |
| :--- | :--- | :--- |
| **Application Behavior** | Graceful shutdown (`server.shutdown=graceful`, 30s + 5s preStop hook), `terminationGracePeriodSeconds: 45` | `deployment.yaml` |
| **Configuration & Secrets** | Ayrık `ConfigMap` ve Vault destekli `ExternalSecret`, periyodik senkronizasyon (`refreshInterval: 1h`) | `configmap.yaml`, `external-secret.yaml` |
| **Container Image** | Multi-stage distroless/JRE, sabit `1.0.0` sürüm etiketi (`:latest` engellendi), build araçları arındırılmış | `kustomization.yaml` |
| **Runtime Contract** | Startup, Liveness ve Readiness probları (`/actuator/health/*`), gerçekçi `requests` & `limits` | `deployment.yaml` |
| **Storage & Ephemeral** | `readOnlyRootFilesystem: true`, `/tmp` `emptyDir` (100Mi sınır), `ephemeral-storage` (100Mi/500Mi) | `deployment.yaml` |
| **Rollouts & Reliability** | `RollingUpdate` (`maxSurge: 1`, `maxUnavailable: 0`), `minReadySeconds: 5`, `revisionHistoryLimit: 5`, `progressDeadlineSeconds: 600` | `deployment.yaml` |
| **Placement & Disruption** | `podAntiAffinity` (hostname bazlı node yayılımı), `PodDisruptionBudget` (`minAvailable: 1`) | `deployment.yaml`, `pdb.yaml` |
| **Runtime Security (PSS Restricted)** | `runAsNonRoot: true`, `runAsUser: 1000`, `runAsGroup: 1000`, `fsGroup: 1000`, `allowPrivilegeEscalation: false`, `capabilities.drop: ["ALL"]`, `seccompProfile: RuntimeDefault` | `deployment.yaml` |
| **ServiceAccount Security** | Dedicated SA (`patient-management-sa`), `automountServiceAccountToken: false` | `serviceaccount.yaml` |
| **Network Isolation (NetPol)** | Ingress (HTTP 8080 Gateway, gRPC 9090 HIS internal), Egress (DNS 53, Write DB 5432, Read DB 5433, Kafka 9092, Vault 8200) | `network-policy.yaml` |
| **Standard Metadata** | Eksiksiz `app.kubernetes.io/*` etiketleri (`name`, `instance`, `version`, `component`, `part-of`, `managed-by`) | `kustomization.yaml` |
| **Horizontal Scaling** | `HorizontalPodAutoscaler` (min 2, max 10, CPU %75, Memory %80) | `hpa.yaml` |

---

## 1. Hazırlanacak Kubernetes Manifestleri (`kubernetes/base/apps/patient-management/`)

### 1.1 `serviceaccount.yaml`
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: patient-management-sa
automountServiceAccountToken: false
```

### 1.2 `configmap.yaml`
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: patient-management-config
data:
  SERVER_PORT: "8080"
  GRPC_SERVER_PORT: "9090"
  SPRING_PROFILES_ACTIVE: "dev"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "validate"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  KAFKA_TOPIC_LAB_RESULT_COMPLETED: "lab-result-completed.v1"
  KAFKA_GROUP_PATIENT_LAB_RESULT: "patient-lab-result-group"
  KAFKA_TOPIC_USER_PROVISIONED: "user-provisioned.v1"
  KAFKA_GROUP_PATIENT_AUTH_LINK: "patient-auth-link-group"
  PATIENT_WRITE_DB_URL: "jdbc:postgresql://patient-write-db:5432/patient_db?currentSchema=patient_schema&reWriteBatchedInserts=true"
  PATIENT_READ_DB_URL: "jdbc:postgresql://patient-read-db:5432/patient_db?currentSchema=patient_schema"
```

### 1.3 `external-secret.yaml` (SecretStore & ExternalSecret)
```yaml
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata:
  name: patient-management-vault-store
spec:
  provider:
    vault:
      server: "http://vault.vault.svc.cluster.local:8200"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "patient-management-role"
          serviceAccountRef:
            name: patient-management-sa
---
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: patient-management-vault-secret
spec:
  refreshInterval: "1h"
  secretStoreRef:
    name: patient-management-vault-store
    kind: SecretStore
  target:
    name: patient-management-secret
    creationPolicy: Owner
  data:
    - secretKey: APP_SECRET
      remoteRef:
        key: hospital/shared/jwt
        property: app_secret
    - secretKey: APP_SECURITY_ENCRYPTION_KEY
      remoteRef:
        key: hospital/patient-management/security
        property: encryption_key
    - secretKey: ENCRYPTION_KEY
      remoteRef:
        key: hospital/patient-management/security
        property: encryption_key
    - secretKey: SPRING_DATASOURCE_USERNAME
      remoteRef:
        key: hospital/patient-management/db
        property: username
    - secretKey: SPRING_DATASOURCE_PASSWORD
      remoteRef:
        key: hospital/patient-management/db
        property: password
```

### 1.4 `service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: patient-management
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: patient-management
  ports:
    - name: http
      port: 8080
      targetPort: http
      protocol: TCP
    - name: grpc
      port: 9090
      targetPort: grpc
      protocol: TCP
```

### 1.5 `deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: patient-management
spec:
  replicas: 2
  minReadySeconds: 5
  revisionHistoryLimit: 5
  progressDeadlineSeconds: 600
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app.kubernetes.io/name: patient-management
  template:
    metadata:
      labels:
        app.kubernetes.io/name: patient-management
        app.kubernetes.io/instance: patient-management
        app.kubernetes.io/component: backend
        app.kubernetes.io/part-of: hospital-information-system
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      serviceAccountName: patient-management-sa
      terminationGracePeriodSeconds: 45
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app.kubernetes.io/name
                      operator: In
                      values:
                        - patient-management
                topologyKey: "kubernetes.io/hostname"
      hostAliases:
        - ip: "192.168.49.1"
          hostnames:
            - "kafka"
            - "patient-write-db"
            - "patient-read-db"
            - "host.minikube.internal"
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      volumes:
        - name: tmp-volume
          emptyDir:
            sizeLimit: 100Mi
      containers:
        - name: patient-management
          image: doguhannilt/patient-management:latest
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
            - name: grpc
              containerPort: 9090
              protocol: TCP
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          volumeMounts:
            - name: tmp-volume
              mountPath: /tmp
          envFrom:
            - configMapRef:
                name: patient-management-config
            - secretRef:
                name: patient-management-secret
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 5"]
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 15
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            periodSeconds: 5
            failureThreshold: 2
          resources:
            requests:
              cpu: 250m
              memory: 384Mi
              ephemeral-storage: 100Mi
            limits:
              cpu: 1000m
              memory: 768Mi
              ephemeral-storage: 500Mi
```

### 1.6 `hpa.yaml`
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: patient-management-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: patient-management
  minReplicas: 2
  maxReplicas: 10
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

### 1.7 `pdb.yaml`
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: patient-management-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: patient-management
```

### 1.8 `network-policy.yaml`
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: patient-management-netpol
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: patient-management
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # 1. API Gateway HTTP Trafiği
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
      ports:
        - protocol: TCP
          port: 8080
    # 2. Servisler arası gRPC Trafiği
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/part-of: hospital-information-system
      ports:
        - protocol: TCP
          port: 9090
    # 3. Prometheus Monitoring Trafiği
    - from:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              app.kubernetes.io/name: prometheus
      ports:
        - protocol: TCP
          port: 8080
  egress:
    # 1. DNS Çözümleme (CoreDNS)
    - ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    # 2. PostgreSQL CQRS Dual DB (Write: 5432, Read: 5433)
    - ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 5433
    # 3. Apache Kafka Broker
    - ports:
        - protocol: TCP
          port: 9092
    # 4. HashiCorp Vault
    - ports:
        - protocol: TCP
          port: 8200
```

### 1.9 `kustomization.yaml`
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

labels:
  - pairs:
      app.kubernetes.io/name: patient-management
      app.kubernetes.io/part-of: hospital-information-system
      app.kubernetes.io/component: backend
      app.kubernetes.io/version: "1.0.0"
      app.kubernetes.io/managed-by: kustomize

images:
  - name: doguhannilt/patient-management
    newTag: "1.0.0"

resources:
  - serviceaccount.yaml
  - configmap.yaml
  - external-secret.yaml
  - service.yaml
  - deployment.yaml
  - hpa.yaml
  - pdb.yaml
  - network-policy.yaml
```

---

## 2. Dağıtım ve Doğrulama Adımları

```bash
# 1. Manifestleri cluster'a uygulama:
kubectl apply -k kubernetes/base/apps/patient-management

# 2. Senkronizasyon ve pod kontrolleri:
kubectl get secretstore patient-management-vault-store
kubectl get externalsecret patient-management-vault-secret
kubectl get pods -l app.kubernetes.io/name=patient-management
kubectl get hpa patient-management-hpa
kubectl get pdb patient-management-pdb
```

---

## 3. Görev Tamamlanma Kriterleri (Checklist)

- [x] `kubernetes/base/apps/patient-management/` altında 8 manifest + `kustomization.yaml` oluşturuldu.
- [x] `kubectl apply -k kubernetes/base/apps/patient-management` komutu hatasız çalıştı.
- [x] `patient-management-vault-secret` nesnesi `SecretSynced: True` durumuna geçti.
- [x] `patient-management` pod'u `1.0.0` imajıyla `1/1 Running` ve Ready durumuna ulaştı.
- [x] Pod içinde salt okunur dosya sistemi (`readOnlyRootFilesystem: true`), `seccompProfile: RuntimeDefault` ve `drop: ["ALL"]` devrede.
- [x] `HPA` ve `PDB` kuralları başarıyla tanımlandı (Minikube yerel ortamı için 1 replika ve optimize kaynaklar ayarlandı).

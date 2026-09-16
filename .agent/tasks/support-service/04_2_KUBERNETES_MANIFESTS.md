# Task 04.2: Kubernetes Manifestlerinin Üretim Standartlarında Yazılması (Manifests Implementation)

Bu doküman, `kubernetes/base/apps/support-service/` dizini altında yer alacak, [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine tam uyumlu tüm Kubernetes manifestolarını ve Kustomize yapılandırmasını içerir.

---

## 1. `configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: support-service-config
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
data:
  SERVER_PORT: "8085"
  SPRING_DATASOURCE_WRITE_JDBC_URL: "jdbc:postgresql://postgres:5432/support_db?currentSchema=support_schema"
  SPRING_DATASOURCE_READ_JDBC_URL: "jdbc:postgresql://postgres:5432/support_db?currentSchema=support_schema"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  SPRING_DATA_REDIS_HOST: "redis"
  SPRING_DATA_REDIS_PORT: "6379"
```

---

## 2. `external-secret.yaml` (SecretStore & ExternalSecret)

```yaml
apiVersion: external-secrets.io/v1
kind: SecretStore
metadata:
  name: support-service-vault-store
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  provider:
    vault:
      server: "http://vault.vault.svc.cluster.local:8200"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "support-service-role"
          serviceAccountRef:
            name: support-service-sa
---
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: support-service-vault-secret
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  refreshInterval: "1h"
  secretStoreRef:
    name: support-service-vault-store
    kind: SecretStore
  target:
    name: support-service-secret
    creationPolicy: Owner
  data:
    - secretKey: SPRING_DATASOURCE_WRITE_USERNAME
      remoteRef:
        key: hospital/support-service/db
        property: username
    - secretKey: SPRING_DATASOURCE_WRITE_PASSWORD
      remoteRef:
        key: hospital/support-service/db
        property: password
    - secretKey: SPRING_DATASOURCE_READ_USERNAME
      remoteRef:
        key: hospital/support-service/db
        property: username
    - secretKey: SPRING_DATASOURCE_READ_PASSWORD
      remoteRef:
        key: hospital/support-service/db
        property: password
    - secretKey: APP_SECRET
      remoteRef:
        key: hospital/shared/jwt
        property: app_secret
    - secretKey: INTERNAL_SERVICE_TOKEN
      remoteRef:
        key: hospital/shared/internal-auth
        property: internal_token
```

---

## 3. `serviceaccount.yaml`

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: support-service-sa
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
automountServiceAccountToken: false
```

---

## 4. `service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: support-service
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: support-service
  ports:
    - name: http
      port: 8085
      targetPort: 8085
      protocol: TCP
```

---

## 5. `deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: support-service
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  # DEV: Reduced to 1 to relieve local Minikube memory pressure (Prod baseline: 2)
  replicas: 1
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
      app.kubernetes.io/name: support-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: support-service
        app.kubernetes.io/instance: support-service
        app.kubernetes.io/component: backend
        app.kubernetes.io/part-of: hospital-information-system
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8085"
    spec:
      serviceAccountName: support-service-sa
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
                        - support-service
                topologyKey: "kubernetes.io/hostname"
      hostAliases:
        - ip: "192.168.49.1"
          hostnames:
            - "postgres"
            - "kafka"
            - "redis"
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
        - name: support-service
          image: doguhannilt/support-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8085
              protocol: TCP
          securityContext:
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
          volumeMounts:
            - name: tmp-volume
              mountPath: /tmp
          envFrom:
            - configMapRef:
                name: support-service-config
            - secretRef:
                name: support-service-secret
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
            # DEV: Optimized for local Minikube memory footprint (Prod baseline: requests 384Mi, limits 768Mi)
            requests:
              cpu: 100m
              memory: 256Mi
              ephemeral-storage: 100Mi
            limits:
              cpu: 500m
              memory: 512Mi
              ephemeral-storage: 500Mi
```

---

## 6. `hpa.yaml`

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: support-service-hpa
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: support-service
  # DEV: Set minReplicas to 1 to reduce local memory footprint (Prod baseline: 2)
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## 7. `pdb.yaml`

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: support-service-pdb
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: support-service
```

---

## 8. `network-policy.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: support-service-netpol
  labels:
    app.kubernetes.io/name: support-service
    app.kubernetes.io/instance: support-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: support-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Allow traffic from API Gateway
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
      ports:
        - protocol: TCP
          port: 8085
  egress:
    # Allow DNS (UDP & TCP)
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
    # Allow PostgreSQL, Kafka, Redis, and Vault
    - ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 9092
        - protocol: TCP
          port: 6379
        - protocol: TCP
          port: 8200
```

---

## 9. `kustomization.yaml`

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - configmap.yaml
  - external-secret.yaml
  - serviceaccount.yaml
  - service.yaml
  - deployment.yaml
  - hpa.yaml
  - pdb.yaml
  - network-policy.yaml
```

---

## 10. Görev Tamamlanma Kriterleri (Checklist)

- [x] `kubernetes/base/apps/support-service/` dizini altında 8 adet manifest ve `kustomization.yaml` dosyası oluşturuldu.
- [x] CHECK_FILE kriterleri (Non-root UID 1000, readOnlyRootFilesystem, emptyDir, startupProbe, liveness/readiness probes, podAntiAffinity, preStop, HPA, PDB, NetworkPolicy) sağlandı.
- [x] `kubectl apply -k kubernetes/base/apps/support-service` komutu ile pod `1/1 Running` ve `SecretSynced: True` olarak ayağa kaldırıldı.

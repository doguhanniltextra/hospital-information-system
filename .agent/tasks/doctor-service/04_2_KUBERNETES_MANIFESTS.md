# Task 04.2: Kubernetes Manifestlerinin Üretim Standartlarında Yazılması (Manifests Implementation)

Bu doküman, `kubernetes/base/apps/doctor-service/` dizini altında yer alacak, [CHECK_FILE.md](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/check/CHECK_FILE.md) üretim kriterlerine tam uyumlu tüm Kubernetes manifestolarını ve Kustomize yapılandırmasını içerir.

---

## 1. `configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: doctor-service-config
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
data:
  SERVER_PORT: "8083"
  DOCTOR_SERVICE_GRPC_PORT: "9005"
  SPRING_DATASOURCE_WRITE_URL: "jdbc:postgresql://postgres:5432/doctor_db?currentSchema=doctor_schema"
  SPRING_DATASOURCE_READ_URL: "jdbc:postgresql://postgres:5432/doctor_db?currentSchema=doctor_schema"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  KAFKA_TOPIC_LAB_ORDER_PLACED: "lab-order-placed.v1"
  KAFKA_TOPIC_LAB_RESULT_COMPLETED: "lab-result-completed.v1"
  KAFKA_GROUP_DOCTOR_LAB_RESULT: "doctor-lab-result-group"
```

---

## 2. `secret.yaml` (ExternalSecret)

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: doctor-service-secrets
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  refreshInterval: "1h"
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: doctor-service-secret
    creationPolicy: Owner
  data:
    - secretKey: SPRING_DATASOURCE_USERNAME
      remoteRef:
        key: secret/data/hospital/doctor-service/db
        property: username
    - secretKey: SPRING_DATASOURCE_PASSWORD
      remoteRef:
        key: secret/data/hospital/doctor-service/db
        property: password
    - secretKey: APP_SECRET
      remoteRef:
        key: secret/data/hospital/shared/jwt
        property: app_secret
    - secretKey: INTERNAL_SERVICE_TOKEN
      remoteRef:
        key: secret/data/hospital/shared/internal-auth
        property: internal_token
```

---

## 3. `serviceaccount.yaml`

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: doctor-service-sa
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
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
  name: doctor-service
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: doctor-service
  ports:
    - name: http
      port: 8083
      targetPort: 8083
      protocol: TCP
    - name: grpc
      port: 9005
      targetPort: 9005
      protocol: TCP
```

---

## 5. `deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: doctor-service
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
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
      app.kubernetes.io/name: doctor-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: doctor-service
        app.kubernetes.io/instance: doctor-service
        app.kubernetes.io/component: backend
        app.kubernetes.io/part-of: hospital-information-system
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8083"
    spec:
      serviceAccountName: doctor-service-sa
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
                        - doctor-service
                topologyKey: "kubernetes.io/hostname"
      hostAliases:
        - ip: "192.168.49.1"
          hostnames:
            - "postgres"
            - "kafka"
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
        - name: doctor-service
          image: doguhannilt/doctor-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8083
              protocol: TCP
            - name: grpc
              containerPort: 9005
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
                name: doctor-service-config
            - secretRef:
                name: doctor-service-secret
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
  name: doctor-service-hpa
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: doctor-service
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
  name: doctor-service-pdb
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: doctor-service
```

---

## 8. `networkpolicy.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: doctor-service-netpol
  labels:
    app.kubernetes.io/name: doctor-service
    app.kubernetes.io/instance: doctor-service
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: hospital-information-system
    app.kubernetes.io/managed-by: kustomize
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: doctor-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Allow traffic from API Gateway, Appointment Service, Admission Service
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: appointment-service
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: admission-service
      ports:
        - protocol: TCP
          port: 8083
        - protocol: TCP
          port: 9005
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
    # Allow PostgreSQL, Kafka, and Vault
    - ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 9092
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
  - secret.yaml
  - serviceaccount.yaml
  - service.yaml
  - deployment.yaml
  - hpa.yaml
  - pdb.yaml
  - networkpolicy.yaml
```

---

## 10. Görev Tamamlanma Kriterleri (Checklist)

- [x] `kubernetes/base/apps/doctor-service/` dizini altında 8 adet manifest ve `kustomization.yaml` dosyası oluşturuldu.
- [x] CHECK_FILE kriterleri (Non-root, readOnlyRootFilesystem, emptyDir, startupProbe, liveness/readiness probes, podAntiAffinity, preStop, HPA, PDB, NetworkPolicy) sağlandı.
- [x] `kubectl apply -k kubernetes/base/apps/doctor-service` komutu ile pod `1/1 Running` ve `SecretSynced: True` olarak ayağa kaldırıldı.

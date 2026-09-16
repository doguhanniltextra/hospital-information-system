# API Gateway Kubernetes Hack and Testing Guide

This guide provides operational and testing commands to inspect, verify, and debug the `api-gateway` running inside Kubernetes.

---

## 1. Port-Forwarding (Local Access)

To route traffic from `localhost:4004` to the in-cluster API Gateway service:

```bash
kubectl port-forward svc/api-gateway 4004:4004
```

Keep this process running in a separate terminal while executing the curl commands below.

---

## 2. Health and Probe Checks

### 2.1 Liveness Probe (Pod Deadlock Check)
Verifies that the gateway process is running and not locked up:
```bash
curl -i http://localhost:4004/actuator/health/liveness
```
* Expected Response: `HTTP/1.1 200 OK` with `{"status":"UP"}`

---

### 2.2 Readiness Probe (Traffic Acceptance Check)
Verifies that the gateway is ready to accept incoming traffic:
```bash
curl -i http://localhost:4004/actuator/health/readiness
```
* Expected Response: `HTTP/1.1 200 OK` with `{"status":"UP"}`

---

### 2.3 Aggregate Health Check (Includes Backing Services)
Displays the health status of all registered components (Disk, Redis, etc.):
```bash
curl -i http://localhost:4004/actuator/health
```
Note: If Redis is unreachable, this returns `HTTP/1.1 503 Service Unavailable` with `redis: DOWN`. The pod remains in `Ready` state because Kubernetes liveness and readiness probes use dedicated endpoints.

---

### 2.4 Prometheus Metrics
Scrapes Micrometer and Prometheus runtime metrics:
```bash
curl -s http://localhost:4004/actuator/prometheus | head -n 30
```

---

## 3. Security and Authentication Verification

### 3.1 Unauthorized Access Test (401 Unauthorized)
Requesting a protected resource without a Bearer token:
```bash
curl -i http://localhost:4004/api/patients
```
* Expected Response: `HTTP/1.1 401 Unauthorized`

---

### 3.2 Permitted Endpoint Test
Authentication endpoints bypass the JWT filter and route directly to downstream services:
```bash
curl -i http://localhost:4004/api/auth/login
```
Note: Returns downstream status (e.g., 500 or 503 if `auth-service` is not yet running), confirming the security filter allowed the request through without demanding a token.

---

### 3.3 Authenticated Request with JWT
```bash
curl -i -H "Authorization: Bearer <VALID_JWT_TOKEN>" http://localhost:4004/api/patients
```

---

## 4. Rate Limiting Verification

The gateway enforces brute-force protection on `/api/auth/**` (burstCapacity: 10, replenishRate: 5).

Execute 15 sequential requests with a spoofed client IP to trigger rate limiting:

```bash
for i in {1..15}; do
  echo -n "Request $i: "
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "X-Forwarded-For: 203.0.113.195" \
    http://localhost:4004/api/auth/login
done
```
* Expected Result: The first 10 requests return downstream responses; subsequent requests return `429` (Too Many Requests).

---

## 5. In-Pod Diagnostics (Without Port-Forwarding)

Query the endpoints directly inside the running container:

```bash
# Obtain an active pod name
POD_NAME=$(kubectl get pods -l app.kubernetes.io/name=api-gateway -o jsonpath="{.items[0].metadata.name}")

# Test liveness probe from inside the pod
kubectl exec -it $POD_NAME -- wget -qO- http://localhost:4004/actuator/health/liveness

# Test readiness probe from inside the pod
kubectl exec -it $POD_NAME -- wget -qO- http://localhost:4004/actuator/health/readiness
```

---

## 6. Real-Time Observability Commands

```bash
# Stream logs across all api-gateway pods
kubectl logs -l app.kubernetes.io/name=api-gateway -f --max-log-requests=10

# Watch pod status transitions and restart counts
kubectl get pods -l app.kubernetes.io/name=api-gateway -w

# Inspect Horizontal Pod Autoscaler metrics
kubectl get hpa api-gateway-hpa

# Describe deployment events and probe history
kubectl describe deployment api-gateway
```

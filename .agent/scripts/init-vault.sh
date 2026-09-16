#!/usr/bin/env bash
# ==============================================================================
# Hospital Information System - Master HashiCorp Vault Initializer
# Populates all KV-v2 Secrets, ACL Policies, and Kubernetes Auth Roles
# ==============================================================================
set -euo pipefail

echo "================================================================="
echo "🔑 Initializing HashiCorp Vault Secrets, Policies & Roles..."
echo "================================================================="

kubectl exec -i vault-0 -n vault -- sh -c "
export VAULT_TOKEN=root

# 1. Enable KV v2 engine if not already enabled
vault secrets enable -path=secret kv-v2 2>/dev/null || true

# 2. Enable Kubernetes auth if not enabled
vault auth enable kubernetes 2>/dev/null || true
vault write auth/kubernetes/config \
    kubernetes_host=\"https://kubernetes.default.svc:443\" 2>/dev/null || true

# -------------------------------------------------------------
# 3. Write Shared Secrets
# -------------------------------------------------------------
vault kv put secret/hospital/shared/jwt app_secret=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong
vault kv put secret/hospital/shared/internal-auth internal_token=pm-internal-token

# -------------------------------------------------------------
# 4. Write Microservice Specific Secrets
# -------------------------------------------------------------
# auth-service
vault kv put secret/hospital/auth-service/db username=auth_user password=auth_pass_123
vault kv put secret/hospital/auth-service/api-keys internal_api_key=auth-internal-key-123

# patient-management
vault kv put secret/hospital/patient-management/db username=patient_user password=patient_pass_123
vault kv put secret/hospital/patient-management/security encryption_key=2b7e151628aed2a6abf7158809cf4f3c

# doctor-service
vault kv put secret/hospital/doctor-service/db username=doctor_user password=doctor_pass_123

# appointment-service
vault kv put secret/hospital/appointment-service/db username=appointment_user password=appointment_pass_123

# admission-service
vault kv put secret/hospital/admission-service/db username=admission_user password=admission_pass_123

# support-service
vault kv put secret/hospital/support-service/db username=support_user password=support_pass_123

# billing-service
vault kv put secret/hospital/billing-service/db username=billing_user password=billing_pass_123
vault kv put secret/hospital/billing-service/api-keys invoice_api_key=invoice_secret_api_key_test_123

# notification-service
vault kv put secret/hospital/notification-service/db username=notification_user password=notification_pass_123
vault kv put secret/hospital/notification-service/api-keys ops_alert_emails=alerts@hospital.com,devops@hospital.com

# -------------------------------------------------------------
# 5. Create ACL Policies & Kubernetes Auth Roles for all 9 Services
# -------------------------------------------------------------

# API Gateway
vault policy write api-gateway-policy - <<EOF
path \"secret/data/hospital/api-gateway/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/api-gateway-role \
    bound_service_account_names=api-gateway-sa \
    bound_service_account_namespaces=default \
    policies=api-gateway-policy \
    ttl=24h

# Auth Service
vault policy write auth-service-policy - <<EOF
path \"secret/data/hospital/auth-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/auth-service-role \
    bound_service_account_names=auth-service-sa \
    bound_service_account_namespaces=default \
    policies=auth-service-policy \
    ttl=24h

# Patient Management
vault policy write patient-management-policy - <<EOF
path \"secret/data/hospital/patient-management/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/patient-management-role \
    bound_service_account_names=patient-management-sa \
    bound_service_account_namespaces=default \
    policies=patient-management-policy \
    ttl=24h

# Doctor Service
vault policy write doctor-service-policy - <<EOF
path \"secret/data/hospital/doctor-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/doctor-service-role \
    bound_service_account_names=doctor-service-sa \
    bound_service_account_namespaces=default \
    policies=doctor-service-policy \
    ttl=24h

# Appointment Service
vault policy write appointment-service-policy - <<EOF
path \"secret/data/hospital/appointment-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/appointment-service-role \
    bound_service_account_names=appointment-service-sa \
    bound_service_account_namespaces=default \
    policies=appointment-service-policy \
    ttl=24h

# Admission Service
vault policy write admission-service-policy - <<EOF
path \"secret/data/hospital/admission-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/admission-service-role \
    bound_service_account_names=admission-service-sa \
    bound_service_account_namespaces=default \
    policies=admission-service-policy \
    ttl=24h

# Support Service
vault policy write support-service-policy - <<EOF
path \"secret/data/hospital/support-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/support-service-role \
    bound_service_account_names=support-service-sa \
    bound_service_account_namespaces=default \
    policies=support-service-policy \
    ttl=24h

# Billing Service
vault policy write billing-service-policy - <<EOF
path \"secret/data/hospital/billing-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/billing-service-role \
    bound_service_account_names=billing-service-sa \
    bound_service_account_namespaces=default \
    policies=billing-service-policy \
    ttl=24h

# Notification Service
vault policy write notification-service-policy - <<EOF
path \"secret/data/hospital/notification-service/*\" { capabilities = [\"read\"] }
path \"secret/data/hospital/shared/*\" { capabilities = [\"read\"] }
EOF
vault write auth/kubernetes/role/notification-service-role \
    bound_service_account_names=notification-service-sa \
    bound_service_account_namespaces=default \
    policies=notification-service-policy \
    ttl=24h
"

echo "✅ All HashiCorp Vault secrets, policies, and Kubernetes roles successfully configured!"

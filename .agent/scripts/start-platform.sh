#!/usr/bin/env bash
# ==============================================================================
# Hospital Information System - Reliable Platform Starter
# Starts Host Dependencies (PostgreSQL, Kafka, Redis), Minikube, and Vault
# ==============================================================================
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo -e "${CYAN}${BOLD}"
echo "================================================================================"
echo "🚀  HOSPITAL INFORMATION SYSTEM - STARTING PLATFORM"
echo "================================================================================"
echo -e "${NC}"

# 1. Start Stateful Containers
echo -e "${YELLOW}1. Host Stateful Bileşenleri Başlatılıyor (PostgreSQL, Kafka, Redis)...${NC}"
docker start his-postgres kafka redis 2>/dev/null || true

# Wait for PostgreSQL to accept connections
echo -e "   ⏳ PostgreSQL'in hazır olması bekleniyor..."
until docker exec -i his-postgres pg_isready -U postgres >/dev/null 2>&1; do
    sleep 1
done
echo -e "   ${GREEN}✔ PostgreSQL hazır.${NC}"

# Wait for Kafka to be ready
echo -e "   ⏳ Kafka'nın hazır olması bekleniyor..."
until docker exec -i kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
    sleep 1
done
echo -e "   ${GREEN}✔ Kafka hazır.${NC}"

# 2. Start Minikube with Optimized Memory
echo -e "\n${YELLOW}2. Minikube Başlatılıyor (4GB RAM, 4 CPU)...${NC}"
minikube start --memory=4096m --cpus=4

# 3. Wait for Vault & ESO Pods
echo -e "\n${YELLOW}3. Vault ve External Secrets Operator Kontrol Ediliyor...${NC}"
kubectl wait --for=condition=Ready pod/vault-0 -n vault --timeout=60s 2>/dev/null || true
kubectl wait --for=condition=Ready pod -l app.kubernetes.io/name=external-secrets -n external-secrets --timeout=60s 2>/dev/null || true

# 4. Initialize Vault Secrets, Policies and Roles
echo -e "\n${YELLOW}4. HashiCorp Vault Secrets, Policies & Roles Yükleniyor...${NC}"
"${ROOT_DIR}/.agent/scripts/init-vault.sh"

# 5. Ensure Exactly 1 Replica Per Microservice
echo -e "\n${YELLOW}5. Tüm mikroservisler 1 replica olarak ayarlanıyor...${NC}"
SERVICES=("admission-service" "api-gateway" "appointment-service" "auth-service" "billing-service" "doctor-service" "notification-service" "patient-management" "support-service")

for svc in "${SERVICES[@]}"; do
    kubectl scale deployment "${svc}" --replicas=1 2>/dev/null || true
done

echo -e "\n${GREEN}${BOLD}✅ Tüm altyapı ve mikroservisler başarıyla başlatıldı!${NC}"
echo -e "Doğrulama ve kanıt raporunu çalıştırmak için: ${CYAN}.agent/scripts/verify-platform.sh${NC}\n"

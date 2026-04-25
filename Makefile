# Patient Management System - Makefile
# Optimized for WSL/Linux and Windows compatibility

# Force Bash shell for consistency across environments
SHELL := /bin/bash

# Variables
DOCKER := docker

# Detect 'docker compose' vs 'docker-compose'
DOCKER_COMPOSE := $(shell docker compose version >/dev/null 2>&1 && echo "docker compose" || echo "docker-compose")

# Multi-compose Configuration
# We aggregate all bounded contexts to maintain the global orchestration capability of the Makefile
COMPOSE_INFRA  := -f infrastructure/docker-compose.yml
COMPOSE_DOMAIN := -f patient-management/docker-compose.yml \
                  -f auth-service/docker-compose.yml \
                  -f doctor-service/docker-compose.yml \
                  -f appointment-service/docker-compose.yml \
                  -f billing-service/docker-compose.yml \
                  -f support-service/docker-compose.yml \
                  -f admission-service/docker-compose.yml \
                  -f notification-service/docker-compose.yml

COMPOSE_ALL := $(COMPOSE_INFRA) $(COMPOSE_DOMAIN)

SERVICES := api-gateway patient-management auth-service doctor-service appointment-service billing-service support-service admission-service notification-service

.PHONY: help
help:
	@echo "╔══════════════════════════════════════════════════════════╗"
	@echo "║  Patient Management System - Development Commands        ║"
	@echo "╚══════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "📦 LOCAL DEVELOPMENT (Docker Compose):"
	@echo "  make dev-up                - Start all services locally with Docker Compose"
	@echo "  make dev-down              - Stop all local services"
	@echo "  make dev-logs              - View logs from all services"
	@echo "  make dev-logs-service SVC  - View logs from specific service (e.g., SVC=patient-management)"
	@echo "  make dev-build             - Build all Docker images locally"
	@echo "  make dev-rebuild           - Rebuild all Docker images from scratch"
	@echo ""
	@echo "🏗️  BUILD COMMANDS:"
	@echo "  make build-all             - Build all services"
	@echo "  make build-service SVC     - Build specific service"
	@echo ""
	@echo "🧹 CLEANUP:"
	@echo "  make clean-local           - Remove all local Docker containers and volumes"
	@echo ""

# ============================================================================
# LOCAL DEVELOPMENT WITH DOCKER COMPOSE
# ============================================================================

.PHONY: dev-up
dev-up:
	@echo "Starting all services with Docker Compose..."
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) up -d
	@echo "Services started!"
	@echo "Services available at:"
	@echo "   API Gateway:       http://localhost:4004"
	@echo ""

.PHONY: dev-down
dev-down:
	@echo "Stopping all services..."
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) down
	@echo "Services stopped!"

.PHONY: dev-logs
dev-logs:
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) logs -f

.PHONY: dev-logs-service
dev-logs-service:
	@if [ -z "$(SVC)" ]; then \
		echo "Error: Please specify SVC=<service-name>"; \
		echo "   Example: make dev-logs-service SVC=patient-management"; \
		exit 1; \
	fi
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) logs -f $(SVC)

.PHONY: dev-build
dev-build:
	@echo "Building Docker images..."
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) build
	@echo "Build complete!"

.PHONY: dev-rebuild
dev-rebuild:
	@echo "Rebuilding Docker images (no cache)..."
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) build --no-cache
	@echo " Rebuild complete!"

# ============================================================================
# BUILD COMMANDS
# ============================================================================

.PHONY: build-all
build-all:
	@echo " Building all services with Maven..."
	@for svc in $(SERVICES); do \
		echo "----------------------------------------------------------------"; \
		echo "Building $$svc..."; \
		(cd $$svc && mvn clean package -DskipTests) || exit 1; \
	done
	@echo " All services built!"

.PHONY: build-service
build-service:
	@if [ -z "$(SVC)" ]; then \
		echo "Error: Please specify SVC=<service-directory>"; \
		echo "   Example: make build-service SVC=auth-service"; \
		exit 1; \
	fi
	@echo "Building $(SVC)..."
	cd $(SVC) && mvn clean package -DskipTests
	@echo " $(SVC) built!"

.PHONY: test-all
test-all:
	@echo " Running all microservice tests from the Maven aggregator..."
	./mvnw test
	@echo " All microservice tests finished!"

.PHONY: test-e2e
test-e2e:
	@echo " Running API/E2E tests from scripts/tests..."
	cd scripts/tests && ../../mvnw test
	@echo " API/E2E tests finished!"

.PHONY: test-service
test-service:
	@if [ -z "$(SVC)" ]; then \
		echo "Error: Please specify SVC=<service-directory>"; \
		echo "   Example: make test-service SVC=auth-service"; \
		exit 1; \
	fi
	@echo "Running tests for $(SVC)..."
	cd $(SVC) && mvn test
	@echo " $(SVC) tests finished!"

# ============================================================================
# CLEANUP
# ============================================================================

.PHONY: clean-local
clean-local:
	@echo "Cleaning up Docker resources..."
	$(DOCKER_COMPOSE) $(COMPOSE_ALL) down -v
	$(DOCKER) system prune -f
	@echo " Local cleanup complete!"

.PHONY: clean-all
clean-all: clean-local
	@echo " Complete cleanup done!"

# ============================================================================
# UTILITY COMMANDS
# ============================================================================

.PHONY: ping
ping:
	@echo "Pong!"

.PHONY: status
status:
	@echo "Checking services status..."
	@if command -v $(DOCKER) > /dev/null 2>&1; then \
		echo -n "Docker: "; \
		$(DOCKER) ps -q | wc -l | tr -d ' ' | xargs echo -n; \
		echo " containers running"; \
	else \
		echo "Docker: Not found"; \
	fi

.PHONY: info
info:
	@echo "Project Information:"
	@echo "   Services:  $(SERVICES)"

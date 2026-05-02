SHELL := /bin/sh

COMPOSE := docker compose

ENV_DIRS := infrastructure api-gateway auth-service patient-management doctor-service appointment-service support-service admission-service billing-service notification-service
APP_SERVICES := patient-management doctor-service appointment-service support-service admission-service billing-service notification-service
CORE_SERVICES := api-gateway auth-service
COMPOSE_DIRS := infrastructure $(CORE_SERVICES) $(APP_SERVICES)

.DEFAULT_GOAL := help

.PHONY: help setup up infra-up core-up services-up smoke health ps logs down reset

help:
	@echo "Hospital Information System local commands"
	@echo ""
	@echo "  make setup              Create missing .env files from .env.example"
	@echo "  make up                 Start infrastructure, gateway, auth, and all services"
	@echo "  make infra-up           Start Kafka, Redis, MongoDB, Prometheus, Grafana"
	@echo "  make core-up            Start API Gateway and Auth Service"
	@echo "  make services-up        Start domain/background services"
	@echo "  make smoke              Get a dev JWT and call appointment-service through gateway"
	@echo "  make health             Check public actuator health endpoints"
	@echo "  make ps                 Show Docker Compose status for every compose file"
	@echo "  make logs SERVICE=name  Follow logs for one compose directory, e.g. SERVICE=appointment-service"
	@echo "  make down               Stop containers without deleting volumes"
	@echo "  make reset              Stop containers and delete local volumes"

setup:
	@for d in $(ENV_DIRS); do \
		if [ ! -f "$$d/.env" ]; then \
			cp "$$d/.env.example" "$$d/.env"; \
			echo "created $$d/.env"; \
		else \
			echo "exists  $$d/.env"; \
		fi; \
	done

up: setup infra-up core-up services-up

infra-up:
	$(COMPOSE) -f infrastructure/docker-compose.yml up -d

core-up:
	$(COMPOSE) -f api-gateway/docker-compose.yml up -d
	$(COMPOSE) -f auth-service/docker-compose.yml up -d

services-up:
	@for d in $(APP_SERVICES); do \
		echo "starting $$d"; \
		$(COMPOSE) -f "$$d/docker-compose.yml" up -d; \
	done

smoke:
	@echo "Requesting development JWT from auth-service via the API Gateway..."
	@TOKEN=$$(curl -fsS "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST"); \
	echo "Calling appointment-service through the API Gateway..."; \
	curl -i -s "http://localhost:4004/api/appointments/get" \
		-H "Authorization: Bearer $$TOKEN"

health:
	@for endpoint in \
		"http://localhost:4004/actuator/health" \
		"http://localhost:8089/actuator/health" \
		"http://localhost:8080/actuator/health" \
		"http://localhost:8083/actuator/health" \
		"http://localhost:8084/actuator/health" \
		"http://localhost:8085/actuator/health" \
		"http://localhost:8086/actuator/health" \
		"http://localhost:8081/actuator/health" \
		"http://localhost:8090/actuator/health"; do \
		printf "%-45s " "$$endpoint"; \
		curl -fsS "$$endpoint" >/dev/null && echo "OK" || echo "NOT READY"; \
	done

ps:
	@for d in $(COMPOSE_DIRS); do \
		echo ""; \
		echo "== $$d =="; \
		$(COMPOSE) -f "$$d/docker-compose.yml" ps; \
	done

logs:
	@if [ -z "$(SERVICE)" ]; then \
		echo "Usage: make logs SERVICE=appointment-service"; \
		exit 1; \
	fi
	$(COMPOSE) -f "$(SERVICE)/docker-compose.yml" logs -f --tail=200

down:
	@for d in notification-service billing-service admission-service support-service appointment-service doctor-service patient-management auth-service api-gateway infrastructure; do \
		echo "stopping $$d"; \
		$(COMPOSE) -f "$$d/docker-compose.yml" down --remove-orphans; \
	done

reset:
	@for d in notification-service billing-service admission-service support-service appointment-service doctor-service patient-management auth-service api-gateway infrastructure; do \
		echo "removing $$d containers and volumes"; \
		$(COMPOSE) -f "$$d/docker-compose.yml" down -v --remove-orphans; \
	done

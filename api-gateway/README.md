# API Gateway

## Description
The API Gateway serves as the centralized edge node and single entry point for all external client requests entering the microservice architecture.

## Functionality
- Routes incoming HTTP requests to the appropriate downstream backend microservices.
- Acts as a reverse proxy, hiding the internal network topology from external clients.
- Manages Cross-Origin Resource Sharing (CORS) configurations globally.
- Implemented using Spring Cloud Gateway, providing a non-blocking, reactive request processing pipeline.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- Downstream services must be reachable via the network

### Environment Variables
- Target routing URIs (e.g., `AUTH_SERVICE_URL`, `PATIENT_SERVICE_URL`, etc., as configured in `application.yml`)

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd api-gateway
mvn spring-boot:run
```
The gateway will start on port `4004` by default and begin routing traffic according to its configured predicates.

### Running via Docker
```bash
docker build -t api-gateway .
docker run -p 4004:4004 --env-file .env api-gateway
```

### Try it yourself (in < 60 seconds)
Run these commands from the api-gateway directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

```bash
# 0. Prepare local env files (first run only)
# Linux/macOS/Git Bash:
[ -f ../infrastructure/.env ] || cp ../infrastructure/.env.example ../infrastructure/.env
[ -f .env ] || cp .env.example .env
# Windows PowerShell:
# if (!(Test-Path ..\infrastructure\.env)) { Copy-Item ..\infrastructure\.env.example ..\infrastructure\.env }
# if (!(Test-Path .env)) { Copy-Item .env.example .env }

# 1. Start global infrastructure (Kafka, Redis, etc.)
docker compose -f ../infrastructure/docker-compose.yml up -d

# 2. Start this microservice
docker compose up -d

# 3. Verify it's running (Wait ~10 seconds for Spring Boot to start)
# Linux/macOS/Git Bash:
curl -s http://localhost:4004/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:4004/actuator/health
```
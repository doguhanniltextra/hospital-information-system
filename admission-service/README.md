# Admission Service

## Description
The Admission Service manages the inpatient lifecycle, specifically tracking hospital admissions, discharges, and physical bed allocations.

## Functionality
- Manages real-time hospital bed availability and assignment.
- Processes inpatient admission and discharge workflows.
- Implements Command Query Responsibility Segregation (CQRS) utilizing dual PostgreSQL data sources.
- Publishes state transition events to Kafka, enabling downstream services (e.g., Billing and Doctor services) to react to admissions asynchronously.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database instances (Write DB and Read DB)
- Kafka cluster active

### Environment Variables
- `SPRING_DATASOURCE_WRITE_URL`
- `SPRING_DATASOURCE_READ_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd admission-service
mvn spring-boot:run
```
The service will start on port `8086` by default.

### Running via Docker
```bash
docker build -t admission-service .
docker run -p 8086:8086 --env-file .env admission-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the admission-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

```bash
# 0. Prepare local env files (first run only)
# Linux/macOS/Git Bash:
[ -f ../infrastructure/.env ] || cp ../infrastructure/.env.example ../infrastructure/.env
[ -f ../api-gateway/.env ] || cp ../api-gateway/.env.example ../api-gateway/.env
[ -f ../auth-service/.env ] || cp ../auth-service/.env.example ../auth-service/.env
[ -f .env ] || cp .env.example .env
# Windows PowerShell:
# if (!(Test-Path ..\infrastructure\.env)) { Copy-Item ..\infrastructure\.env.example ..\infrastructure\.env }
# if (!(Test-Path ..\api-gateway\.env)) { Copy-Item ..\api-gateway\.env.example ..\api-gateway\.env }
# if (!(Test-Path ..\auth-service\.env)) { Copy-Item ..\auth-service\.env.example ..\auth-service\.env }
# if (!(Test-Path .env)) { Copy-Item .env.example .env }

# 1. Start global infrastructure (Kafka, Redis, etc.)
docker compose -f ../infrastructure/docker-compose.yml up -d

# 2. Start API Gateway and Auth Service (needed for a development JWT)
docker compose -f ../api-gateway/docker-compose.yml up -d
docker compose -f ../auth-service/docker-compose.yml up -d

# 3. Start this microservice and its database
docker compose up -d

# 4. Wait until Spring Boot is ready, then verify health
# Linux/macOS/Git Bash:
curl -s http://localhost:8086/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8086/actuator/health

# 5. Get a development JWT with the role required by this endpoint
# Linux/macOS/Git Bash:
TOKEN=$(curl -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST")
# Windows PowerShell:
# $TOKEN = curl.exe -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST"

# 6. Make an authenticated request through the API Gateway
# Linux/macOS/Git Bash:
curl -i -s http://localhost:4004/api/admissions/active \
  -H "Authorization: Bearer $TOKEN"
# Windows PowerShell:
# curl.exe -i -s http://localhost:4004/api/admissions/active `
#   -H "Authorization: Bearer $TOKEN"
```
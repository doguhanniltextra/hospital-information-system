# Support Service

## Description
The Support Service encapsulates ancillary hospital operations, specifically combining Laboratory operations and Inventory/Stock management.

## Functionality
- Manages the hospital laboratory catalog and reference data.
- Processes incoming lab test orders and publishes test results.
- Tracks hospital inventory items, stock levels, and consumption.
- Implements Command Query Responsibility Segregation (CQRS) utilizing PostgreSQL data sources.
- Employs Redis for caching frequently accessed reference data to reduce database load.
- Communicates asynchronously via Kafka for lab order placement and result dissemination.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database instances (Write DB and Read DB)
- Redis instance active
- Kafka cluster active

### Environment Variables
- `SPRING_DATASOURCE_URL` (mapped to read/write properties)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd support-service
mvn spring-boot:run
```
The service will start on port `8085` by default.

### Running via Docker
```bash
docker build -t support-service .
docker run -p 8085:8085 --env-file .env support-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the support-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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
curl -s http://localhost:8085/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8085/actuator/health

# 5. Get a development JWT with the role required by this endpoint
# Linux/macOS/Git Bash:
TOKEN=$(curl -s "http://localhost:4004/api/auth/dev/token/raw?role=PATIENT")
# Windows PowerShell:
# $TOKEN = curl.exe -s "http://localhost:4004/api/auth/dev/token/raw?role=PATIENT"

# 6. Make an authenticated request through the API Gateway
# Linux/macOS/Git Bash:
curl -i -s http://localhost:4004/api/labs/catalog \
  -H "Authorization: Bearer $TOKEN"
# Windows PowerShell:
# curl.exe -i -s http://localhost:4004/api/labs/catalog `
#   -H "Authorization: Bearer $TOKEN"
```
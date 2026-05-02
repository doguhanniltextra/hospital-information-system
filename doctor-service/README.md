# Doctor Service

## Description
The Doctor Service is a domain-specific microservice responsible for managing medical staff profiles, specializations, schedules, and patient capacity limits.

## Functionality
- Maintains records of doctors, their clinical specializations, and working shifts.
- Validates capacity constraints (e.g., maximum patient limit per doctor) during admission and appointment workflows.
- Implements CQRS architecture using distinct read and write PostgreSQL data sources.
- Exposes synchronous gRPC endpoints for availability queries and validation.
- Consumes Kafka events corresponding to patient admissions to asynchronously update active patient counts.

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
cd doctor-service
mvn spring-boot:run
```
The REST API will bind to port `8083` and the gRPC server will bind to port `9005` by default.

### Running via Docker
```bash
docker build -t doctor-service .
docker run -p 8083:8083 -p 9005:9005 --env-file .env doctor-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the doctor-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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
curl -s http://localhost:8083/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8083/actuator/health

# 5. Get a development JWT with the role required by this endpoint
# Linux/macOS/Git Bash:
TOKEN=$(curl -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST")
# Windows PowerShell:
# $TOKEN = curl.exe -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST"

# 6. Make an authenticated request through the API Gateway
# Linux/macOS/Git Bash:
curl -i -s http://localhost:4004/api/doctors \
  -H "Authorization: Bearer $TOKEN"
# Windows PowerShell:
# curl.exe -i -s http://localhost:4004/api/doctors `
#   -H "Authorization: Bearer $TOKEN"
```
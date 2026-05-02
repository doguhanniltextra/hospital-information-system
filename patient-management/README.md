# Patient Service

## Description
The Patient Service manages the primary medical demographic and historical records for all patients registered in the hospital system.

## Functionality
- Manages patient creation, updates, and retrieval operations.
- Implements Command Query Responsibility Segregation (CQRS) with dual PostgreSQL datasources (read/write separation) to optimize read-heavy workloads.
- Exposes synchronous gRPC endpoints for inter-service queries (e.g., used by Appointment Service).
- Publishes patient data mutation events to Kafka via the Transactional Outbox pattern.
- Consumes asynchronous lab result events from Kafka to update the patient's medical history.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database instances (Write DB and Read DB)
- Kafka cluster active

### Environment Variables

For Docker Compose, create `patient-management/.env` from the checked-in example:

```bash
# Linux/macOS/Git Bash
cp .env.example .env

# Windows PowerShell
# Copy-Item .env.example .env
```

```env
POSTGRES_DB=patient_db
POSTGRES_USER=patient_user
POSTGRES_PASSWORD=password_here
APP_SECRET=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong
```

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: credentials used by the patient write/read PostgreSQL containers.
- `APP_SECRET`: JWT signing key used to validate Bearer tokens. It must match `auth-service` and `api-gateway`.

Docker Compose injects the datasource configuration through `SPRING_APPLICATION_JSON`. If you run with `mvn spring-boot:run` outside Docker, configure equivalent write/read datasource properties and `KAFKA_BOOTSTRAP_SERVERS` for your local environment.

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd patient-management
mvn spring-boot:run
```
The REST API will bind to port `8080` and the gRPC server will bind to port `9002` by default.

### Running via Docker
```bash
docker build -t patient-service .
docker run -p 8080:8080 -p 9002:9002 --env-file .env patient-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the patient-management directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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
curl -s http://localhost:8080/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8080/actuator/health

# 5. Get a development JWT with the role required by this endpoint
# Linux/macOS/Git Bash:
TOKEN=$(curl -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST")
# Windows PowerShell:
# $TOKEN = curl.exe -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST"

# 6. Make an authenticated request through the API Gateway
# Linux/macOS/Git Bash:
curl -i -s http://localhost:4004/api/patients \
  -H "Authorization: Bearer $TOKEN"
# Windows PowerShell:
# curl.exe -i -s http://localhost:4004/api/patients `
#   -H "Authorization: Bearer $TOKEN"
```
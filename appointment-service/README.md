# Appointment Service

## Description

The Appointment Service is a core microservice that handles the scheduling lifecycle of medical appointments within the distributed architecture.

## Functionality

- Orchestrates complex distributed transactions for appointment creation using the Saga pattern.
- Makes synchronous gRPC calls to the Patient Service and Doctor Service to validate identities and check availability constraints.
- Persists appointment state in PostgreSQL.
- Implements the Transactional Outbox pattern to guarantee at-least-once delivery of appointment lifecycle events.
- Publishes state changes to Kafka topics for consumption by Billing and Notification services.

## Execution

### Prerequisites

- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database active and accessible
- Kafka cluster active
- Patient Service and Doctor Service must be reachable via gRPC

### Environment Variables

For Docker Compose, create `appointment-service/.env` from the checked-in example:

```bash
# Linux/macOS/Git Bash
cp .env.example .env

# Windows PowerShell
# Copy-Item .env.example .env
```

```env
POSTGRES_DB=appointment_db
POSTGRES_USER=appointment_user
POSTGRES_PASSWORD=password_here
APP_SECRET=mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong
```

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: credentials used by the appointment write/read PostgreSQL containers.
- `APP_SECRET`: JWT signing key used to validate Bearer tokens. It must match `auth-service` and `api-gateway`.

Docker Compose fills the Spring datasource URLs for you. If you run with `mvn spring-boot:run` outside Docker, override the Spring variables from `src/main/resources/application.properties`, especially `SPRING_DATASOURCE_WRITE_JDBC_URL`, `SPRING_DATASOURCE_READ_JDBC_URL`, `SPRING_DATASOURCE_WRITE_USERNAME`, `SPRING_DATASOURCE_WRITE_PASSWORD`, `SPRING_DATASOURCE_READ_USERNAME`, `SPRING_DATASOURCE_READ_PASSWORD`, and `KAFKA_BOOTSTRAP_SERVERS`.

### Running Locally

Navigate to the module directory and execute the Spring Boot application:

```bash
cd appointment-service
mvn spring-boot:run
```

The service will start on port `8084` by default.

### Running via Docker

```bash
docker build -t appointment-service .
docker run -p 8084:8084 --env-file .env appointment-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the appointment-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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
curl -s http://localhost:8084/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8084/actuator/health

# 5. Get a development JWT with the role required by this endpoint
# Linux/macOS/Git Bash:
TOKEN=$(curl -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST")
# Windows PowerShell:
# $TOKEN = curl.exe -s "http://localhost:4004/api/auth/dev/token/raw?role=RECEPTIONIST"

# 6. Make an authenticated request through the API Gateway
# Linux/macOS/Git Bash:
curl -i -s http://localhost:4004/api/appointments/get \
  -H "Authorization: Bearer $TOKEN"
# Windows PowerShell:
# curl.exe -i -s http://localhost:4004/api/appointments/get `
#   -H "Authorization: Bearer $TOKEN"
```
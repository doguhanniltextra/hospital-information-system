# Notification Service

## Description

The Notification Service is an asynchronous dispatcher responsible for delivering alerts and operational messages to patients and medical staff.

## Functionality

- Subscribes to multiple Kafka topics across the architecture to intercept system events (e.g., appointment confirmations, lab results, discharge events).
- Formats and dispatches external communications (e.g., emails, SMS).
- Logs notification delivery statuses into a PostgreSQL database for auditing and retry mechanisms.
- Decouples notification logic from core domain services, ensuring high performance in synchronous request paths.

## Execution

### Prerequisites

- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database active and accessible
- Kafka cluster active

### Environment Variables

For Docker Compose, create `notification-service/.env` from the checked-in example:

```bash
# Linux/macOS/Git Bash
cp .env.example .env

# Windows PowerShell
# Copy-Item .env.example .env
```

```env
POSTGRES_DB=notification_db
POSTGRES_USER=notification_user
POSTGRES_PASSWORD=password_here
```

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: credentials used by the notification PostgreSQL container.

Docker Compose fills the runtime database URL, Kafka bootstrap server, and patient-service URL for you. If you run with `mvn spring-boot:run` outside Docker, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `REDIS_PORT`, and `PATIENT_SERVICE_URL` for your local services.

### Running Locally

Navigate to the module directory and execute the Spring Boot application:

```bash
cd notification-service
mvn spring-boot:run
```

The service will start on port `8090` by default.

### Running via Docker

```bash
docker build -t notification-service .
docker run -p 8090:8090 --env-file .env notification-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the notification-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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

# 2. Start this microservice and its database
docker compose up -d

# 3. Verify it's running (Wait ~10 seconds for Spring Boot to start)
# Linux/macOS/Git Bash:
curl -s http://localhost:8090/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8090/actuator/health
```
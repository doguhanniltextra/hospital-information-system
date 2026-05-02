# Billing Service

## Description
The Billing Service handles financial operations, invoice generation, and payment tracking across the hospital architecture.

## Functionality
- Operates primarily as an event-driven consumer, listening to Kafka topics for billable actions (e.g., appointments, admissions, lab orders).
- Generates invoices dynamically based on consumed domain events.
- Tracks patient balances and processes payment state transitions.
- Implements Command Query Responsibility Segregation (CQRS) with PostgreSQL for read/write isolation.
- Utilizes the Transactional Outbox pattern to emit events upon successful payment processing.

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
cd billing-service
mvn spring-boot:run
```
The service will start on port `8081` by default.

### Running via Docker
```bash
docker build -t billing-service .
docker run -p 8081:8081 --env-file .env billing-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the billing-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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
curl -s http://localhost:8081/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8081/actuator/health
```
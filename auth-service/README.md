# Auth Service

## Description
The Auth Service is the centralized authentication and authorization microservice for the Hospital Information System. It is responsible for identity management and securing access to downstream services.

## Functionality
- Manages user registration and password encoding.
- Authenticates user credentials during the login process.
- Issues, validates, and revokes JSON Web Tokens (JWT) and refresh tokens.
- Manages user roles and role-based access control.
- Provides endpoints for token rotation.
- Uses PostgreSQL as the persistence layer for users and tokens.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database active and accessible
- Kafka cluster active

### Environment Variables
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `APP_SECRET` (JWT signing key)

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd auth-service
mvn spring-boot:run
```
The service will start on port `8089` by default.

### Running via Docker
```bash
docker build -t auth-service .
docker run -p 8089:8089 --env-file .env auth-service
```

### Try it yourself (in < 60 seconds)
Run these commands from the auth-service directory. On the first Docker build this can take a few minutes; after images are built, the smoke test is quick.

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

# 3. Verify it's running (Wait ~15 seconds for Spring Boot to start)
# Linux/macOS/Git Bash:
curl -s http://localhost:8089/actuator/health
# Windows PowerShell:
# curl.exe -s http://localhost:8089/actuator/health

# 4. Register a test user
# Linux/macOS/Git Bash:
curl -X POST http://localhost:8089/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"testuser", "email":"test@example.com", "password":"Password1"}'
# Windows PowerShell:
# curl.exe -X POST http://localhost:8089/auth/register `
#   -H "Content-Type: application/json" `
#   -d '{"name":"testuser", "email":"test@example.com", "password":"Password1"}'

# 5. Login to get your JWT token
# Linux/macOS/Git Bash:
curl -s -X POST http://localhost:8089/auth/login \
  -H "Content-Type: application/json" \
  -d '{"name":"testuser", "password":"Password1"}'
# Windows PowerShell:
# curl.exe -s -X POST http://localhost:8089/auth/login `
#   -H "Content-Type: application/json" `
#   -d '{"name":"testuser", "password":"Password1"}'

# Optional: generate a role-specific development token
# Linux/macOS/Git Bash:
curl -s "http://localhost:8089/auth/dev/token?role=RECEPTIONIST"
# Windows PowerShell:
# curl.exe -s "http://localhost:8089/auth/dev/token?role=RECEPTIONIST"

# Optional: raw token output for shell scripts
# Linux/macOS/Git Bash:
curl -s "http://localhost:8089/auth/dev/token/raw?role=RECEPTIONIST"
# Windows PowerShell:
# curl.exe -s "http://localhost:8089/auth/dev/token/raw?role=RECEPTIONIST"
```
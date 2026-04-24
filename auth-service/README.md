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
The service will start on port `8081` by default.

### Running via Docker
```bash
docker build -t auth-service .
docker run -p 8081:8081 --env-file .env auth-service
```

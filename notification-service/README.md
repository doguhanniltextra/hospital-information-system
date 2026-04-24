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
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd notification-service
mvn spring-boot:run
```
The service will start on port `8088` by default.

### Running via Docker
```bash
docker build -t notification-service .
docker run -p 8088:8088 --env-file .env notification-service
```

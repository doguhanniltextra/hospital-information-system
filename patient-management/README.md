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
- `SPRING_DATASOURCE_WRITE_URL`
- `SPRING_DATASOURCE_READ_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd patient-management
mvn spring-boot:run
```
The REST API will bind to port `8082` and the gRPC server will bind to port `9002` by default.

### Running via Docker
```bash
docker build -t patient-service .
docker run -p 8082:8082 -p 9002:9002 --env-file .env patient-service
```

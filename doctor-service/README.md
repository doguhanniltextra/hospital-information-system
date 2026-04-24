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

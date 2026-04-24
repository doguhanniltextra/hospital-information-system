# Admission Service

## Description
The Admission Service manages the inpatient lifecycle, specifically tracking hospital admissions, discharges, and physical bed allocations.

## Functionality
- Manages real-time hospital bed availability and assignment.
- Processes inpatient admission and discharge workflows.
- Implements Command Query Responsibility Segregation (CQRS) utilizing dual PostgreSQL data sources.
- Publishes state transition events to Kafka, enabling downstream services (e.g., Billing and Doctor services) to react to admissions asynchronously.

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
cd admission-service
mvn spring-boot:run
```
The service will start on port `8086` by default.

### Running via Docker
```bash
docker build -t admission-service .
docker run -p 8086:8086 --env-file .env admission-service
```

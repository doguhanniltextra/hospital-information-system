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
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `PATIENT_SERVICE_HOST` / `PATIENT_SERVICE_PORT`
- `DOCTOR_SERVICE_HOST` / `DOCTOR_SERVICE_PORT`

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

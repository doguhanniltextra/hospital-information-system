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
The service will start on port `8087` by default.

### Running via Docker
```bash
docker build -t billing-service .
docker run -p 8087:8087 --env-file .env billing-service
```

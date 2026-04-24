# Support Service

## Description
The Support Service encapsulates ancillary hospital operations, specifically combining Laboratory operations and Inventory/Stock management.

## Functionality
- Manages the hospital laboratory catalog and reference data.
- Processes incoming lab test orders and publishes test results.
- Tracks hospital inventory items, stock levels, and consumption.
- Implements Command Query Responsibility Segregation (CQRS) utilizing PostgreSQL data sources.
- Employs Redis for caching frequently accessed reference data to reduce database load.
- Communicates asynchronously via Kafka for lab order placement and result dissemination.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- PostgreSQL database instances (Write DB and Read DB)
- Redis instance active
- Kafka cluster active

### Environment Variables
- `SPRING_DATASOURCE_URL` (mapped to read/write properties)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd support-service
mvn spring-boot:run
```
The service will start on port `8085` by default.

### Running via Docker
```bash
docker build -t support-service .
docker run -p 8085:8085 --env-file .env support-service
```

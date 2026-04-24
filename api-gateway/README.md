# API Gateway

## Description
The API Gateway serves as the centralized edge node and single entry point for all external client requests entering the microservice architecture.

## Functionality
- Routes incoming HTTP requests to the appropriate downstream backend microservices.
- Acts as a reverse proxy, hiding the internal network topology from external clients.
- Manages Cross-Origin Resource Sharing (CORS) configurations globally.
- Implemented using Spring Cloud Gateway, providing a non-blocking, reactive request processing pipeline.

## Execution
### Prerequisites
- Java 21 Runtime Environment
- Maven 3.9+
- Downstream services must be reachable via the network

### Environment Variables
- Target routing URIs (e.g., `AUTH_SERVICE_URL`, `PATIENT_SERVICE_URL`, etc., as configured in `application.yml`)

### Running Locally
Navigate to the module directory and execute the Spring Boot application:
```bash
cd api-gateway
mvn spring-boot:run
```
The gateway will start on port `4004` by default and begin routing traffic according to its configured predicates.

### Running via Docker
```bash
docker build -t api-gateway .
docker run -p 4004:4004 --env-file .env api-gateway
```

# System Topology and Network Architecture

The HIS architecture utilizes a distributed microservices model with a bifurcated communication strategy. Data integrity is maintained via synchronous gRPC calls for state validation, while scalability is addressed through asynchronous event choreography via Apache Kafka.

## Component Landscape

The following diagram maps the logical network boundaries, internal service ports, and data flow directions.

```mermaid
graph TB
    subgraph "Edge Network (Public)"
        direction TB
        LB["Load Balancer / Ingress"]
        GW["API Gateway<br/>Port: 4004 (REST/HTTP1.1)<br/>Component: SecurityConfig (JWT)"]
    end

    subgraph "Internal Control Plane (Master Data)"
        direction TB
        PS["Patient Service<br/>Ports: 8080 (REST), 9090 (gRPC)<br/>Storage: Postgres (R/W Split)"]
        DS["Doctor Service<br/>Port: 8083 (REST)"]
    end

    subgraph "Domain Verticals (Command Side)"
        direction TB
        AS["Appointment Service<br/>Port: 8084 (REST)<br/>Logic: Sagas / Outbox"]
        AD["Admission Service<br/>Port: 8086 (REST)<br/>Logic: CQRS / Outbox"]
        Auth["Auth Service<br/>Port: 8089 (REST)<br/>Storage: User Auth DB"]
    end

    subgraph "Reactive Plane (Query & Side Effects)"
        direction TB
        BI["Billing Service<br/>Port: 8081 (REST)<br/>Logic: Idempotent Consumer"]
        NO["Notification Service<br/>Port: 8082 (REST)<br/>Cache: Redis L1"]
        SU["Support Service<br/>Port: 8085 (REST)<br/>Logic: Lab/Inventory Sync"]
    end

    subgraph "Infrastructure Layer"
        Kafka[("Apache Kafka Cluster<br/>Port: 9092 (KRaft)")]
        Redis[("Redis Persistence<br/>Port: 6379")]
    end

    %% Edge to Internal
    LB --> GW
    GW --> Auth
    GW --> PS
    GW --> AS
    GW --> AD

    %% Internal Orchestration (gRPC)
    AS -- "gRPC/Protobuf (9090)" --> PS
    AD -- "gRPC/Protobuf (9090)" --> PS
    NO -- "gRPC/Protobuf (9090)" --> PS

    %% Event Choreography (Kafka)
    AS -- "Topic: appointment-scheduled.v1" --> Kafka
    AD -- "Topic: admission-discharged.v1" --> Kafka
    SU -- "Topic: lab-result-completed.v1" --> Kafka
    Kafka -.-> BI
    Kafka -.-> NO
    Kafka -.-> SU

    %% Persistence & Cache
    NO --- Redis
```

## Protocol Specifications

### 1. gRPC (Internal Synchronous Communication)
Internal lookups for patient master data and doctor availability bypass the REST gateway and communicate directly via gRPC over HTTP/2.
- **Serialization**: Protocol Buffers (proto3)
- **Service Interfaces**: `PatientQueryService.proto`, `DoctorQueryService.proto`
- **Interactions**: Unary Requests for validation; Client/Server streaming for bulk health data transfers.

### 2. Apache Kafka (Event Choreography)
The system employs a decentralized event-driven model to ensure loose coupling.
- **Topology**: KRaft-based cluster (no Zookeeper).
- **Semantics**: At-least-once delivery with consumer-side idempotency tracking.
- **Data Privacy**: Message payloads are **PII-Clean**. All sensitive attributes (Email, Phone, PII) are replaced with surrogate keys (UUIDs), requiring enrichment via gRPC fallback in downstream consumers.

### 3. Database Strategy
- **Master Data**: `patient-management` uses a Read/Write splitting pattern with dedicated data sources to optimize for read-heavy gRPC lookups.
- **Transactional Outbox**: Command-side services (Admission, Appointment) implement the Outbox pattern in Postgres to ensure atomic state updates and event publishing.

## Build Architecture and Dependency Strategy

The system has transitioned from a centralized inheritance model to an **Independent Build Lifecycle** strategy.

### 1. Dependency Decoupling
- **Removal of BOM**: The `patient-management-bom` has been eliminated to prevent dependency version collisions and allow services to upgrade libraries (e.g., Spring Boot, gRPC) at different velocities.
- **Direct Parentage**: Every microservice now inherits directly from `spring-boot-starter-parent`, ensuring they have full control over their runtime configuration.

### 2. Standardization via Infrastructure
- **Common Libraries**: Cross-cutting concerns (Lombok, Jackson, Protobuf) are managed via explicit, service-local version properties to maintain build hermeticity.
- **Protocol Consistency**: While builds are independent, the API contracts (Protobuf definitions) remain the "source of truth," ensuring that decoupled services remain compatible over gRPC.


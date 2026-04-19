# Hybrid Data Enrichment Specification

The system utilizes a **Hybrid Cache-First Enrichment** strategy for event hydration. This architectural decision addresses the conflict between high-throughput event-driven communication and strict security requirements (PII removal from the message bus).

## Architectural Rationale: PII Sanitization

- **Security Constraint**: No Personal Identifiable Information (PII) including email, phone, or address is permitted on the Apache Kafka bus.
- **Latency Constraint**: Consumers (e.g., `notification-service`) must not introduce high latency by performing synchronous gRPC calls to the `patient-management-service` for every event.

## Hybrid Enrichment Logic

The enrichment layer employs two tiers of data retrieval:
1.  **L1 (Non-Authoritative)**: Redis cache for low-latency lookups.
2.  **L2 (Authoritative)**: gRPC service call for authoritative recovery.

### Logical Data Flow (Decision Tree)

```mermaid
flowchart TD
    E[Kafka Message Inbound] --> ID[Extract surrogate patientId]
    ID --> L1{Query Redis L1 Cache}
    
    subgraph "Tier 1: High-Performance Lookup"
    L1 -- "Cache Hit (O(1))" --> P[Extract Contact Info Record]
    end

    subgraph "Tier 2: Authoritative Recovery"
    L1 -- "Cache Miss" --> G[Invoke PatientQueryService.findById]
    G -- "gRPC/HTTP2" --> PS["Master Data Store (Postgres)"]
    PS -- "PatientResponse" --> G
    G --> CR[Construct PatientContactInfo DTO]
    CR --> RU[SET patientContacts::{patientId} in Redis]
    RU --> P
    end

    P --> FC[Format Notification Context]
    FC --> DISP[Dispatch Communication]
```

## Implementation Specifications

### 1. Redis L1 Configuration
- **Key Strategy**: `patientContacts::{patientId}` (where patientId is a UUID string).
- **Serialization**: Standard JSON serialization using Jackson for interoperability.
- **Time-To-Live (TTL)**: 3600 seconds (1 hour). This sliding window ensures that patient contact updates propagate to the cache within 60 minutes.

### 2. Tier 2 gRPC Integration
- **Stub Type**: `PatientQueryServiceBlockingStub`. Used because enrichment is a sequential dependency for the notification dispatch.
- **Circuit Breaking**: The L2 call is wrapped in a Resilience4j circuit breaker to prevent cascading failures if the `patient-management` service is undergoing maintenance.

### 3. Consistency Model
The enrichment layer provides **Session Consistency**. If a patient updates their email via the `patient-management-service`, the notification system may continue using the stale L1 record until the TTL expires or a cache eviction event is broadcast (future optimization).

## Component Mapping
| Component | Implementation Class | Responsibility |
| :--- | :--- | :--- |
| **L1 Cache** | `RedisTemplate<String, Object>` | Storage of `PatientContactInfo` records. |
| **gRPC Client** | `PatientGrpcClient` | Circuit-broken interface for L2 fallback. |
| **Authoritative Agent** | `PatientQueryServiceGrpcImpl` | Server-side gRPC implementation for master data lookups. |

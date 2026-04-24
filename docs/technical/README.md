# HIS Technical Documentation Specification

This documentation suite provides a comprehensive architectural breakdown of the Hospital Information System (HIS). It is intended for senior engineers and system architects to understand the distributed communication patterns, consistency models, and service boundaries of the ecosystem.

## Architectural Framework
- [System Topology](topology.md): High-level service landscape including gRPC/Kafka protocol mapping and network topology.
- [Design Patterns](../Design%20Pattern/DESIGN_PATTERNS.md): Analysis of the structural and behavioral patterns (CQRS, Sagas, Outbox) implemented per-service.

## Core Protocol Journeys
- [Appointment Lifecycle](appointment_lifecycle.md): Synchronous validation chains via gRPC and asynchronous notification propagation via Kafka.
- [Admission & Financial Orchestration](admission_and_billing.md): Inpatient lifecycle management, automated daily charge generation, and the Transactional Outbox pattern.

## Distributed Paradigms
- [Hybrid Data Enrichment](hybrid_enrichment.md): Two-tier caching strategy (Redis L1 + gRPC L2) for PII-clean event hydration.
- [Idempotency and Consistency](admission_and_billing.md#idempotency-tracking): Exactly-once processing logic using message tracking in downstream consumers.
- [Security and Compliance](security_and_compliance.md): Identity linking, privilege escalation mitigation, and data exposure prevention.

---

### microservice Registry

| Service | Port (REST/Edge) | Port (gRPC/Int) | Primary Data Store | Consistency Model |
| :--- | :--- | :--- | :--- | :--- |
| `api-gateway` | 4004 | N/A | N/A | Stateless (JWT) |
| `auth-service` | 8089 | N/A | Postgres (User) | ACID |
| `patient-management` | 8080 | 9090 | Postgres (Master) | ACID (R/W Split) |
| `appointment-service`| 8084 | N/A | Postgres (Transactional) | Eventual (Outbox) |
| `admission-service` | 8086 | N/A | Postgres (CQRS) | Eventual (Outbox) |
| `support-service` | 8085 | N/A | Postgres (CQRS) | Read-Repair / Async |
| `billing-service` | 8081 | N/A | Postgres | Idempotent Consumer |
| `notification-service`| 8082 | N/A | Redis (L1 Cache) | Cache-First Hybrid |

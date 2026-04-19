# Appointment Lifecycle Specification

The appointment scheduling process is an orchestrated distributed transaction that enforces data integrity across the Patient, Doctor, and Appointment domains.

## Synchronous Validation and Persistence Sequence

The following sequence diagram details the interaction between internal service components, gRPC stubs, and the Kafka producer lifecycle.

```mermaid
sequenceDiagram
    autonumber
    participant U as External Client
    participant G as API Gateway
    participant ACS as AppointmentCommandService
    participant PS as PatientQueryService (gRPC)
    participant K as Kafka (Broker)
    participant NCS as NotificationConsumer (Service)
    participant R as Redis (L1 Cache)

    U->>G: POST /api/appointments/schedule (JWT Required)
    G->>ACS: POST /internal/v1/appointments

    rect rgb(245, 245, 245)
        Note right of ACS: Domain Validation Layer
        ACS->>PS: PatientQueryServiceBlockingStub.findById(FindPatientRequest)
        PS-->>ACS: FindPatientResponse (UUID existence verification)
    end

    ACS->>ACS: Persist Appointment Entity (Status: SCHEDULED)

    rect rgb(230, 240, 255)
        Note right of ACS: Outbox Persistence
        ACS->>ACS: Save AppointmentScheduledEvent to AppointmentOutbox
    end

    ACS-->>G: 201 Created (AppointmentResponseDto)
    G-->>U: HTTP 201 Response

    rect rgb(230, 255, 230)
        Note right of NCS: Asynchronous Enrichment
        K-->>NCS: Consume: AppointmentScheduledEvent (PII-Clean)
        NCS->>R: Cache Lookup (Key: patientContacts::{uuid})
        alt Cache Miss
            NCS->>PS: gRPC lookup for PII recovery
            PS-->>NCS: PatientResponse (Email, Phone)
            NCS->>R: SETEX patientContacts::{uuid} 3600
        end
        NCS->>NCS: Execute Notification Logic
    end
```

## Implementation Details

### 1. Data Transfer Objects (DTOs)
- **Request**: `AppointmentRequestDto` containing `patientId`, `doctorId`, and `appointmentDate`.
- **gRPC Message**: `FindPatientRequest` (Protobuf-encoded) used for low-latency validation against the Patient Master Data service.
- **Kafka Payload**: `AppointmentScheduledEvent`. This object is strictly sanitized; it contains surrogate identifiers and lacks any patient-identifiable markers (PII).

### 2. Transactional Integrity
The Appointment Service employs a local transaction boundary that includes:
1.  State transition in the primary `appointments` table.
2.  Insertion into the `appointment_outbox` table.

A dedicated `AppointmentOutboxPublisher` polls the outbox table to ensure at-least-once delivery to the `appointment-scheduled.v1` topic, decoupling the database commit from the Kafka network availability.

### 3. Concurrency and Race Conditions
Validations via gRPC are performed within the request-response thread. The system relies on Postgres default isolation levels (Read Committed) and gRPC timeouts (default 2s) to maintain throughput while preventing cascading failures during high load.

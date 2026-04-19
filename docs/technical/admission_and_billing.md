# Admission and Financial Orchestration

The Admission vertical manages the lifecycle of inpatient stay, coordinating between bed utilization, recurring daily charges, and final discharge billing.

## Admission Lifecycle State Machine

A patient's localized state within the hospital environment is managed via the `Admission` aggregate.

```mermaid
stateDiagram-v2
    [*] --> ADMITTED: Persistent Admission Record Created
    ADMITTED --> ACTIVE: Bed Assigned (BedStatus: OCCUPIED)
    
    state ACTIVE {
        [*] --> STABLE
        STABLE --> CRITICAL: Clinical Indicator Threshold Met
        CRITICAL --> STABLE: Stabilized
        STABLE --> CHARGING: Midnight Scheduler Trigger (Cron)
        CHARGING --> STABLE: DailyBedChargeEvent Emitted
    }

    ACTIVE --> DISCHARGING: Discharge Order Issued (Clinical)
    DISCHARGING --> DISCHARGED: Discharge Finalized (Administrative)
    DISCHARGED --> [*]: Resource De-allocation (BedStatus: CLEANING)

    Note right of ACTIVE: Daily charges are generated<br/>for all admissions with<br/>status = 'ACTIVE'.
```

## Transactional Outbox Implementation

To ensure eventual consistency between the Admission state and the Billing/Notification services without distributed transactions (2PC), the system implements the **Transactional Outbox Pattern**.

### Outbox Table Schema
| Component | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID | Primary Key / Message Identity |
| `aggregate_id` | String | The ID of the Admission record (surrogate key) |
| `event_type` | String | e.g., `PATIENT_DISCHARGED`, `DAILY_BED_CHARGE` |
| `payload` | TEXT (JSON) | Protobuf-derived JSON payload |
| `status` | Enum | PENDING, PROCESSED, FAILED |
| `retry_count` | INT | Backoff tracking for Kafka publisher failures |

### Publication Flow
```mermaid
sequenceDiagram
    participant DB as Postgres (Write DataSource)
    participant AD as AdmissionCommandService
    participant OP as OutboxPublisher (Scheduler)
    participant K as Kafka (Broker)
    participant BI as BillingConsumer (Service)

    Note over AD,DB: Atomic Transaction Begin
    AD->>DB: UPDATE admissions SET status = 'DISCHARGED'
    AD-->>DB: INSERT INTO admission_outbox (PATIENT_DISCHARGED, PENDING)
    Note over AD,DB: Atomic Transaction Commit

    loop Outbox Polling Cycle (FixedDelay: 5000ms)
        OP->>DB: SELECT * FROM admission_outbox WHERE status = 'PENDING'
        OP->>K: KafkaTemplate.send(topic, payload)
        K-->>OP: RecordMetadata (Ack)
        OP->>DB: UPDATE admission_outbox SET status = 'PROCESSED'
    end

    K-->>BI: Consume(PATIENT_DISCHARGED)
    BI->>BI: FinalizeInvoice(admission_id)
```

## Idempotency Tracking

To mitigate the effects of Kafka's "at-least-once" delivery semantics, all downstream consumers (Billing, Notification) utilize a **Processed Message Store**.

### Implementation Strategy
Before executing a state change (e.g., generating an invoice), the consumer performs an existence check:
```java
if (processedEventRepository.existsByMessageId(messageId)) {
    log.info("Duplicate message detected: {}. Skipping.", messageId);
    return;
}
// Proceed with processing...
processedEventRepository.save(new ProcessedEvent(messageId));
```
- **Storage**: The `processed_events` table in the consumer's local schema.
- **Cleanup**: Messages older than 7 days are automatically purged via a background purge job to maintain index performance.

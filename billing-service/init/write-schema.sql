CREATE USER billing_user WITH PASSWORD 'billing_pass_123';
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema;

-- Main tables for write model
CREATE TABLE IF NOT EXISTS billing_schema.invoices (
    invoice_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    patient_owes DECIMAL(10,2) NOT NULL,
    insurance_owes DECIMAL(10,2) NOT NULL,
    invoice_pdf_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS billing_schema.claims (
    claim_id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS billing_schema.unbilled_charges (
    charge_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    admission_id UUID,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing_schema.billing_outbox_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE
);
CREATE USER billing_user WITH PASSWORD 'billing_pass_123';
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema;

-- Read model tables
CREATE TABLE IF NOT EXISTS billing_schema.invoice_summaries (
    invoice_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    patient_owes DECIMAL(10,2) NOT NULL,
    insurance_owes DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    patient_name VARCHAR(255),
    doctor_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
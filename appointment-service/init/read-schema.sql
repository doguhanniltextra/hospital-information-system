CREATE USER appointment_user WITH PASSWORD 'appointment_pass_123';
CREATE SCHEMA IF NOT EXISTS appointment_schema AUTHORIZATION appointment_user;
GRANT ALL ON SCHEMA appointment_schema TO appointment_user;
ALTER USER appointment_user SET search_path = appointment_schema;

CREATE TABLE IF NOT EXISTS appointment_schema.appointment_summaries (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    patient_name VARCHAR(255),
    patient_email VARCHAR(255),
    doctor_id UUID NOT NULL,
    doctor_name VARCHAR(255),
    doctor_specialization VARCHAR(255),
    service_date VARCHAR(255),
    service_type VARCHAR(50),
    amount FLOAT,
    payment_status BOOLEAN
);

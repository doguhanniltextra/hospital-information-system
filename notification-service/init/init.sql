CREATE USER notification_user WITH PASSWORD 'notification_pass_123';
CREATE SCHEMA IF NOT EXISTS notification_schema AUTHORIZATION notification_user;
GRANT ALL ON SCHEMA notification_schema TO notification_user;
ALTER USER notification_user SET search_path = notification_schema;

CREATE TABLE IF NOT EXISTS notification_schema.notification_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(255) NOT NULL UNIQUE,
    channel VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE notification_schema.notification_templates OWNER TO notification_user;

INSERT INTO notification_schema.notification_templates (id, template_code, channel, subject, body, created_at, updated_at)
VALUES 
(gen_random_uuid(), 'LAB_RESULT_READY', 'EMAIL', 'Your Lab Results are Ready', 'Dear Patient, your lab results for order [(${patientId})] are now available at: [(${reportUrl})]', now(), now()),
(gen_random_uuid(), 'APPOINTMENT_CONFIRMATION', 'EMAIL', 'Appointment Confirmation', 'Dear Patient, your appointment is confirmed for [(${appointmentDate})].', now(), now()),
(gen_random_uuid(), 'HOSPITAL_DISCHARGE', 'EMAIL', 'Discharge Summary', 'Dear Patient, you have been discharged. Admission ID: [(${admissionId})]. Get well soon!', now(), now())
ON CONFLICT (template_code) DO NOTHING;

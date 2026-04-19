CREATE USER appointment_user WITH PASSWORD 'appointment_pass_123';
CREATE SCHEMA IF NOT EXISTS appointment_schema AUTHORIZATION appointment_user;
GRANT ALL ON SCHEMA appointment_schema TO appointment_user;
ALTER USER appointment_user SET search_path = appointment_schema;

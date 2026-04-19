CREATE USER patient_user WITH PASSWORD 'patient_pass_123';
CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user;
GRANT ALL ON SCHEMA patient_schema TO patient_user;
ALTER USER patient_user SET search_path = patient_schema;

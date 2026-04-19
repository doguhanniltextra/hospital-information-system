CREATE USER doctor_user WITH PASSWORD 'doctor_pass_123';
CREATE SCHEMA IF NOT EXISTS doctor_schema AUTHORIZATION doctor_user;
GRANT ALL ON SCHEMA doctor_schema TO doctor_user;
ALTER USER doctor_user SET search_path = doctor_schema;

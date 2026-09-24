-- =============================================================================
-- Hospital Information System (HIS) - Centralized Database Initialization
-- =============================================================================

-- 1. AUTH SERVICE
CREATE DATABASE auth_db;
CREATE USER auth_user WITH ENCRYPTED PASSWORD 'auth_pass_123';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;
\c auth_db
CREATE SCHEMA IF NOT EXISTS auth_schema AUTHORIZATION auth_user;
GRANT ALL ON SCHEMA auth_schema TO auth_user;
ALTER USER auth_user SET search_path = auth_schema, public;


-- 2. PATIENT MANAGEMENT (CQRS Architecture)
\c postgres
CREATE DATABASE patient_db;
CREATE USER patient_user WITH ENCRYPTED PASSWORD 'patient_pass_123';
GRANT ALL PRIVILEGES ON DATABASE patient_db TO patient_user;
\c patient_db
CREATE SCHEMA IF NOT EXISTS patient_schema AUTHORIZATION patient_user;
GRANT ALL ON SCHEMA patient_schema TO patient_user;
ALTER USER patient_user SET search_path = patient_schema, public;

-- 3. DOCTOR SERVICE (CQRS Architecture)
\c postgres
CREATE DATABASE doctor_db;
CREATE USER doctor_user WITH ENCRYPTED PASSWORD 'doctor_pass_123';
GRANT ALL PRIVILEGES ON DATABASE doctor_db TO doctor_user;
\c doctor_db
CREATE SCHEMA IF NOT EXISTS doctor_schema AUTHORIZATION doctor_user;
GRANT ALL ON SCHEMA doctor_schema TO doctor_user;
ALTER USER doctor_user SET search_path = doctor_schema, public;

-- 4. APPOINTMENT SERVICE (CQRS Architecture)
\c postgres
CREATE DATABASE appointment_db;
CREATE USER appointment_user WITH ENCRYPTED PASSWORD 'appointment_pass_123';
GRANT ALL PRIVILEGES ON DATABASE appointment_db TO appointment_user;
\c appointment_db
CREATE SCHEMA IF NOT EXISTS appointment_schema AUTHORIZATION appointment_user;
GRANT ALL ON SCHEMA appointment_schema TO appointment_user;
ALTER USER appointment_user SET search_path = appointment_schema, public;

-- 5. ADMISSION SERVICE (CQRS Architecture)
\c postgres
CREATE DATABASE admission_db;
CREATE USER admission_user WITH ENCRYPTED PASSWORD 'admission_pass_123';
GRANT ALL PRIVILEGES ON DATABASE admission_db TO admission_user;
\c admission_db
CREATE SCHEMA IF NOT EXISTS admission_schema AUTHORIZATION admission_user;
GRANT ALL ON SCHEMA admission_schema TO admission_user;
ALTER USER admission_user SET search_path = admission_schema, public;

-- 6. BILLING SERVICE (CQRS Architecture)
\c postgres
CREATE DATABASE billing_db;
CREATE USER billing_user WITH ENCRYPTED PASSWORD 'billing_pass_123';
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;
\c billing_db
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema, public;

-- 7. SUPPORT SERVICE (Lab Results, Inventory)
\c postgres
CREATE DATABASE support_db;
CREATE USER support_user WITH ENCRYPTED PASSWORD 'support_pass_123';
GRANT ALL PRIVILEGES ON DATABASE support_db TO support_user;
\c support_db
CREATE SCHEMA IF NOT EXISTS support_schema AUTHORIZATION support_user;
GRANT ALL ON SCHEMA support_schema TO support_user;
ALTER USER support_user SET search_path = support_schema, public;

-- 8. NOTIFICATION SERVICE
\c postgres
CREATE DATABASE notification_db;
CREATE USER notification_user WITH ENCRYPTED PASSWORD 'notification_pass_123';
GRANT ALL PRIVILEGES ON DATABASE notification_db TO notification_user;
\c notification_db
CREATE SCHEMA IF NOT EXISTS notification_schema AUTHORIZATION notification_user;
GRANT ALL ON SCHEMA notification_schema TO notification_user;
ALTER USER notification_user SET search_path = notification_schema, public;

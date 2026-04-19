CREATE USER billing_user WITH PASSWORD 'billing_pass_123';
CREATE SCHEMA IF NOT EXISTS billing_schema AUTHORIZATION billing_user;
GRANT ALL ON SCHEMA billing_schema TO billing_user;
ALTER USER billing_user SET search_path = billing_schema;

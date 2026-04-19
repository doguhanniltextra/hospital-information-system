CREATE USER audit_user WITH PASSWORD 'audit_pass_123';
CREATE SCHEMA IF NOT EXISTS audit_schema AUTHORIZATION audit_user;
GRANT ALL ON SCHEMA audit_schema TO audit_user;
ALTER USER audit_user SET search_path = audit_schema;

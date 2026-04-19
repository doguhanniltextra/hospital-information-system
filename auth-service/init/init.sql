CREATE USER auth_user WITH PASSWORD 'auth_pass_123';
CREATE SCHEMA IF NOT EXISTS auth_schema AUTHORIZATION auth_user;
GRANT ALL ON SCHEMA auth_schema TO auth_user;
ALTER USER auth_user SET search_path = auth_schema;

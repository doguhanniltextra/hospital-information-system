CREATE USER admission_user WITH PASSWORD 'admission_pass_123';
CREATE SCHEMA IF NOT EXISTS admission_schema AUTHORIZATION admission_user;
GRANT ALL ON SCHEMA admission_schema TO admission_user;
ALTER USER admission_user SET search_path = admission_schema;

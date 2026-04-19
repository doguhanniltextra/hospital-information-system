DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'doctor_user') THEN
    CREATE USER doctor_user WITH PASSWORD 'doctor_pass_123';
  END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS doctor_schema AUTHORIZATION doctor_user;
GRANT ALL ON SCHEMA doctor_schema TO doctor_user;
ALTER USER doctor_user SET search_path = doctor_schema;
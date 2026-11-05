DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${app_user}') THEN
        CREATE ROLE ${app_user} LOGIN PASSWORD '${app_password}';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO ${app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${app_user};
REVOKE ALL ON flyway_schema_history FROM ${app_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${app_user};

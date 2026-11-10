\set ON_ERROR_STOP on

-- Run as a database owner/administrator. Supply psql variables through a
-- protected secret manager or secure interactive session, never commit values.
SELECT format('CREATE ROLE ledger_migrator LOGIN PASSWORD %L', :'migrator_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ledger_migrator')
\gexec
SELECT format('ALTER ROLE ledger_migrator PASSWORD %L', :'migrator_password')
\gexec

SELECT format('CREATE ROLE ledger_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ledger_app')
\gexec
SELECT format('ALTER ROLE ledger_app PASSWORD %L', :'app_password')
\gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO ledger_migrator, ledger_app', current_database())
\gexec

GRANT USAGE, CREATE ON SCHEMA public TO ledger_migrator;
GRANT USAGE ON SCHEMA public TO ledger_app;

-- Grant existing LedgerLine objects only; do not expose unrelated tables in a
-- shared database. For a fresh database the default privileges below apply.
DO $$
DECLARE object_name text;
BEGIN
    FOR object_name IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename IN ('users', 'admins', 'account_holders', 'vendors',
                            'verification_tokens', 'ledger_accounts',
                            'ledger_entries', 'audit_events')
    LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.%I TO ledger_app', object_name);
    END LOOP;

    FOR object_name IN
        SELECT seq_obj.relname
        FROM pg_class seq_obj
        JOIN pg_namespace ns ON ns.oid = seq_obj.relnamespace
        WHERE seq_obj.relkind = 'S'
          AND ns.nspname = 'public'
          AND (
              seq_obj.relname = 'user_sequence'
              OR EXISTS (
                  SELECT 1
                  FROM pg_depend dep
                  JOIN pg_class table_obj ON table_obj.oid = dep.refobjid
                  WHERE dep.objid = seq_obj.oid
                    AND dep.deptype IN ('a', 'i')
                    AND table_obj.relname IN ('ledger_accounts', 'ledger_entries', 'audit_events')
              )
          )
    LOOP
        EXECUTE format('GRANT USAGE, SELECT, UPDATE ON SEQUENCE public.%I TO ledger_app', object_name);
    END LOOP;
END $$;

ALTER DEFAULT PRIVILEGES FOR ROLE ledger_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ledger_app;
ALTER DEFAULT PRIVILEGES FOR ROLE ledger_migrator IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO ledger_app;

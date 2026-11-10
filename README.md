# Ledger backend

Spring Boot 4 / Java 21 API for `P:\ledger-frontend`.

## Local configuration

The application reads secrets and deployment-specific addresses from process environment variables. Do not put production secrets in `application.yml`.

Required for authentication and database access:

- `JWT_SECRET` — at least 32 random bytes for HS256 signing.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — defaults point to a local PostgreSQL database; set the password for your local instance.
- `REDIS_HOST`, `REDIS_PORT` — defaults to local Redis. Redis backs login rate limiting.
- `FLYWAY_DB_URL`, `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` — optional separate migration connection; by default Flyway inherits the application DB connection. Use a schema-migration role in deployed environments and give the runtime role only the DML and sequence privileges it needs.
- `CORS_ALLOWED_ORIGINS` — comma-separated frontend origins; defaults to the local origins and dev-tunnel origin listed in `src/main/resources/application.yml`.

The default local file upload limits are 10 MB per file and 20 MB per request. Database and JWT credentials must be supplied through environment variables; do not add them as plaintext YAML values.

Optional integrations:

- `MAIL_USERNAME`, `MAIL_PASSWORD` for email verification.
- `WHATSAPP_API_URL`, `WHATSAPP_API_BASE` for mobile verification links.
- `JWT_REFRESH_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS` to override token defaults.

Use `.env.example` as the list of backend variables to configure in your shell, IDE, or hosting provider. Spring Boot does not automatically load a `.env` file. Keep real credentials out of `.env.example` and source control.

Start the API from PowerShell:

```powershell
cd P:\ledger
$env:JWT_SECRET = '<generate a random value of at least 32 bytes>'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/ecommerce_dev'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = '<your local database password>'
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
.\mvnw.cmd spring-boot:run
```

## First administrator bootstrap

Bootstrap is disabled unless explicitly enabled. Configure these values in the deployment's secret environment, then start one backend instance:

- `BOOTSTRAP_ADMIN_ENABLED=true`
- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_NUMBER` (6–10 characters, matching the existing user column)
- `BOOTSTRAP_ADMIN_PASSWORD` (at least 12 characters)

The runner creates an administrator only if no admin role exists, hashes the password, links the admin profile, marks the bootstrap account verified, and writes an audit event. It does not log the password. Once the first admin is created, set `BOOTSTRAP_ADMIN_ENABLED=false` or remove it. An existing admin causes the runner to skip creation.

## API overview

- Sign-in: `POST /api/users/auth/admin/login`, `/api/users/auth/ACCOUNT_HOLDER/login`, `/api/users/auth/auditor/login`
- Current session and verification: `GET /api/users/auth/me`, `POST /api/users/auth/email`, `POST /api/users/auth/mobile`
- Admin provisions account holders/auditors: `POST /api/users/auth/register`; admin directory: `GET /api/admin/users`
- Admin opens account: `POST /api/ledger/accounts`; holder account: `GET /api/ledger/accounts/me`
- Money operations: `POST /api/ledger/deposits`, `/api/ledger/withdrawals`, `/api/ledger/transfers`
- Holder ledger/audit: `GET /api/ledger/entries/me`, `/api/ledger/audit-events/me`
- Admin/auditor read APIs: `GET /api/audit/ledger-entries`, `/api/audit/events`, `/api/audit/integrity`
- Health: `GET /api/health/live`, `GET /api/health/ready`

## Authorization checks

Spring Security restricts the ledger API to authenticated administrators and account holders, and restricts audit APIs to administrators and auditors. Services also check the actor's role and account ownership before performing ledger operations. The focused role-boundary tests cover auditor denials for account opening, withdrawals, and holder-scoped ledger history:

```powershell
mvn -o -Dtest=LedgerRoleAccessTests test
```

These unit tests do not replace end-to-end checks against a configured database and Redis instance.

## Container deployment

Build the API image with `docker build -t ledger-backend .` and run it with PostgreSQL and Redis addresses plus the required secrets supplied by the hosting platform. The container listens on port 8080 and runs as an unprivileged user. Configure the host's health probe to use `/api/health/ready`; the readiness endpoint requires both PostgreSQL and Redis. The frontend must be deployed separately with `VITE_API_URL` pointing to the API origin, and that origin must allow the deployed frontend origin through `CORS_ALLOWED_ORIGINS`.

Do not enable the first-admin bootstrap with real credentials until database access is configured. After a successful bootstrap, disable it. Never use the sample values in `.env.example` as deployment credentials.

## Database migrations and roles

Versioned Flyway SQL lives in `src/main/resources/db/migration`. Flyway applies migrations during startup; Hibernate then validates the mapped schema and does not create or alter tables. The initial migration creates the eight current JPA entity tables. It uses `IF NOT EXISTS` to support a database previously initialized by Hibernate, but existing production data and object ownership must be reviewed before applying it. Back up existing data and run migrations against a staging copy first.

`database/bootstrap-roles.sql` provisions a `ledger_migrator` login and a separate `ledger_app` runtime login, then grants the runtime account table DML and sequence access. Run it with a PostgreSQL administrator in a protected session, supplying the `migrator_password` and `app_password` psql variables through your approved secret-management process. Configure `FLYWAY_DB_*` with the migrator credentials and `DB_*` with the runtime credentials. The script does not grant role-admin rights to the application. Some managed PostgreSQL services restrict `CREATE ROLE`; use the provider's role management and equivalent grants in that case.

For an interactive local PostgreSQL administrator login, run `psql --host localhost --username postgres --dbname ecommerce_dev --file database/bootstrap-roles.sql`. The script prompts for the two new passwords without echoing them. It is safe to rerun to rotate those passwords and reapply the grants. In automation, supply `migrator_password` and `app_password` as protected psql variables rather than storing them in this file.

For an existing schema whose tables were created by another owner, the migrator may need a DBA-reviewed ownership or grants plan for future `ALTER TABLE` migrations. Do not transfer ownership or run bootstrap SQL against a live database without reviewing its current roles and data. CI starts a clean PostgreSQL and Redis pair and runs the application context plus role-boundary tests against the migration.

# Ledger backend

Spring Boot 4 / Java 21 API for `P:\ledger-frontend`.

## Local configuration

The application reads secrets and deployment-specific addresses from Spring configuration. Do not put production secrets in YAML or source control. At the time this README was updated, no tracked Spring `application.yml` or backend `.env.example` was present in this repository; configure all required properties explicitly in your shell, IDE, or hosting environment before starting the API.

Required for authentication and database access:

- `JWT_SECRET` — at least 32 random bytes for HS256 signing.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — configure a dedicated PostgreSQL database. Do not point LedgerLine migrations at an unrelated or existing ecommerce database without reviewing its schema first.
- `REDIS_HOST`, `REDIS_PORT` — defaults to local Redis. Redis backs login rate limiting.
- `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` — separate schema migration connection. Configure `ledger_migrator` here and `ledger_app` for the datasource; give the runtime role only the DML and sequence privileges it needs.
- `app.cors.allowed-origins` — comma-separated frontend origins required by `CorsConfig`.
- `whaths-app-base.base` — required by `RestClientConfig`, even if you are not using WhatsApp verification locally.
- `jwt.expirationMs` — JWT lifetime, required by `JWTUtil`.

Database and JWT credentials must be supplied through environment variables or secret-managed Spring properties; do not add them as plaintext YAML values.

Optional integrations:

- `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` for email verification.

Spring Boot does not automatically load a `.env` file. Keep real credentials out of source control. The current repository documentation and application wiring need to stay aligned; confirm each property name in the active runtime configuration before deployment.

Start the API from PowerShell:

```powershell
cd P:\ledger
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/ledger_dev'
$env:SPRING_DATASOURCE_USERNAME = 'ledger_app'
$env:SPRING_DATASOURCE_PASSWORD = '<the ledger_app password you entered during role setup>'
$env:SPRING_FLYWAY_URL = $env:SPRING_DATASOURCE_URL
$env:SPRING_FLYWAY_USER = 'ledger_migrator'
$env:SPRING_FLYWAY_PASSWORD = '<the ledger_migrator password you entered during role setup>'
$env:JWT_SECRET = '<generate a random value of at least 32 bytes>'
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
$env:SPRING_APPLICATION_JSON = '{"app":{"cors":{"allowed-origins":"http://localhost:5173"}},"whaths-app-base":{"base":"http://localhost:3000"},"jwt":{"expirationMs":86400000}}'
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
- Account-holder recipient lookup: `GET /api/ledger/accounts/{accountId}/recipient` (returns recipient name and ID, not balance)
- Money operations: `POST /api/ledger/deposits`, `/api/ledger/withdrawals`, `/api/ledger/transfers`
- MPIN setup/status: `PUT /api/users/auth/mpin`, `GET /api/users/auth/mpin/status`
- Holder ledger/audit/transfer history: `GET /api/ledger/entries/me`, `/api/ledger/audit-events/me`, `/api/ledger/transfers/me`
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

Do not enable the first-admin bootstrap with real credentials until database access is configured. After a successful bootstrap, disable it. Treat all example values and placeholders as development-only.

## Database migrations and roles

Versioned Flyway SQL lives in `src/main/resources/db/migration`. Flyway applies migrations during startup; Hibernate then validates the mapped schema and does not create or alter tables. The initial migration creates the eight current JPA entity tables. It uses `IF NOT EXISTS` to support a database previously initialized by Hibernate, but existing production data and object ownership must be reviewed before applying it. Back up existing data and run migrations against a staging copy first.

`database/bootstrap-roles.sql` provisions a `ledger_migrator` login and a separate `ledger_app` runtime login, then grants the runtime account table DML and sequence access. Run it with a PostgreSQL administrator in a protected session, supplying the `migrator_password` and `app_password` psql variables through your approved secret-management process. Configure `FLYWAY_DB_*` with the migrator credentials and `DB_*` with the runtime credentials. The script does not grant role-admin rights to the application. Some managed PostgreSQL services restrict `CREATE ROLE`; use the provider's role management and equivalent grants in that case.

For local setup, create the dedicated database with `createdb --host localhost --username postgres ledger_dev`, then run `psql --host localhost --username postgres --dbname ledger_dev --file database/bootstrap-roles.sql`. The script prompts for the two new passwords without echoing them. It is safe to rerun to rotate those passwords and reapply the grants. In automation, supply `migrator_password` and `app_password` as protected psql variables rather than storing them in this file.

For an existing schema whose tables were created by another owner, the migrator may need a DBA-reviewed ownership or grants plan for future `ALTER TABLE` migrations. Do not transfer ownership or run bootstrap SQL against a live database without reviewing its current roles and data. CI is configured to start PostgreSQL and Redis and run the application context plus role-boundary tests; check the latest CI run before claiming those checks passed.

## Transfer confirmation and duplicate protection

Transfers require a positive amount with no more than two fractional digits and a six-digit MPIN. Account holders configure an MPIN once through the authenticated MPIN setup route; the backend stores a BCrypt hash, checks the MPIN on every transfer, and temporarily locks MPIN verification after five failures in a ten-minute window using Redis. Redis must be available for this check.

Every transfer request must include a unique `Idempotency-Key` header. The backend records the key and transfer result in the same database transaction as the paired ledger postings. A retry with the same key and identical account/amount returns the original result; reusing that key for a different transfer is rejected. The frontend creates a new key for each Pay confirmation and reuses it when retrying that same confirmation.

Flyway migrations `V2__add_user_mpin_hash.sql` and `V3__add_transfer_idempotency.sql` add the MPIN hash and idempotency table. Apply and verify all migrations against the intended database before deploying this backend version. The MPIN currently has first-time setup but no self-service reset/change flow.

See [threats.md](threats.md) for the trust boundaries, current controls, and known security limitations.

## Verification status and scope

Frontend lint and production build passed in the checked development environment. The backend role-boundary test passed. A full local backend test run could not start the Spring application context because PostgreSQL was not reachable in that environment. CI is configured to run the suite with PostgreSQL and Redis services, but check the latest CI run before claiming it passed. The deployed registration, first-admin bootstrap, transfer flow, and audit screens still need live end-to-end verification unless you have separately completed that verification.

This project is a demonstration using synthetic data. It is not a production banking service and must not handle real funds or real customer financial data.

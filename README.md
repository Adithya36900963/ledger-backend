# Ledger backend

Spring Boot 4 / Java 21 API for `P:\ledger-frontend`.

## Local configuration

The application reads secrets and deployment-specific addresses from process environment variables. Do not put production secrets in `application.yml`.

Required for authentication and database access:

- `JWT_SECRET` — at least 32 random bytes for HS256 signing.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — defaults point to a local PostgreSQL database; set the password for your local instance.
- `REDIS_HOST`, `REDIS_PORT` — defaults to local Redis. Redis backs login rate limiting.
- `CORS_ALLOWED_ORIGINS` — comma-separated frontend origins; defaults to `http://localhost:5173`.

Optional integrations:

- `MAIL_USERNAME`, `MAIL_PASSWORD` for email verification.
- `WHATSAPP_API_URL`, `WHATSAPP_API_BASE` for mobile verification links.
- `JWT_REFRESH_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS` to override token defaults.

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

## Current schema-management note

The checked-in backend currently uses Hibernate `ddl-auto: update`; there are no versioned Flyway/Liquibase migrations or database-role provisioning scripts in this repository. The DB login is environment-configurable, but moving to a least-privilege runtime role requires versioned schema migrations and a DBA-reviewed grants plan before disabling schema updates. No automatic role grants are performed by this application.

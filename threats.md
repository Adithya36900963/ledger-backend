# LedgerLine threat model

## Scope and intended use

This document describes the current LedgerLine demo: the React frontend, Spring Boot API, PostgreSQL ledger, Redis rate limits, and deployment configuration. It is a hackathon/hiring-challenge prototype using synthetic data. It is not a bank, payment processor, or production system and must not hold real funds or real customer financial data.

## Assets to protect

- Account balances and paired ledger postings.
- User credentials, JWT cookies, MPIN hashes, and verification state.
- Role assignments and account ownership.
- Audit events, transaction identifiers, and idempotency records.
- Database credentials, JWT signing keys, bootstrap credentials, and Redis availability.

## Trust boundaries and actors

- The browser is untrusted. Frontend route guards and form validation improve usability but are not security controls.
- The Spring API is the enforcement boundary for authentication, role checks, account ownership, amount validation, MPIN checks, transaction posting, and audit logging.
- PostgreSQL stores users, balances, paired ledger entries, audit events, MPIN hashes, and transfer idempotency results.
- Redis stores login and MPIN attempt-limit state. Losing Redis affects those checks and readiness.
- Administrators provision holders/auditors and can open accounts. Account holders operate only their own accounts. Auditors have read-only audit access.
- External threats include stolen credentials or cookies, malicious authenticated users, cross-site request attempts, request replay, and compromised deployment secrets.

## Current controls in code

- Spring Security and service-level role/ownership checks protect ledger and audit operations.
- Passwords and six-digit MPINs are BCrypt-hashed; MPIN is checked on every transfer.
- MPIN attempts are temporarily locked after five failures within the Redis window.
- Transfer amounts must be positive and have at most two decimal places; available funds, destination existence, and self-transfer rules are checked server-side.
- Transfer posting writes both sides of the ledger update in a database transaction.
- The required `Idempotency-Key` is persisted with the transfer result in the same database transaction. Retrying an identical request returns the original result; key reuse for a different transfer is rejected.
- User, ledger, and failed transaction activity is recorded in audit events where implemented.
- Flyway applies versioned migrations; the database runtime and migration roles are separate in the documented setup.
- JWT cookies are marked HttpOnly and Secure. CORS is configured for explicit origins with credentials.

## Known risks and unfinished protections

### High: CSRF protection needs review

Authentication uses a cookie with `SameSite=None`, while Spring CSRF protection is disabled. CORS is not a substitute for CSRF protection. Before production use, add and verify a CSRF-token strategy for state-changing routes, or redesign cookie attributes/authentication so cross-site requests cannot use the credential. Test transfers, deposits, withdrawals, admin actions, and MPIN setup from an untrusted origin.

### High: MPIN lifecycle and strength

MPIN is six numeric digits, is configured once, and has no self-service reset/change workflow. The Redis attempt limit reduces online guessing but does not make a short PIN appropriate for real funds. Add a secure recovery/change process, review lockout behavior and monitoring, and use stronger step-up authentication for any real financial operation.

### Medium: database-backed behaviors need deployment verification

The transfer-idempotency race handling, Flyway migrations V2/V3, MPIN hashing/lockout, account lookup, and ledger history need integration tests against PostgreSQL and Redis. The full local Spring context test could not connect to PostgreSQL in the checked environment. CI is configured with PostgreSQL and Redis; verify a successful run and run the deployed flow before claiming it is verified.

### Medium: availability and operational controls

Redis is required for the MPIN failure limiter. Redis outages can block transfer verification. Add operational alerts, backups, restore checks, and monitoring for database/Redis readiness, bootstrap use, repeated MPIN failures, and failed transaction requests. Define retention and cleanup for idempotency and audit records without weakening duplicate protection.

### Medium: administration and recovery

The first-admin bootstrap is opt-in and must be disabled after use. Ensure deployment secrets are private, bootstrap is never enabled on an already exposed instance, and admin/auditor access is tested. Account recovery, MPIN reset, and administrative changes need auditable workflows.

### Medium: privacy and enumeration

Recipient lookup returns an account holder name for a valid account ID. Apply abuse monitoring and consider throttling/enumeration controls before exposing the service publicly. Avoid logging MPINs, passwords, cookies, or full secrets.

## Verification record

- Frontend lint and production build passed in the checked development environment on 2026-11-13.
- The backend role-boundary unit test passed.
- The full local backend test suite had one application-context error because PostgreSQL was unreachable; this is not a passing full-suite result.
- No live end-to-end deployment verification is claimed by this document.

## Before any production use

Resolve the CSRF issue, implement MPIN recovery and stronger step-up controls, complete migration-backed integration and concurrency tests, verify least-privilege database grants and backups, add operational monitoring, and perform a security review. Keep this prototype restricted to synthetic data until those items are complete.

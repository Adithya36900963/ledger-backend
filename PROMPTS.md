# AI development log

AI assistance is allowed for this assessment. These entries are transparent retrospectives of prompts in the project conversation. The original message timestamps were not preserved, so each date below is the date this entry was recorded, not a claim that the prompt or coding happened on that date. I will keep adding entries as work happens. Do not backdate commits or claim unverified behavior.

## 2026-11-10 — Add auditor access-boundary tests (retrospective)

- **What I asked:** “first priority is what they want like coading and files” — apply the assessment requirements to project code and files first.
- **What the tool produced:** Three service-level tests assert that auditors cannot open accounts, withdraw, or read account-holder-only history. The stale role comment was updated to include auditors.
- **What was wrong:** The prior assistant response emphasized profile/submission tasks instead of making a code-level improvement.
- **What I changed instead:** Added executable authorization tests and kept the scope on implementation. The focused Maven tests passed.
- **Commit:** [404f7c7 — verify auditor access boundaries](https://github.com/Adithya36900963/ledger-backend/commit/404f7c7).

## 2026-11-10 — Enforce admin-created account-holder and auditor access (retrospective)

- **What I asked:** Admin should create account holders and auditors, and all roles should be able to log in.
- **What the tool produced:** Role-specific authentication routes, admin-only registration, role validation, account verification, and backend authorization rules.
- **What was wrong:** A frontend role selector by itself would not enforce access control, and the earlier role filter comment did not account for auditors.
- **What I changed instead:** Kept role checks in Spring Security and service methods, added an admin bootstrap path, and corrected the stale comment. The frontend route is not treated as the security boundary.
- **Commit:** [362ddcb — role-aware authentication and verification](https://github.com/Adithya36900963/ledger-backend/commit/362ddcb).

## 2026-11-10 — Implement ledger postings and activity reads (retrospective)

- **What I asked:** Build LedgerLine around money movement with a provable ledger and separate deposit, withdrawal, and transfer services.
- **What the tool produced:** Transactional posting logic writes the paired ledger entries and updates balances; role checks constrain who may post each operation.
- **What was wrong:** An operation that updated only a balance would not leave a balanced, auditable ledger trail.
- **What I changed instead:** Post both sides of each movement in one database transaction and expose scoped holder history plus auditor/admin read APIs.
- **Commit:** [75d5956 — implement double-entry ledger operations](https://github.com/Adithya36900963/ledger-backend/commit/75d5956).

## 2026-11-10 — Add admin bootstrap, audit events, and health checks (retrospective)

- **What I asked:** Complete admin onboarding, admin/auditor audit-log access, bootstrap, and deployment health/readiness work.
- **What the tool produced:** An opt-in first-admin bootstrap, auditable user and ledger actions, read-only audit endpoints, and liveness/readiness endpoints.
- **What was wrong:** It would be misleading to claim a working live admin flow without a configured deployment and database, or to assume database role grants that were not verified.
- **What I changed instead:** Kept bootstrap disabled by default, documented the environment requirements and current schema-management limits, and did not add speculative database grants. Live end-to-end behavior remains unverified.
- **Commit:** [bb9a0f3 — add admin bootstrap, audit, and health APIs](https://github.com/Adithya36900963/ledger-backend/commit/bb9a0f3).

## 2026-11-10 — Document the checked database setup (retrospective)

- **What I asked:** Complete the project while accounting for the discovered PostgreSQL permission/bootstrap behavior.
- **What the tool produced:** A README with safe bootstrap configuration, route overview, and a note that the repository currently uses Hibernate schema updates and has no checked-in versioned migration or DB-role scripts.
- **What was wrong:** Earlier assumptions about PostgreSQL role membership and permission to administer a different role were incorrect; a wider grant would have hidden the real issue.
- **What I changed instead:** Documented the actual repository state and avoided automatic role grants. Migration and least-privilege provisioning still need an explicit, reviewed implementation.
- **Commit:** [592c758 — add Ledger backend setup guide](https://github.com/Adithya36900963/ledger-backend/commit/592c758).

## 2026-11-11 — Merge local application settings safely

- **What I asked:** Add my updated `application.yml` settings to the backend.
- **What the tool produced:** Merged the multipart limits, requested local CORS origins, and logging levels into the existing configuration.
- **What was wrong:** The pasted YAML repeated top-level `spring` and `app` keys, which can cause settings to be overwritten, and included plaintext database/JWT credential values.
- **What I changed instead:** Kept one `spring` section and one `app` section, used the existing environment-variable placeholders for secrets, and added the non-secret settings. I advised rotating the pasted credentials.
- **Commit:** [0b67b7c — add multipart limits and local CORS origins](https://github.com/Adithya36900963/ledger-backend/commit/0b67b7c).

## 2026-11-13 — Protect transfers with MPIN and idempotency (retrospective)

- **What I asked:** Require a transfer amount and MPIN confirmation, and make retries unable to create more than one transaction. The transfer history must also identify the recipient account.
- **What the tool produced:** Recipient lookup and transfer-history APIs, server-side amount validation, one-time MPIN setup with BCrypt storage and Redis attempt limiting, and a database-backed idempotency record written atomically with the ledger posting.
- **What needed correction:** The first UI draft asked for MPIN directly in the chat composer. I clarified that Pay should open a separate secure confirmation step, and that entering a recipient account in Make a transaction should open that recipient's chat.
- **What I changed instead:** The UI opens the recipient chat first, takes the amount in the bottom composer, and asks for MPIN in a confirmation dialog. The client sends a per-attempt idempotency key; the backend returns the original result for a matching retry and rejects reuse with different transfer details.
- **Verification and limits:** Frontend lint/build and backend role-access tests passed in the local environment. The full backend test suite could not connect to local PostgreSQL, so database-backed behavior still requires a successful CI/deployment run. MPIN recovery/reset and live end-to-end verification remain unfinished.
- **Commits:** [bb38f22 — server-verified MPIN](https://github.com/Adithya36900963/ledger-backend/commit/bb38f22), [993b981 — transfer idempotency and recipient lookup](https://github.com/Adithya36900963/ledger-backend/commit/993b981).

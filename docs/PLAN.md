# LicenseLeak AI — Implementation Plan

**Status:** MVP implementation is in place; local case browsing was confirmed against PostgreSQL. Automated test and deployment verification remain.
**Owner focus:** The current developer is coordinating and implementing the combined Person 1, Person 2, and shared MVP work.

## Project goal

Build a small demo that helps a creator review fictional licence terms and usage evidence, consider a structured analysis, make an explicit decision, and (after a valid approval and actionable fee quote exist) request a PayPal Sandbox invoice. The product is an assistive review tool. It does not determine that infringement occurred or provide legal advice.

## Agreed project decisions

- One modular Spring Boot application, not microservices.
- Java 21, Maven, PostgreSQL, Spring Data JPA, and Flyway.
- Next.js, React, and TypeScript frontend.
- Gemini analysis follows a provider interface; a deterministic mock is the default for the demo, and an optional Gemini adapter is configured locally.
- PayPal REST Invoicing API Sandbox for the payment demonstration.
- USD is the agreed demo invoice currency. The current fee rate is $10 per billable calendar day. Billable days begin the day after the contract's final valid date and include the date the out-of-contract ad is received. The rate is configurable.
- PostgreSQL is installed and started manually for local development; do not use Docker. The deployed service connects to a hosted PostgreSQL database using environment variables.
- Deployment target is Render; deployment requires the team's Render account, hosted database, and configured secrets.
- Person 2's work is developed on the `animesh` branch and integrated with the team through the agreed Git workflow.
- Use fictional cases, contracts, and evidence only.
- Java tests go in Maven's conventional `backend/src/test/java` source tree. Frontend and end-to-end tests go under top-level `/tests`.

## MVP boundaries

### In scope

- List and inspect preloaded fictional cases, contracts, and evidence.
- Display structured analysis and uncertainty from the selected provider (mock by default; optional Gemini).
- Persist creator approval or rejection against the exact analysis and fee quote reviewed.
- Enforce approval and freshness on the backend.
- Calculate each out-of-contract ad independently with `BigDecimal`: billable days run from the day after contract expiry through and including the ad receipt date. The default rate is configurable at $10 USD per ad per day.
- Create a PayPal Sandbox invoice only for an approved, current, actionable fee quote.
- Verify and deduplicate PayPal webhook events before updating invoice status.
- Record safe audit events; never log secrets.

### Out of scope for the first milestone

- Live social-media monitoring or scraping.
- Real customer data, real accusations, or legal conclusions.
- Training a custom AI model or making legal determinations.
- Live Gemini analysis as a requirement for the demo path; the mock provider supports offline rehearsal.
- Production payments or a complex user login system.

## Suggested milestones

### 0. Local development baseline - complete

Java 21 and Maven work locally. PostgreSQL is connected, Flyway seeded the fictional cases, and both case list and case detail endpoints were confirmed working.

### 1. Agree on shared contracts - implemented

The shared DTO, versioning, and analysis/quote/decision/invoice relationships are implemented. Contract expiry and ad receipt are calendar dates; the receipt date is included and the final contract date is excluded from billable days. Missing dates prevent an actionable quote.

### 2. Analysis and licensing intelligence - implemented

The Gemini adapter, structured output, source references, and uncertainty policy are implemented behind a provider interface. The model does not calculate fees or interact with PayPal. The default mock supports offline demos.

### 3. Shared frontend vertical slice - implemented

The Next.js frontend lists cases, presents details and analysis, handles decisions and invoices, and shows audit history with loading/error states and an assistive-tool disclaimer.

### 4. Persist analysis and implement deterministic fee quotes - implemented

Flyway/JPA persist analysis versions and itemized fee quotes. The calculator is configured by `LICENSELEAK_FEE_DAILY_RATE` (default `10.00`) and `LICENSELEAK_FEE_CURRENCY` (default `USD`). It computes the date difference with `BigDecimal` amounts and makes quotes non-actionable when inputs or analysis are uncertain.

### 5. Creator decision and approval enforcement - implemented

Persist approve/reject against the exact analysis and fee quote reviewed. Reject stale versions, missing/non-actionable quotes, and rejected cases for invoice eligibility. The backend is the source of truth for approval.

### 6. PayPal Sandbox invoices, audit, and webhooks - implemented; Sandbox credentials and end-to-end verification remain

The PayPal guide covers Sandbox setup, server-only credentials, OAuth, invoice creation, idempotency, timeout reconciliation, webhook verification/deduplication, and audit records. A draft or sent invoice is never treated as paid.

### 7. Tests, deployment, and demo rehearsal - in progress

The unit suites cover mismatch/uncertainty, date fees, stale decisions, missing approval, duplicate invoice requests, and webhook payment status. Configure Render and secrets, then rehearse a demo path that uses the mock provider.

## PostgreSQL collaboration and deployment

- Each developer can run a local PostgreSQL installation manually and use the same Flyway migrations and fictional seed data.
- Keep connection settings outside source code. Use a documented environment-variable template with placeholder values; do not commit real passwords.
- For deployment, follow [the Render guide](RENDER_DEPLOY_GUIDE.md) and configure the hosted PostgreSQL connection and credentials in the hosting environment. Do not depend on a developer's laptop database.
- The app should use the same database engine and migration scripts locally and in deployment. Keep Hibernate in validation mode so Flyway owns schema changes.
- Decide whether the team needs a shared development database or can work with separate local databases. If shared, restrict access and do not put credentials in Git or chat.
- Before implementation, confirm the selected hosted database is compatible with the chosen deployment target and available within the team's account/budget.

## Testing approach

Keep tests easy to discover:

- Java unit, MVC, and integration tests: `backend/src/test/java` so Maven/Surefire finds them by default.
- Frontend component tests: `frontend/tests/` (kept within the frontend package for Windows/Vitest path resolution).
- Browser end-to-end tests: `tests/e2e/`.

External Gemini and PayPal calls are mocked in unit tests. Latest verification: 11 backend tests and 3 frontend tests pass; frontend typecheck, lint, and production build pass. Render deployment and live Sandbox rehearsal need the team's account credentials.

## Decisions still needed from the team

- Whether local databases are separate per developer or shared.
- Hosted PostgreSQL and Render service details and credentials.
- Person 1/team review of the implemented analysis DTO and prompt policy.
- Team confirmation whether the $10 USD per-ad daily rate needs to change; it can be changed through configuration.

## Safety and correctness rules

- No approval means no invoice; rejection blocks invoice creation.
- The backend never trusts frontend approval state.
- Approval must reference the exact analysis and fee quote reviewed. Changed source data invalidates stale approval.
- Never use `float` or `double` for money; never invent a fee.
- Only verified provider information can update payment status.
- Use persistent idempotency/business keys and do not blindly retry uncertain PayPal requests.
- Never log or commit credentials, API keys, or secrets.

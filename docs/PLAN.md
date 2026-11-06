# LicenseLeak AI — Implementation Plan

**Status:** Discussion draft; review with both teammates before implementation.  
**Owner focus:** Person 2 — backend, persistence, approvals, payments, and integration.

## Project goal

Build a small demo that helps a creator review fictional licence terms and usage evidence, consider a structured analysis, make an explicit decision, and (after a valid approval and actionable fee quote exist) request a PayPal Sandbox invoice. The product is an assistive review tool. It does not determine that infringement occurred or provide legal advice.

## Agreed project decisions

- One modular Spring Boot application, not microservices.
- Java 21, Maven, PostgreSQL, Spring Data JPA, and Flyway.
- Next.js, React, and TypeScript frontend.
- Gemini analysis is owned by Person 1. Person 2 will integrate through an agreed interface and will not recreate the AI logic.
- PayPal REST Invoicing API Sandbox for the payment demonstration.
- USD is the agreed demo invoice currency. The sample contract wording and fee schedule remain undecided; do not seed or calculate a fee until the team agrees on the rule.
- PostgreSQL is installed and started manually for local development; do not use Docker. The deployed service connects to a hosted PostgreSQL database using environment variables.
- Deployment target is Render, subject to confirming a compatible PostgreSQL service and project limits.
- Person 2's work is developed on the `animesh` branch and integrated with the team through the agreed Git workflow.
- Use fictional cases, contracts, and evidence only.
- Java tests go in Maven's conventional `backend/src/test/java` source tree. Frontend and end-to-end tests go under top-level `/tests`.

## MVP boundaries

### In scope

- List and inspect preloaded fictional cases, contracts, and evidence.
- Display structured analysis and uncertainty from Person 1's module (a mock can be used before integration).
- Persist creator approval or rejection against the exact analysis and fee quote reviewed.
- Enforce approval and freshness on the backend.
- Calculate fees deterministically with `BigDecimal` only after the sample rule is agreed.
- Create a PayPal Sandbox invoice only for an approved, current, actionable fee quote.
- Verify and deduplicate PayPal webhook events before updating invoice status.
- Record safe audit events; never log secrets.

### Out of scope for the first milestone

- Live social-media monitoring or scraping.
- Real customer data, real accusations, or legal conclusions.
- Building Gemini prompts or analysis logic owned by Person 1.
- Fee calculation before a signed-off sample contract rule exists.
- Production payments or a complex user login system.

## Suggested milestones

### 0. Team contract review

Review this plan, `API_CONTRACTS.md`, and `USER_STORIES.md` together. Agree on the API fields shared with Person 1 and the initial data relationships before writing migrations.

### 1. Backend foundation and case browsing

Create the Spring Boot Maven project, configure PostgreSQL through environment variables, enable Flyway migrations, and use Hibernate schema validation. Add fictional case/contract/evidence persistence and the case list/detail API. Document manual setup for both teammates and deployment configuration.

**Progress:** Starter project, initial fictional case migration, and `GET /api/v1/cases` plus `GET /api/v1/cases/{caseId}` are in place. Local build and database startup still need to be checked after Java and PostgreSQL are available in the terminal.

### 2. Analysis and decision integration

Integrate Person 1's agreed analysis interface (or a clearly labeled mock while that work is in progress). Persist analysis versions and creator approve/reject decisions. Verify that decisions point to the exact analysis and quote reviewed, and become stale when source data or quote changes.

### 3. Fee quote

Only after team agreement on contract wording, required inputs, rounding, and USD: implement and document the deterministic rule using `BigDecimal`. Ambiguous or missing inputs must produce no actionable quote.

### 4. PayPal Sandbox and audit

Create invoices only after a valid persisted approval and actionable quote. Make duplicate requests safe, handle timeouts as uncertain, verify webhook authenticity, deduplicate event IDs, and record status transitions. Invoice creation or sending must not be shown as payment received.

### 5. Deployment and rehearsal

Configure the backend, frontend, and hosted PostgreSQL service on Render using environment variables. Rehearse the fictional demo flow and keep a known-good path if external APIs fail.

## PostgreSQL collaboration and deployment

- Each developer can run a local PostgreSQL installation manually and use the same Flyway migrations and fictional seed data.
- Keep connection settings outside source code. Use a documented environment-variable template with placeholder values; do not commit real passwords.
- For deployment, configure the hosted PostgreSQL connection and credentials in the hosting environment. Do not depend on a developer's laptop database.
- The app should use the same database engine and migration scripts locally and in deployment. Keep Hibernate in validation mode so Flyway owns schema changes.
- Decide whether the team needs a shared development database or can work with separate local databases. If shared, restrict access and do not put credentials in Git or chat.
- Before implementation, confirm the selected hosted database is compatible with the chosen deployment target and available within the team's account/budget.

## Testing approach

Keep tests easy to discover:

- Java unit, MVC, and integration tests: `backend/src/test/java` so Maven/Surefire finds them by default.
- Frontend component tests: `tests/frontend/`.
- Browser end-to-end tests: `tests/e2e/`.

When tests are eventually run, check the command output to confirm the expected tests were discovered. External Gemini and PayPal calls should be mocked in automated tests. Testing is a later implementation activity; this document does not claim any tests have run.

## Decisions still needed from the team

- Exact initial database relationships and constraints.
- Person 1's analysis and uncertainty fields and shared DTO boundary.
- Sample contract text, fee rule, required inputs, and rounding behavior.
- Exact API response fields and final HTTP status codes.
- Whether local databases are separate per developer or shared.
- Hosted PostgreSQL choice and deployment connection details.
- PayPal webhook event types and verification steps, to be checked against official PayPal documentation during implementation.

## Safety and correctness rules

- No approval means no invoice; rejection blocks invoice creation.
- The backend never trusts frontend approval state.
- Approval must reference the exact analysis and fee quote reviewed. Changed source data invalidates stale approval.
- Never use `float` or `double` for money; never invent a fee.
- Only verified provider information can update payment status.
- Use persistent idempotency/business keys and do not blindly retry uncertain PayPal requests.
- Never log or commit credentials, API keys, or secrets.

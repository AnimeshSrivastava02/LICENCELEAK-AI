# LicenseLeak AI — Project Blueprint
**PayPal AI Hackathon | Team handoff and implementation guide**  
**Status:** Planning baseline — no implementation is claimed to be complete.

## 1. Project summary

LicenseLeak AI helps creators review whether a brand's use of licensed content may fall outside the agreed licence. It compares fictional contract terms with fictional usage evidence, explains possible mismatches with clause/evidence references, calculates a potential additional fee using an explicit contract rule, and lets the creator approve or reject before a PayPal Sandbox invoice can be created.

The system is an assistive review tool, not a legal decision-maker. It must not claim that infringement is proven. Ambiguous cases must be surfaced for human review; the exact uncertainty-handling policy is intentionally deferred to the teammate assigned to AI/licensing.

## 2. Locked decisions

- Frontend: Next.js, React, TypeScript.
- UI/styling: Tailwind CSS and shadcn/ui.
- Backend: Java 21 and Spring Boot; use a stable compatible Spring Boot patch release when creating the project.
- Build: Maven.
- Database: PostgreSQL.
- Persistence: Spring Data JPA.
- Schema migrations: Flyway. Hibernate/JPA validates the schema; it should not silently mutate production schema.
- Local database: Docker Compose runs PostgreSQL only. Frontend and backend run locally during development.
- AI: Gemini API. Exact model/version is chosen by the AI-assigned teammate and recorded in the implementation notes.
- Payments: PayPal REST Invoicing API using Sandbox for the demo.
- Deployment: Render only.
- Demo authentication: preconfigured demo creator account; no complex sign-up/auth flow for MVP.
- Demo input: preloaded fictional contracts and matching fictional evidence. No live social-media scraping/monitoring in MVP.
- Demo scenarios: at least one possible mismatch, one apparently permitted use, and one ambiguous case.
- Currency/contract fee details: the team will settle the sample contract rules and currency. The rule must be explicit in the contract. Calculation is deterministic application code, not an LLM-generated amount.
- Invoice gate: explicit creator approval is mandatory before invoice creation. Rejection must prevent invoice creation.
- Audit trail: record analysis, evidence references, fee calculation, approval/rejection, invoice creation attempts/IDs, and verified invoice status changes. Never log API secrets.
- Testing is a separate top-level folder and separate concern from production source.
- Team split: Person 1 owns AI/licensing intelligence; Person 2 owns platform/payments/backend integration. Both contribute to UI, with ownership of specific screens agreed between them.
- Avoid microservices. Build one modular monolith with one backend and one PostgreSQL database.

## 3. MVP user journey

1. Creator opens the demo and selects a preloaded fictional case.
2. Frontend requests analysis from the backend.
3. Backend loads the contract and evidence and invokes the AI/licensing module.
4. AI/licensing module returns structured analysis: outcome category, explanation, relevant clause references, evidence references, uncertainty notes, and the data needed for fee calculation.
5. Deterministic fee logic calculates a potential amount only when the contract rule and required inputs are clear and valid. Otherwise, the case is flagged for human review; do not invent a fee.
6. Frontend displays the analysis, supporting references, limitations, and potential fee.
7. Creator explicitly approves or rejects. Approval and rejection are persisted on the backend.
8. Only an approved case may request PayPal Sandbox invoice creation. Backend re-checks the persisted approval; frontend state alone is never sufficient.
9. Backend stores the PayPal invoice identifier and current known status, with an audit event.
10. Verified PayPal webhook events update invoice status idempotently. An invoice being created or sent must not be presented as paid.

## 4. High-level architecture

```text
Browser (Next.js / React / TypeScript)
        |
        | HTTPS / JSON REST
        v
Spring Boot modular monolith
  ├── Case & demo data module
  ├── Contract/evidence module
  ├── Licensing analysis orchestration
  ├── Fee calculation module (deterministic)
  ├── Creator review & approval module
  ├── PayPal invoicing + webhook module
  └── Audit trail module
        |
        ├── Gemini API (server-side only)
        ├── PayPal REST API (Sandbox; server-side only)
        └── PostgreSQL (Spring Data JPA + Flyway)

Testing (separate top-level /tests)
  ├── backend unit / MVC / integration tests
  ├── frontend component tests
  └── end-to-end browser tests
```

### Architecture rules

- Modular monolith, not microservices.
- Keep API keys and PayPal client secrets on the server only.
- LLM output is untrusted input: validate against a typed schema and business rules before use.
- The LLM may extract/interpret terms, but must not decide an unsupported fee amount.
- All approval and invoice authorization checks happen on the backend.
- Use a persisted, unique idempotency/business key to prevent duplicate invoices on retry.
- Verify PayPal webhook authenticity before applying status changes; handle duplicate webhook deliveries safely.
- Do not mark invoices paid just because creation succeeded. Use verified status/events and reconcile status as supported by PayPal.
- Keep the demo dataset fictional and label it clearly.
- Log useful audit events, but redact secrets and unnecessary personal data.

## 5. Proposed repository and folder structure

This is a monorepo. Keep tests in a dedicated top-level `/tests` directory as requested. Test source files should not be scattered through the production folders. If a Java build tool requires test source roots, configure Maven to use the external test directory or keep the conventional test source roots only if the team explicitly chooses that trade-off; the preferred structure is shown below and must be verified against Maven/Spring Boot tooling before implementation.

```text
licenseleak-ai/
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml              # PostgreSQL only
├── docs/
│   ├── PROJECT_BLUEPRINT.md
│   ├── ARCHITECTURE.md
│   ├── API_CONTRACTS.md
│   ├── DATA_MODEL.md
│   ├── PAYPAL_WORKFLOW.md
│   └── TEAM_WORKFLOW.md
├── frontend/
│   ├── package.json
│   ├── next.config.ts
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   │   ├── ui/                 # shadcn/ui generated components
│   │   │   └── shared/
│   │   ├── features/
│   │   │   ├── cases/
│   │   │   ├── analysis/
│   │   │   ├── approvals/
│   │   │   └── invoices/
│   │   ├── lib/
│   │   │   ├── api-client.ts
│   │   │   └── schemas.ts
│   │   └── types/
│   └── public/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/licenseleak/
│       │   ├── LicenseLeakApplication.java
│       │   ├── config/
│       │   ├── common/
│       │   │   ├── errors/
│       │   │   ├── validation/
│       │   │   └── security/
│       │   ├── cases/
│       │   │   ├── api/
│       │   │   ├── application/
│       │   │   ├── domain/
│       │   │   └── infrastructure/
│       │   ├── licensing/
│       │   │   ├── api/
│       │   │   ├── application/
│       │   │   ├── domain/
│       │   │   └── infrastructure/
│       │   ├── fees/
│       │   │   ├── application/
│       │   │   └── domain/
│       │   ├── approvals/
│       │   │   ├── api/
│       │   │   ├── application/
│       │   │   └── domain/
│       │   ├── invoices/
│       │   │   ├── api/
│       │   │   ├── application/
│       │   │   ├── domain/
│       │   │   └── infrastructure/
│       │   └── audit/
│       │       ├── application/
│       │       ├── domain/
│       │       └── infrastructure/
│       └── resources/
│           ├── application.yml
│           └── db/migration/       # Flyway SQL migrations
├── tests/
│   ├── README.md
│   ├── backend/
│   │   ├── unit/
│   │   ├── mvc/
│   │   └── integration/
│   ├── frontend/
│   │   └── components/
│   └── e2e/
│       └── playwright/
└── .github/
    └── workflows/
        └── ci.yml
```

### Testing-folder implementation note

A separate top-level testing folder is the desired repository organization. Java's Maven/Surefire defaults expect tests under `backend/src/test/java`; placing them under `/tests/backend` requires explicit Maven configuration and may be less conventional. Before scaffolding, validate a small Maven configuration for the external test source root and ensure CI discovers tests. If that becomes fragile, agree explicitly on a compromise: keep Java tests in Maven's conventional test source root while retaining frontend/e2e tests in `/tests`. Do not silently assume tests are running—confirm test discovery in CI.

## 6. Backend module responsibilities

- `cases`: preloaded fictional scenarios, case retrieval, contract/evidence associations.
- `licensing`: analysis orchestration, Gemini adapter, schema validation, clause/evidence references, uncertainty fields.
- `fees`: deterministic calculation from explicit contract rules and validated inputs. Use decimal-safe money types (`BigDecimal` in Java), never floating-point arithmetic for currency.
- `approvals`: persist creator approval/rejection and enforce allowed state transitions.
- `invoices`: PayPal client adapter, invoice creation, idempotency, webhook verification/processing, invoice status.
- `audit`: append meaningful audit events with actor/action/time/relevant record IDs; avoid secrets.
- `common`: shared API errors, validation, configuration, and common types only where genuinely shared.

Do not let the AI module call PayPal directly. It should produce analysis; the application workflow handles approval and payments.

## 7. Initial REST API contract (proposed)

All endpoints are under `/api/v1`. Exact DTOs and status codes must be agreed in `docs/API_CONTRACTS.md` before parallel coding.

| Method and path | Purpose |
|---|---|
| `GET /cases` | List preloaded demo cases |
| `GET /cases/{caseId}` | Retrieve case details, contract summary, and evidence metadata |
| `POST /cases/{caseId}/analysis` | Run or retrieve analysis for the case |
| `GET /cases/{caseId}/analysis` | Retrieve the latest persisted analysis |
| `POST /cases/{caseId}/decision` | Submit `APPROVE` or `REJECT`; backend validates current state |
| `POST /cases/{caseId}/invoice` | Create Sandbox invoice only if a persisted valid approval exists |
| `GET /cases/{caseId}/invoice` | Retrieve known invoice state/identifier |
| `POST /webhooks/paypal` | Receive PayPal webhook; verify authenticity and process idempotently |
| `GET /cases/{caseId}/audit` | Retrieve the case's audit history for the demo UI |

### Shared response expectations

- JSON with stable field names and explicit enums, not free-form status strings.
- Use a consistent error shape with `code`, safe `message`, and optional `details`/`requestId`.
- Analysis result should include `outcome` (for example `POSSIBLE_MISMATCH`, `PERMITTED`, `AMBIGUOUS`), `summary`, `clauseReferences`, `evidenceReferences`, `uncertainties`, and optional `feeQuote`.
- `feeQuote` should include amount, currency, calculation basis/rule reference, and whether it is actionable. It must be absent or non-actionable when inputs/rules are unclear.
- Approval decision includes decision, timestamp, and case/analysis version reviewed.
- Invoice response includes provider invoice ID, current known status, and safe link if provided by PayPal. Never expose credentials.
- The API must reject invoice creation without an approved, current fee quote. The backend must independently enforce this.

## 8. Data model (initial proposal)

Tables/entities:
- `demo_case`: id, display name, scenario type, status, created/updated timestamps.
- `license_contract`: id, case id, fictional party labels, contract text or reference, parsed rule fields, currency, version.
- `usage_evidence`: id, case id, evidence type, description, source/reference label, observed attributes.
- `analysis_run`: id, case id, contract version, evidence version/hash, model/version metadata, structured output, created timestamp.
- `fee_quote`: id, analysis id, amount, currency, rule reference, calculation inputs, validation status.
- `creator_decision`: id, case id, analysis id/version, decision, decided timestamp, reason optional.
- `invoice_record`: id, case id, decision id, provider invoice ID, status, idempotency key, created/updated timestamps.
- `audit_event`: id, case id, actor label, event type, timestamp, safe structured details.

Use database constraints/unique indexes for invoice idempotency. Keep only the fields needed for the demo. Exact schema and relationships are to be finalized before Flyway migrations are written.

## 9. Invoice and approval state rules

Suggested case workflow states:
- `READY_FOR_ANALYSIS`
- `ANALYSIS_COMPLETE`
- `NEEDS_HUMAN_REVIEW`
- `APPROVED`
- `REJECTED`
- `INVOICE_CREATED`
- `INVOICE_SENT` (only if provider status supports it)
- `PAID` (only after verified provider status/event)
- `INVOICE_FAILED`

Rules:
1. Approval must reference the exact analysis/fee quote version reviewed.
2. If contract, evidence, analysis, or fee quote changes after approval, approval becomes stale and invoice creation is blocked until re-approval.
3. Rejecting a case must block invoice creation.
4. Backend checks persisted approval and quote validity immediately before the PayPal call.
5. Use a stable idempotency key/business uniqueness constraint; repeated requests must return the existing invoice/result rather than create a second invoice.
6. If PayPal times out after receiving a create request, do not blindly retry without reconciliation/idempotency safeguards.
7. Verify webhook signatures/authenticity using PayPal's supported mechanism; deduplicate event IDs.
8. A webhook is not trusted merely because it reached the endpoint.
9. Record provider response/status transitions in the audit trail.

## 10. Testing strategy (kept separate)

### Backend unit tests
- Fee calculations for valid rules, missing values, boundary values, rounding, and unsupported/ambiguous rules.
- Approval state transitions and stale-approval rejection.
- Idempotency logic.
- AI response schema validation and handling malformed/unexpected model output.

### Backend MVC/API tests
- Request validation and error responses.
- Invoice endpoint rejects no approval, rejected case, stale approval, missing fee quote, and invalid currency/rule.
- Approval endpoint persists decision.
- Webhook endpoint rejects invalid/unverified events.

### Backend integration tests
- Spring Data JPA + Flyway against PostgreSQL (prefer Testcontainers once the baseline works).
- Constraints, persistence, and transactions.
- PayPal/Gemini adapters mocked or stubbed in automated tests; do not depend on live external services in CI.

### Frontend tests
- Case list/detail render correctly.
- Analysis states: possible mismatch, permitted, ambiguous.
- Approve/reject UI and invoice disabled before approval.
- API errors/loading states and accessible controls.

### End-to-end tests
- Select demo case → view analysis → approve → invoice creation mocked or Sandbox-configured.
- Reject → invoice creation is blocked.
- Duplicate invoice request does not create duplicates.
- Invoice status updates only after a verified webhook/status check.

### Minimum test gates
- `mismatch`, `permitted`, and `ambiguous` demo cases all behave as intended.
- Rejection prevents invoice creation.
- No approval means no invoice.
- Duplicate retries do not duplicate invoices.
- Invalid webhook does not change invoice state.
- Failed Gemini/PayPal calls show safe errors and are auditable.
- CI visibly discovers and runs the intended test suites.

## 11. Team ownership and collaboration

### Person 1 — AI and licensing intelligence
Owns:
- Gemini API adapter and prompt/schema design.
- Contract-term extraction/normalization from preloaded contract text.
- Evidence-to-clause comparison.
- Structured analysis output and references to the source clause/evidence.
- AI uncertainty handling policy (deferred until this person begins their task).
- AI response validation, malformed-output handling, and AI/licensing tests.
- Collaborates on UI components that display analysis and evidence.

Does not own:
- PayPal invoice creation.
- Final fee arithmetic in the LLM.
- Approval enforcement as the source of truth.

### Person 2 — Platform, payments, and integration
Owns:
- Spring Boot app foundation, PostgreSQL, JPA, Flyway, and demo-case persistence.
- Approval/rejection persistence and backend enforcement.
- Deterministic fee calculation implementation, based on the agreed explicit sample contract rule.
- PayPal Sandbox invoice client, idempotency, webhook verification, invoice status, and audit trail.
- Backend API contracts, integration tests, and deployment configuration.
- Collaborates on UI components for case selection, decisions, invoice status, and audit history.

Does not own:
- All frontend work alone.
- AI conclusions or unvalidated model output.

### Shared responsibilities
- Both contribute to frontend work and agree on screen ownership.
- Agree DTOs/enums and API paths before parallel coding.
- Use Git branches and pull requests; avoid committing secrets.
- Merge a minimal vertical slice early instead of waiting until all modules are finished.
- Both review the end-to-end demo flow and rehearse the presentation.
- One teammate should not change a shared DTO/database/API contract without notifying the other.

## 12. Integration workflow

1. Create one GitHub monorepo.
2. Commit this blueprint and a concise README.
3. Create `docs/API_CONTRACTS.md` with example JSON request/response payloads and enums.
4. Agree the initial database entity IDs and module interface.
5. Build a small working vertical slice with seeded demo data and a mock AI response.
6. Integrate the UI with the real backend contract early.
7. Add Gemini and PayPal behind interfaces/adapters after the core flow works.
8. Add Sandbox credentials through local environment variables and Render environment settings.
9. Run all tests and a complete demo rehearsal before submission.
10. Keep a known-good demo path in case external APIs are unavailable during presentation.

## 13. Environment variables (names only; never commit real values)

Example `.env.example` names:
```text
SPRING_PROFILES_ACTIVE=local
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
GEMINI_API_KEY=
GEMINI_MODEL=
PAYPAL_BASE_URL=
PAYPAL_CLIENT_ID=
PAYPAL_CLIENT_SECRET=
PAYPAL_WEBHOOK_ID=
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

The exact PayPal base URL and webhook verification configuration must follow the current PayPal developer documentation. `NEXT_PUBLIC_` variables are exposed to the browser; only put a public backend URL there, never secrets. Use appropriate Render environment variables for deployment.

## 14. Suggested local development commands (after scaffolding)

- Start database: `docker compose up -d postgres`
- Stop database: `docker compose down`
- Frontend: `cd frontend && npm install && npm run dev`
- Backend: `cd backend && ./mvnw spring-boot:run` (or the Maven command generated by the project)
- Tests: use documented root scripts/commands that run the suites under `/tests`; ensure CI uses the same commands.

Exact service names and commands must match the actual files created; do not copy commands blindly before scaffolding.

## 15. Definition of done for MVP

- Demo creator can select a fictional case and inspect the contract/evidence.
- Analysis shows outcome, explanation, clause/evidence references, and uncertainty without asserting legal certainty.
- Fee is calculated deterministically from an explicit contract rule.
- Ambiguous/missing rules are sent for human review; no invented fee.
- Creator can approve or reject; rejection and missing approval block invoice creation server-side.
- Approved case can create a real PayPal Sandbox invoice and store its identifier/status.
- Verified webhook/status updates are deduplicated and reflected in the UI.
- Audit trail shows meaningful events without exposing secrets.
- Tests are separated from production code, discovered in CI, and cover the minimum gates.
- App is deployed on Render with secrets configured securely.
- Demo clearly labels fictional data and uses no real accusations.

## 16. Open items deliberately not decided yet

- Exact Gemini model/version and AI uncertainty policy: assigned to Person 1 to decide when they begin that work.
- Final sample contract wording, fee schedule, and currency: team will agree before creating seeded contract data.
- Exact DTO schemas, database relationships, and HTTP status codes: finalize in API/data model docs before parallel implementation.
- Exact Spring Boot patch and compatible library versions: choose stable compatible versions at scaffolding time.
- PayPal webhook event types and verification steps: verify against current official PayPal documentation during implementation.
- Render service topology and database plan: confirm at deployment time based on current Render options and project limits.

## 17. First actions

1. Share this document with both teammates and add it to the project’s shared knowledge/context.
2. Create the GitHub repository and agree branch/PR workflow.
3. Assign the Person 1 and Person 2 work briefs below in separate chats.
4. Ask both chats to read this entire blueprint and follow it as the shared source of truth.
5. Before implementation, jointly approve the API contract and data model.
6. Start coding only after both owners agree on the initial interfaces.

# LicenseLeak AI — User Stories

**Status:** Discussion draft. Acceptance criteria describe intended behavior and are not evidence that the features are implemented or tested.

## Demo creator stories

### US-01 — Browse fictional cases

As a demo creator, I want to see the available fictional cases so I can choose one to review.

**Acceptance criteria**

- The case list shows a title and scenario label for each fictional case.
- The data is clearly marked as fictional.
- Selecting a case opens its contract summary and evidence metadata.
- Missing case IDs return a safe not-found response.

### US-02 — Inspect contract and evidence

As a demo creator, I want to inspect the relevant fictional contract and usage evidence so I can understand what the analysis considered.

**Acceptance criteria**

- The case detail includes contract identity/version and evidence references.
- Evidence is labeled and presented as supporting material, not proof of infringement.
- The UI does not claim a legal conclusion.

### US-03 — Review analysis and uncertainty

As a demo creator, I want to see a structured analysis with clause/evidence references and uncertainty so I can make an informed decision.

**Acceptance criteria**

- The analysis outcome is one of `POSSIBLE_MISMATCH`, `PERMITTED`, or `AMBIGUOUS`.
- The result includes a summary, clause references, evidence references, and uncertainty notes when applicable.
- Missing or ambiguous information is visible.
- Person 1 owns the analysis behavior and final response contract; a mock can be used during early integration.
- No unsupported fee is presented as actionable.

### US-04 — Approve or reject the reviewed result

As a demo creator, I want to approve or reject the exact analysis and quote I reviewed so my decision is recorded accurately.

**Acceptance criteria**

- The decision is stored by the backend and references the analysis version and, when applicable, fee quote reviewed.
- The backend checks that referenced records belong to the case and are current.
- A rejection blocks invoice creation.
- The frontend cannot authorize an invoice by presenting local approval state.
- If evidence, contract, analysis, or quote changes, the previous approval no longer authorizes invoicing.

### US-05 — See a deterministic fee quote

As a demo creator, I want to see a fee quote based only on the agreed contract rule so I can review the amount before deciding.

**Acceptance criteria**

- The fee rule and required inputs have been approved by the team before any quote is actionable.
- Monetary arithmetic uses `BigDecimal`, never floating-point types.
- The quote includes amount, USD currency, and a reference to its rule/basis.
- Missing or ambiguous inputs produce no actionable quote and are surfaced for human review.
- The LLM never invents or calculates the final fee.

### US-06 — Create an invoice after valid approval

As a demo creator, I want to request a PayPal Sandbox invoice after approving a current actionable quote.

**Acceptance criteria**

- The backend independently checks for a persisted current approval and quote immediately before the provider request.
- No approval, a rejection, a stale approval, or a non-actionable quote blocks invoice creation.
- Repeated requests do not create duplicate provider invoices.
- A provider timeout is treated as uncertain and is not blindly retried.
- The UI distinguishes invoice draft/sent states from paid status.

### US-07 — View invoice status

As a demo creator, I want to see the latest verified invoice status so I know whether the invoice is still open or paid.

**Acceptance criteria**

- Invoice status is sourced from verified provider responses/events.
- Invoice creation or sending alone is never shown as payment received.
- Duplicate provider events do not create repeated transitions.

### US-08 — Review case history

As a demo creator, I want to see the important events for a case so I can understand what happened.

**Acceptance criteria**

- Audit history includes analysis, quote, decision, invoice attempt/identifier, and verified status changes where applicable.
- Event entries identify time and relevant record IDs.
- Audit data excludes secrets and unnecessary personal data.

## Developer/operator stories

### US-09 — Run the project without Docker

As a teammate, I want clear manual PostgreSQL and environment setup steps so I can run the project on my computer without Docker.

**Acceptance criteria**

- Setup instructions explain installing/starting PostgreSQL and configuring the app connection through environment variables.
- Both teammates can use the same migrations and fictional seed data.
- Credentials are not committed; `.env.example` contains placeholders only.

### US-10 — Deploy with external configuration

As a project maintainer, I want the deployed app to connect to hosted PostgreSQL using deployment environment variables so deployment does not depend on a developer's machine.

**Acceptance criteria**

- Database URL and credentials are configured in the hosting environment, not source code.
- Flyway manages schema changes; Hibernate validates the schema.
- The selected hosted database is compatible with the deployment target and the team's account/budget.

### US-11 — Confirm test discovery

As a teammate, I want Maven to find backend tests by default so I can tell whether backend checks actually ran.

**Acceptance criteria**

- Backend tests are located under `backend/src/test/java`.
- Frontend and end-to-end tests are kept under top-level `/tests`.
- Documented commands and CI report which suites were discovered and run.
- External Gemini and PayPal calls are mocked in automated tests where practical.

## Out of scope for this demo

- Live scraping or monitoring of social media.
- Definitive legal decisions or claims that infringement has been proven.
- Real customer contracts/evidence or production payments.
- AI-generated fee amounts.

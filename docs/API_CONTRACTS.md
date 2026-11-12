# LicenseLeak AI — API Contract Proposal

**Status:** Case, analysis, decision, invoice, audit, and webhook routes are implemented. Gemini and PayPal use configurable adapters; mock adapters are the local defaults.
**Base path:** `/api/v1`  
**Content type:** `application/json`  
**Demo currency:** USD (`USD`). Current fee-rate decision: $10 per billable calendar day. Billable days are the calendar days after the contract's final valid date through the date the out-of-contract ad is received, inclusive of the receipt date. The rate is configurable.

## Shared conventions

- IDs are opaque strings. Timestamps use ISO 8601 UTC, for example `2026-10-31T12:00:00Z`.
- Enums use stable uppercase values. Clients should not infer state from display text.
- Money is represented as a decimal string plus a three-letter currency code; do not send JSON floating-point money values.
- An absent or non-actionable `feeQuote` must never be treated as permission to invoice.
- The backend is the source of truth for approval, quote freshness, and invoice eligibility.
- Response fields below reflect the implemented API. Error responses contain `code` and `message`.

## Endpoints

| Method and path | Purpose |
|---|---|
| `GET /cases` | List fictional demo cases. |
| `GET /cases/{caseId}` | Get case, contract summary, and evidence metadata. |
| `POST /cases/{caseId}/analysis` | Run and persist a new synchronous analysis. |
| `GET /cases/{caseId}/analysis` | Get the latest persisted analysis. |
| `POST /cases/{caseId}/decision` | Persist an approval or rejection tied to the reviewed analysis and quote. |
| `GET /cases/{caseId}/decision` | Retrieve the latest persisted creator decision. |
| `POST /cases/{caseId}/invoice` | Request invoice creation; backend checks persisted current approval and quote. |
| `GET /cases/{caseId}/invoice` | Retrieve the known invoice and status. |
| `POST /webhooks/paypal` | Verify and process a PayPal event idempotently. |
| `GET /cases/{caseId}/audit` | Retrieve safe audit history for the case. |

Missing cases and missing latest analysis/decision/invoice records return `404 Not Found` with the shared error shape. Stale or ineligible decisions return `409 Conflict`. Provider failures return safe `502`/`503` errors or a persisted `UNKNOWN` invoice state when the remote result cannot be confirmed.

## Data shapes

### Case summary — `GET /cases`

```json
{
  "items": [
    {
      "caseId": "case-demo-001",
      "title": "Fictional campaign example",
      "scenarioType": "POSSIBLE_MISMATCH",
      "status": "READY_FOR_ANALYSIS",
      "fictional": true
    }
  ]
}
```

`scenarioType` is fictional demo metadata, not a legal finding. Proposed values: `POSSIBLE_MISMATCH`, `PERMITTED`, `AMBIGUOUS`.

### Case detail — `GET /cases/{caseId}`

```json
{
  "caseId": "case-demo-001",
  "title": "Fictional campaign example",
  "fictional": true,
    "contract": {
    "contractId": "contract-demo-001",
    "version": 1,
    "title": "Illustrative licence draft",
      "summary": "Fictional sample campaign licence terms.",
      "validThrough": "2026-10-01"
  },
  "evidence": [
    {
      "evidenceId": "evidence-demo-001",
      "type": "CAMPAIGN_POST",
      "label": "Fictional sample post",
      "adReceivedOn": "2026-10-04",
      "outOfContract": true
    }
  ]
}
```

### Analysis — `POST /cases/{caseId}/analysis` and `GET /cases/{caseId}/analysis`

Analysis is synchronous. A mock provider is used by default; Gemini is optional.

```json
{
  "analysisId": "analysis-demo-001",
  "caseId": "case-demo-001",
  "version": 1,
  "outcome": "AMBIGUOUS",
  "summary": "The fictional evidence does not resolve one contract condition.",
  "clauseReferences": ["clause-4"],
  "evidenceReferences": ["evidence-demo-001"],
  "uncertainties": ["Required campaign duration is not available."],
  "feeQuote": null,
  "source": "MOCK",
  "modelName": "fictional-scenario-mock",
  "createdAt": "2026-10-31T12:00:00Z"
}
```

Outcomes are `POSSIBLE_MISMATCH`, `PERMITTED`, and `AMBIGUOUS`. A quote may be absent or non-actionable if inputs/rules are incomplete. The calculator uses the configurable rate per ad, default `$10.00 USD`, where `billableDays = max(0, days between contractValidThrough and adReceivedOn)`. Both fields are calendar dates. The contract final date is excluded and the receipt date is included. Do not create an actionable quote if either date is missing or uncertain.

An actionable quote has this shape:

```json
{
  "feeQuoteId": "quote-demo-001",
  "amount": "30.00",
  "currency": "USD",
  "dailyRate": "10.00",
  "billableDays": 3,
  "ruleReference": "DAILY_RATE_PER_BILLABLE_DAY",
  "actionable": true,
  "items": [
    {
      "evidenceId": "evidence-demo-001",
      "contractValidThrough": "2026-10-01",
      "adReceivedOn": "2026-10-04",
      "billableDays": 3,
      "amount": "30.00"
    }
  ]
}
```

The amount is `dailyRate × billableDays` per out-of-contract ad, then summed. Example: valid through Oct 1 and received Oct 4 gives 3 days and `$30.00` at the default rate.

### Decision — `POST /cases/{caseId}/decision`

Request:

```json
{
  "decision": "APPROVE",
  "analysisId": "analysis-demo-001",
  "feeQuoteId": "quote-demo-001"
}
```

For a rejection, use `"decision": "REJECT"`; `feeQuoteId` may be omitted if no quote exists. The backend must confirm the referenced analysis and quote belong to the case and are current. A rejection always blocks invoice creation.

Response:

```json
{
  "decisionId": "decision-demo-001",
  "decision": "APPROVE",
  "analysisId": "analysis-demo-001",
  "feeQuoteId": "quote-demo-001",
  "decidedAt": "2026-10-31T12:05:00Z"
}
```

The decision must reference the latest analysis. An approval may be recorded without a quote for review purposes, but invoice creation requires a current positive actionable quote. A rejection blocks invoicing.

### Invoice — `POST /cases/{caseId}/invoice` and `GET /cases/{caseId}/invoice`

The request body is empty: the backend loads the current persisted decision and quote itself. The client must not be able to authorize an invoice by sending `approved: true`.

Response:

```json
{
  "invoiceRecordId": "invoice-record-001",
  "provider": "MOCK",
  "providerInvoiceId": "MOCK-INVOICE-ID",
  "status": "DRAFT",
  "currency": "USD",
  "approvalDecisionId": "decision-demo-001",
  "invoiceUrl": null,
  "createdAt": "2026-10-31T12:10:00Z",
  "updatedAt": "2026-10-31T12:10:00Z"
}
```

The backend uses a stable persisted idempotency/business key. Repeated requests must not create duplicate provider invoices. If a PayPal request times out and the result is unknown, return a safe pending/uncertain state and reconcile; do not blindly retry. `DRAFT` or `SENT` does not mean paid.

### PayPal webhook — `POST /webhooks/paypal`

The endpoint receives PayPal's provider payload and verification headers. In PayPal mode it verifies authenticity with PayPal before changing invoice state. Event IDs are deduplicated; SENT, PAID, and CANCELLED events update matching invoices. Unmatched events are held and replayed when the invoice ID is later recorded.

### Audit — `GET /cases/{caseId}/audit`

Response:

```json
{
  "items": [
    {
      "eventId": "audit-demo-001",
      "eventType": "DECISION_RECORDED",
      "occurredAt": "2026-10-31T12:05:00Z",
      "details": {"decisionId": "decision-demo-001"}
    }
  ]
}
```

Audit output must not contain credentials, API keys, or unnecessary personal data.

## Error shape

```json
{
  "code": "APPROVAL_REQUIRED",
  "message": "A current creator approval is required before an invoice can be created."
}
```

HTTP status mapping:

| Status | Use |
|---|---|
| `400` | Malformed request or validation error. |
| `404` | Case or requested record not found. |
| `409` | Stale approval, rejected case, duplicate/conflicting state, or invoice not eligible. |
| `502` | Invoice provider failure. An uncertain result is stored as `UNKNOWN` and must be reconciled before retrying. |
| `503` | Analysis provider unavailable. |

## Remaining team decisions

- Whether the local database is shared across developers or kept separate.
- Hosted PostgreSQL, Render, Gemini, and PayPal Sandbox credentials/configuration for the live demo.
- Team review of analysis wording and the initial `$10.00 USD` rate. The rate is configurable.

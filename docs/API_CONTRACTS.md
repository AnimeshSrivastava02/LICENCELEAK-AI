# LicenseLeak AI — API Contract Proposal

**Status:** Discussion draft. Field names and status codes must be agreed with Person 1 before implementation.  
**Base path:** `/api/v1`  
**Content type:** `application/json`  
**Demo currency:** USD (`USD`). Fee rules and amounts remain unset until the team approves the sample contract.

## Shared conventions

- IDs are opaque strings. Timestamps use ISO 8601 UTC, for example `2026-10-31T12:00:00Z`.
- Enums use stable uppercase values. Clients should not infer state from display text.
- Money is represented as a decimal string plus a three-letter currency code; do not send JSON floating-point money values.
- An absent or non-actionable `feeQuote` must never be treated as permission to invoice.
- The backend is the source of truth for approval, quote freshness, and invoice eligibility.
- Exact response fields and HTTP status codes in this draft are proposals.

## Proposed endpoints

| Method and path | Purpose |
|---|---|
| `GET /cases` | List fictional demo cases. |
| `GET /cases/{caseId}` | Get case, contract summary, and evidence metadata. |
| `POST /cases/{caseId}/analysis` | Request a new analysis. Sync/async behavior remains to be agreed with Person 1. |
| `GET /cases/{caseId}/analysis` | Get the latest persisted analysis. |
| `POST /cases/{caseId}/decision` | Persist an approval or rejection tied to the reviewed analysis and quote. |
| `POST /cases/{caseId}/invoice` | Request invoice creation; backend checks persisted current approval and quote. |
| `GET /cases/{caseId}/invoice` | Retrieve the known invoice and status. |
| `POST /webhooks/paypal` | Verify and process a PayPal event idempotently. |
| `GET /cases/{caseId}/audit` | Retrieve safe audit history for the case. |

## Proposed data shapes

### Case summary — `GET /cases`

```json
{
  "items": [
    {
      "caseId": "case-demo-001",
      "title": "Fictional campaign example",
      "scenarioType": "POSSIBLE_MISMATCH",
      "status": "READY_FOR_ANALYSIS"
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
    "title": "Demo licence"
  },
  "evidence": [
    {
      "evidenceId": "evidence-demo-001",
      "type": "CAMPAIGN_POST",
      "label": "Fictional sample post"
    }
  ]
}
```

### Analysis — `POST /cases/{caseId}/analysis` and `GET /cases/{caseId}/analysis`

Proposed response shape; Person 1 owns the final analysis schema.

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
  "createdAt": "2026-10-31T12:00:00Z"
}
```

Outcomes are proposed as `POSSIBLE_MISMATCH`, `PERMITTED`, and `AMBIGUOUS`. A quote may be absent or explicitly non-actionable if inputs/rules are incomplete. The contract and exact amount rule are still open; do not invent an amount.

If a quote is actionable after team approval, the proposed shape is:

```json
{
  "feeQuoteId": "quote-demo-001",
  "amount": "<decimal string from the agreed rule>",
  "currency": "USD",
  "ruleReference": "contract-clause-reference",
  "actionable": true
}
```

The amount is a placeholder, not an agreed fee rule or seed value.

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

Proposed response:

```json
{
  "decisionId": "decision-demo-001",
  "decision": "APPROVE",
  "analysisId": "analysis-demo-001",
  "feeQuoteId": "quote-demo-001",
  "status": "CURRENT",
  "decidedAt": "2026-10-31T12:05:00Z"
}
```

### Invoice — `POST /cases/{caseId}/invoice` and `GET /cases/{caseId}/invoice`

The request body is proposed to be empty: the backend loads the current persisted decision and quote itself. The client must not be able to authorize an invoice by sending `approved: true`.

Proposed response:

```json
{
  "invoiceId": "invoice-record-001",
  "providerInvoiceId": "PAYPAL-INVOICE-ID",
  "status": "DRAFT",
  "currency": "USD",
  "approvalDecisionId": "decision-demo-001",
  "createdAt": "2026-10-31T12:10:00Z"
}
```

The backend uses a stable persisted idempotency/business key. Repeated requests must not create duplicate provider invoices. If a PayPal request times out and the result is unknown, return a safe pending/uncertain state and reconcile; do not blindly retry. `DRAFT` or `SENT` does not mean paid.

### PayPal webhook — `POST /webhooks/paypal`

The endpoint receives PayPal's provider payload and verification headers. It must verify authenticity using PayPal's supported verification process before changing invoice state. Persist/deduplicate provider event IDs. Exact event types and response behavior must be confirmed during implementation with official PayPal documentation.

### Audit — `GET /cases/{caseId}/audit`

Proposed response:

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

## Proposed error shape

```json
{
  "code": "APPROVAL_REQUIRED",
  "message": "A current creator approval is required before an invoice can be created.",
  "requestId": "request-demo-001"
}
```

Proposed HTTP status mapping, to be reviewed:

| Status | Use |
|---|---|
| `400` | Malformed request or validation error. |
| `404` | Case or requested record not found. |
| `409` | Stale approval, rejected case, duplicate/conflicting state, or invoice not eligible. |
| `422` | Validly formed request that cannot be processed because business inputs are incomplete or ambiguous. |
| `502` / `503` | Provider unavailable or provider result uncertain; exact handling depends on reconciliation design. |

## Decisions to make with Person 1 and the team

- Final analysis/uncertainty DTO and sync versus async behavior.
- Whether decision requests require a quote for all approvals or permit approval of an analysis that has no actionable quote.
- Initial database relationships and version/freshness fields.
- Final error/status mapping and idempotency response behavior.
- Sample contract wording, fee rule, required inputs, and rounding before any actionable quote is produced.

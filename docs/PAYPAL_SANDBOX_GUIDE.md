# PayPal Sandbox integration guide

This project defaults to a **mock invoice provider**. The mock never contacts PayPal and labels every invoice as `MOCK`. Switch to `paypal` only after configuring Sandbox credentials. The app uses the PayPal Invoicing API v2 flow: obtain an OAuth token, create a draft invoice, then send it to the configured Sandbox recipient.

## 1. Create Sandbox accounts and app credentials

1. Sign in to the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/).
2. Open **Sandbox Accounts**. Use a Sandbox business account as the app owner and create or use a Sandbox personal account as the invoice recipient.
3. Create a REST app in Sandbox and copy its **Client ID** and **Secret**. These are server credentials; never put them in the frontend or commit them.
4. Use the Sandbox personal account email for `PAYPAL_INVOICE_RECIPIENT_EMAIL`. Use the business account email for `PAYPAL_INVOICER_EMAIL` if desired.

PayPal's current quick start uses the Sandbox host `https://api-m.sandbox.paypal.com`, client-credentials OAuth, `POST /v2/invoicing/invoices` to create a draft, and `POST /v2/invoicing/invoices/{invoice_id}/send` to email it to the recipient. [PayPal Invoicing quick start](https://developer.paypal.com/api/invoicing) · [Invoicing API v2 reference](https://developer.paypal.com/api/invoicing/v2/)

## 2. Configure the backend locally

Set these in the same PowerShell terminal that starts the backend. Replace the values on your computer; do not paste secrets into chat or Git.

```powershell
$env:LICENSELEAK_INVOICE_PROVIDER = "paypal"
$env:PAYPAL_BASE_URL = "https://api-m.sandbox.paypal.com"
$env:PAYPAL_CLIENT_ID = "your-sandbox-client-id"
$env:PAYPAL_CLIENT_SECRET = "your-sandbox-secret"
$env:PAYPAL_INVOICE_RECIPIENT_EMAIL = "your-sandbox-personal-account-email"
$env:PAYPAL_INVOICER_EMAIL = "your-sandbox-business-account-email"
```

The repository's plain `.env.example` is a variable-name template; Spring Boot does not load it automatically. Keep these values in your local terminal/IDE configuration or deployment environment. To return to safe mock behavior, set `LICENSELEAK_INVOICE_PROVIDER=mock` or remove the setting.

## 3. Run the approved demo flow

Start the backend and frontend, select a case, and run an analysis. For a mismatch quote to be actionable, the case must have a contract final valid date and an ad receipt date. The current rule charges $10 per ad for each calendar day after the final valid date through the receipt date, including the receipt date.

The backend requires a persisted approval tied to the current analysis and exact actionable quote before it calls PayPal. The UI sends no approval flag to the invoice endpoint; the server checks stored decision state itself.

You can also exercise the API in PowerShell:

```powershell
$base = "http://localhost:8080/api/v1"
$caseId = "case-demo-001"
$analysis = Invoke-RestMethod -Method Post "$base/cases/$caseId/analysis"
$analysis | ConvertTo-Json -Depth 8
```

Check that `feeQuote.actionable` is `true`, then approve the exact IDs returned:

```powershell
$decisionBody = @{
  decision = "APPROVE"
  analysisId = $analysis.analysisId
  feeQuoteId = $analysis.feeQuote.feeQuoteId
} | ConvertTo-Json

Invoke-RestMethod -Method Post "$base/cases/$caseId/decision" `
  -ContentType "application/json" -Body $decisionBody
```

Only after approval, request the invoice:

```powershell
Invoke-RestMethod -Method Post "$base/cases/$caseId/invoice"
Invoke-RestMethod "$base/cases/$caseId/invoice"
```

The invoice endpoint creates and sends the invoice to the Sandbox recipient. A second request returns the existing local invoice record and does not create a duplicate. If the response status is `UNKNOWN`, inspect PayPal's Sandbox business dashboard and the `GET` endpoint before considering any manual recovery; do not blindly retry.

## 4. Register and verify webhooks

PayPal needs a publicly reachable HTTPS endpoint to deliver webhooks. `localhost` is not reachable by PayPal. For the end-to-end webhook demo, deploy the backend to the team's HTTPS Render service first (or use an approved HTTPS development tunnel).

1. In the REST app's Sandbox settings, add a webhook URL ending in `/api/v1/webhooks/paypal`.
2. Subscribe to `INVOICING.INVOICE.SENT`, `INVOICING.INVOICE.PAID`, and `INVOICING.INVOICE.CANCELLED`.
3. Copy the generated webhook ID into `PAYPAL_WEBHOOK_ID` in the backend environment, then restart/redeploy.
4. Send an invoice to a Sandbox personal account and complete a test payment there. Check `GET /api/v1/cases/{caseId}/invoice` and the case audit endpoint.

The backend asks PayPal's webhook verification endpoint to verify the raw event and transmission headers before it applies a status. It records event IDs so duplicate deliveries are ignored, and it only marks an invoice paid after a verified `INVOICING.INVOICE.PAID` event. [PayPal webhook integration guide](https://developer.paypal.com/api/rest/webhooks/rest/) · [Invoicing webhook events](https://developer.paypal.com/api/invoicing/webhooks/)

PayPal retries webhook deliveries when the listener does not return a 2xx response. The handler records verified events and is idempotent; test it in Sandbox before using it for a demo. PayPal notes that invoices move through draft/sent and paid states separately, so `SENT` is not payment received.

## 5. Keep Sandbox isolated

- Use the Sandbox host and Sandbox app credentials for this demo. Do not switch the base URL to production.
- Never call invoice creation with real creator/customer data; all seeded cases and labels are fictional.
- Do not send a request directly to PayPal from the browser.
- Keep the `PAYPAL_CLIENT_SECRET` and webhook ID on the backend only.
- Use the mock provider for development when no Sandbox recipient/account is available.

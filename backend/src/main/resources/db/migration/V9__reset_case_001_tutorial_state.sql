-- Reset the seeded tutorial case so the walkthrough starts from a clean review.
-- Keep the case, agreement, and usage evidence; remove only generated review/payment history.
DELETE FROM paypal_webhook_event
WHERE provider_invoice_id IN (
    SELECT provider_invoice_id
    FROM invoice_record
    WHERE case_id = 'case-demo-001'
);

DELETE FROM invoice_record
WHERE case_id = 'case-demo-001';

DELETE FROM creator_decision
WHERE case_id = 'case-demo-001';

DELETE FROM audit_event
WHERE case_id = 'case-demo-001';

DELETE FROM fee_quote
WHERE analysis_id IN (
    SELECT id
    FROM analysis_run
    WHERE case_id = 'case-demo-001'
);

DELETE FROM analysis_run
WHERE case_id = 'case-demo-001';

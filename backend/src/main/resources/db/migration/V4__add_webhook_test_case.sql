-- Separate fictional case for testing the PayPal Sandbox invoice and webhook flow.
-- Its dates produce the same deterministic $30 USD quote as the original sample.
INSERT INTO demo_case (id, title, scenario_type, status, fictional)
VALUES (
    'case-demo-004',
    'Fictional campaign: webhook payment test',
    'POSSIBLE_MISMATCH',
    'READY_FOR_ANALYSIS',
    TRUE
);

INSERT INTO license_contract (id, case_id, title, version, summary, contract_valid_through)
VALUES (
    'contract-demo-004',
    'case-demo-004',
    'Illustrative webhook test licence',
    1,
    'clause-4: Fictional sample: the campaign licence permits the listed ad through 2026-10-01. The $10 daily charge applies per ad for each calendar day after that date through the date the out-of-contract ad is received.',
    DATE '2026-10-01'
);

INSERT INTO usage_evidence (id, case_id, evidence_type, label, ad_received_on, is_out_of_contract)
VALUES (
    'evidence-demo-004',
    'case-demo-004',
    'CAMPAIGN_POST',
    'Fictional ad received after the sample licence period for webhook testing',
    DATE '2026-10-04',
    TRUE
);

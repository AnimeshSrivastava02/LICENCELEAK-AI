ALTER TABLE license_contract
    ADD COLUMN contract_valid_through DATE;

ALTER TABLE usage_evidence
    ADD COLUMN ad_received_on DATE,
    ADD COLUMN is_out_of_contract BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE license_contract
SET summary = 'clause-4: Fictional sample: the campaign licence permits the listed ad through 2026-10-01. The $10 daily charge applies per ad for each calendar day after that date through the date the out-of-contract ad is received.',
    contract_valid_through = DATE '2026-10-01'
WHERE case_id = 'case-demo-001';

UPDATE usage_evidence
SET label = 'Fictional ad received after the sample licence period',
    ad_received_on = DATE '2026-10-04',
    is_out_of_contract = TRUE
WHERE id = 'evidence-demo-001';

UPDATE license_contract
SET summary = 'clause-2: Fictional sample: the campaign licence permits the listed ad through 2026-12-31.',
    contract_valid_through = DATE '2026-12-31'
WHERE case_id = 'case-demo-002';

UPDATE usage_evidence
SET label = 'Fictional ad received within the sample licence period',
    ad_received_on = DATE '2026-10-10',
    is_out_of_contract = FALSE
WHERE id = 'evidence-demo-002';

UPDATE license_contract
SET summary = 'clause-4: Fictional sample with an unclear end date. The creator must review the source wording before any fee can be calculated.',
    contract_valid_through = NULL
WHERE case_id = 'case-demo-003';

UPDATE usage_evidence
SET label = 'Fictional ad evidence with an unresolved receipt date',
    ad_received_on = NULL,
    is_out_of_contract = TRUE
WHERE id = 'evidence-demo-003';

CREATE TABLE analysis_run (
    id VARCHAR(36) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES demo_case (id) ON DELETE CASCADE,
    version INTEGER NOT NULL CHECK (version > 0),
    outcome VARCHAR(40) NOT NULL CHECK (outcome IN ('POSSIBLE_MISMATCH', 'PERMITTED', 'AMBIGUOUS')),
    summary TEXT NOT NULL,
    source VARCHAR(16) NOT NULL CHECK (source IN ('MOCK', 'GEMINI', 'VALIDATION')),
    model_name VARCHAR(120),
    contract_version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT analysis_run_case_version_unique UNIQUE (case_id, version)
);

CREATE TABLE analysis_clause_reference (
    analysis_id VARCHAR(36) NOT NULL REFERENCES analysis_run (id) ON DELETE CASCADE,
    clause_reference VARCHAR(120) NOT NULL,
    reference_order INTEGER NOT NULL,
    PRIMARY KEY (analysis_id, clause_reference)
);

CREATE TABLE analysis_evidence_reference (
    analysis_id VARCHAR(36) NOT NULL REFERENCES analysis_run (id) ON DELETE CASCADE,
    evidence_reference VARCHAR(64) NOT NULL REFERENCES usage_evidence (id),
    reference_order INTEGER NOT NULL,
    PRIMARY KEY (analysis_id, evidence_reference)
);

CREATE TABLE analysis_uncertainty (
    id BIGSERIAL PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL REFERENCES analysis_run (id) ON DELETE CASCADE,
    uncertainty_order INTEGER NOT NULL,
    uncertainty TEXT NOT NULL
);

CREATE TABLE fee_quote (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL UNIQUE REFERENCES analysis_run (id) ON DELETE CASCADE,
    amount NUMERIC(12, 2),
    currency CHAR(3) NOT NULL,
    daily_rate NUMERIC(12, 2) NOT NULL,
    total_billable_days INTEGER NOT NULL CHECK (total_billable_days >= 0),
    actionable BOOLEAN NOT NULL,
    rule_reference VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fee_quote_item (
    id BIGSERIAL PRIMARY KEY,
    fee_quote_id VARCHAR(36) NOT NULL REFERENCES fee_quote (id) ON DELETE CASCADE,
    evidence_id VARCHAR(64) NOT NULL REFERENCES usage_evidence (id),
    contract_valid_through DATE,
    ad_received_on DATE,
    billable_days INTEGER NOT NULL CHECK (billable_days >= 0),
    amount NUMERIC(12, 2),
    actionable BOOLEAN NOT NULL,
    CONSTRAINT fee_quote_item_evidence_unique UNIQUE (fee_quote_id, evidence_id)
);

CREATE TABLE creator_decision (
    id VARCHAR(36) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES demo_case (id) ON DELETE CASCADE,
    analysis_id VARCHAR(36) NOT NULL REFERENCES analysis_run (id),
    fee_quote_id VARCHAR(36) REFERENCES fee_quote (id),
    decision VARCHAR(16) NOT NULL CHECK (decision IN ('APPROVE', 'REJECT')),
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX creator_decision_case_time_idx ON creator_decision (case_id, decided_at DESC);

CREATE TABLE invoice_record (
    id VARCHAR(36) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL UNIQUE REFERENCES demo_case (id) ON DELETE CASCADE,
    decision_id VARCHAR(36) NOT NULL REFERENCES creator_decision (id),
    fee_quote_id VARCHAR(36) NOT NULL REFERENCES fee_quote (id),
    provider_name VARCHAR(16) NOT NULL CHECK (provider_name IN ('MOCK', 'PAYPAL')),
    provider_invoice_id VARCHAR(120) UNIQUE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('CREATING', 'DRAFT', 'SENT', 'PAID', 'CANCELLED', 'FAILED', 'UNKNOWN')),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    invoice_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE paypal_webhook_event (
    event_id VARCHAR(120) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    provider_invoice_id VARCHAR(120),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_event (
    id VARCHAR(36) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES demo_case (id) ON DELETE CASCADE,
    event_type VARCHAR(48) NOT NULL,
    record_id VARCHAR(120),
    details TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX audit_event_case_time_idx ON audit_event (case_id, occurred_at);

CREATE TABLE demo_case (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    scenario_type VARCHAR(40) NOT NULL CHECK (scenario_type IN ('POSSIBLE_MISMATCH', 'PERMITTED', 'AMBIGUOUS')),
    status VARCHAR(40) NOT NULL CHECK (status = 'READY_FOR_ANALYSIS'),
    fictional BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT demo_case_must_be_fictional CHECK (fictional = TRUE)
);

CREATE TABLE license_contract (
    id VARCHAR(64) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL UNIQUE REFERENCES demo_case (id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    summary TEXT NOT NULL
);

CREATE TABLE usage_evidence (
    id VARCHAR(64) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES demo_case (id) ON DELETE CASCADE,
    evidence_type VARCHAR(40) NOT NULL,
    label VARCHAR(160) NOT NULL
);

CREATE INDEX usage_evidence_case_id_idx ON usage_evidence (case_id);

INSERT INTO demo_case (id, title, scenario_type, status, fictional) VALUES
    ('case-demo-001', 'Fictional campaign: possible mismatch sample', 'POSSIBLE_MISMATCH', 'READY_FOR_ANALYSIS', TRUE),
    ('case-demo-002', 'Fictional campaign: permitted use sample', 'PERMITTED', 'READY_FOR_ANALYSIS', TRUE),
    ('case-demo-003', 'Fictional campaign: needs human review sample', 'AMBIGUOUS', 'READY_FOR_ANALYSIS', TRUE);

INSERT INTO license_contract (id, case_id, title, version, summary) VALUES
    ('contract-demo-001', 'case-demo-001', 'Illustrative licence draft', 1, 'Placeholder contract metadata. Final sample wording and all fee terms are awaiting team agreement.'),
    ('contract-demo-002', 'case-demo-002', 'Illustrative licence draft', 1, 'Placeholder contract metadata. Final sample wording and all fee terms are awaiting team agreement.'),
    ('contract-demo-003', 'case-demo-003', 'Illustrative licence draft', 1, 'Placeholder contract metadata. Final sample wording and all fee terms are awaiting team agreement.');

INSERT INTO usage_evidence (id, case_id, evidence_type, label) VALUES
    ('evidence-demo-001', 'case-demo-001', 'CAMPAIGN_POST', 'Fictional sample campaign material'),
    ('evidence-demo-002', 'case-demo-002', 'CAMPAIGN_POST', 'Fictional sample campaign material'),
    ('evidence-demo-003', 'case-demo-003', 'CAMPAIGN_POST', 'Fictional sample campaign material');

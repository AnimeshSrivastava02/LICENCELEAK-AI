UPDATE license_contract
SET summary = REPLACE(
        REPLACE(
            REPLACE(summary, 'this example record', 'this case record'),
            'this example agreement', 'this agreement'),
        'in this example record', 'in this case record')
WHERE case_id IN ('case-demo-001', 'case-demo-002', 'case-demo-003', 'case-demo-004');

UPDATE license_contract
SET summary = REPLACE(summary,
    'The displayed fee is a calculation from the dates in this case record; confirm the signed terms and source evidence before relying on it.',
    'The displayed fee is a review calculation from the recorded dates; confirm the signed terms and source evidence before relying on it.')
WHERE case_id IN ('case-demo-001', 'case-demo-004');

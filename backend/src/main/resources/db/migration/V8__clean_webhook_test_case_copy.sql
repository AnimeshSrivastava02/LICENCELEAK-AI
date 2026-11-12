UPDATE demo_case
SET title = 'Autumn Collection Campaign'
WHERE id = 'case-demo-004';

UPDATE license_contract
SET title = 'Autumn Collection Content License',
    summary = E'CONTENT LICENSE AGREEMENT\nLicensor: Creator (rights holder)\nLicensee: Brand (campaign advertiser)\nCampaign: Autumn Collection paid social campaign\nLicensed work: One short-form video and three still images created for the campaign.\nGrant: Non-exclusive, non-transferable use on the licensee''s owned Instagram and TikTok channels and in paid social placements targeted to the United States.\nLicense term: July 1, 2026 through October 1, 2026, inclusive.\nCampaign fee: USD 4,000, payable within 30 days of invoice.\nClause 4 — Post-term use: Each paid placement that remains active after the license term carries an additional USD 10 per ad for each calendar day, starting the day after expiry and ending on the date documented in the usage record. Placements are counted separately.\nRenewal: Any extension or additional use requires written agreement by both parties.\nReview note: The displayed fee is a review calculation from the recorded dates; confirm the signed terms and source evidence before relying on it.',
    contract_valid_through = DATE '2026-10-01'
WHERE case_id = 'case-demo-004';

UPDATE usage_evidence
SET label = 'Instagram Reel · Autumn Collection paid placement'
WHERE id = 'evidence-demo-004';

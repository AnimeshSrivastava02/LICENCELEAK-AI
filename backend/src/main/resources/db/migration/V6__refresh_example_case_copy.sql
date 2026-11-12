UPDATE demo_case
SET title = CASE id
    WHEN 'case-demo-001' THEN 'Summer Essentials Campaign'
    WHEN 'case-demo-002' THEN 'Spring Launch Campaign'
    WHEN 'case-demo-003' THEN 'Creator Partnership Campaign'
    WHEN 'case-demo-004' THEN 'Autumn Collection Campaign'
    ELSE title
END
WHERE id IN ('case-demo-001', 'case-demo-002', 'case-demo-003', 'case-demo-004');

UPDATE license_contract
SET title = CASE case_id
    WHEN 'case-demo-001' THEN 'Summer Essentials Content License'
    WHEN 'case-demo-002' THEN 'Spring Launch Content License'
    WHEN 'case-demo-003' THEN 'Creator Partnership Content License'
    WHEN 'case-demo-004' THEN 'Autumn Collection Content License'
    ELSE title
END,
summary = CASE case_id
    WHEN 'case-demo-001' THEN E'CONTENT LICENSE AGREEMENT\nLicensor: Creator (rights holder)\nLicensee: Brand (campaign advertiser)\nCampaign: Summer Essentials paid social campaign\nLicensed work: One short-form video and three still images created for the campaign.\nGrant: Non-exclusive, non-transferable use on the licensee’s owned Instagram and TikTok channels and in paid social placements targeted to the United States.\nLicense term: July 1, 2026 through October 1, 2026, inclusive.\nCampaign fee: USD 4,000, payable within 30 days of invoice.\nClause 4 — Post-term use: Each paid placement that remains active after the license term carries an additional USD 10 per ad for each calendar day, starting the day after expiry and ending on the date documented in the usage record. Placements are counted separately.\nRenewal: Any extension or additional use requires written agreement by both parties.\nReview note: The displayed fee is a calculation from the dates in this example record; confirm the signed terms and source evidence before relying on it.'
    WHEN 'case-demo-002' THEN E'CONTENT LICENSE AGREEMENT\nLicensor: Creator (rights holder)\nLicensee: Brand (campaign advertiser)\nCampaign: Spring Launch product feature\nLicensed work: One campaign post and associated product stills.\nGrant: Non-exclusive use on the licensee’s owned social channels for campaign promotion.\nLicense term: January 1, 2026 through December 31, 2026, inclusive.\nCampaign fee: USD 2,500, payable within 30 days of invoice.\nUsage condition: Use within the stated channels and term is covered by this example agreement. Any extension or additional placement requires written approval.\nReview note: Compare each source record with the signed agreement before closing the review.'
    WHEN 'case-demo-003' THEN E'CONTENT LICENSE AGREEMENT — REVIEW COPY\nLicensor: Creator (rights holder)\nLicensee: Brand (campaign advertiser)\nCampaign: Creator partnership collection\nLicensed work: Short-form campaign video.\nGrant: The available agreement excerpt refers to campaign use on social channels.\nLicense term: The end date is not present in the provided excerpt.\nClause 4: The excerpt refers to post-term use but does not provide enough information to determine when the term ends.\nReview required: Check the signed agreement and source records. No fee is calculated while the required dates remain unconfirmed.'
    WHEN 'case-demo-004' THEN E'CONTENT LICENSE AGREEMENT\nLicensor: Creator (rights holder)\nLicensee: Brand (campaign advertiser)\nCampaign: Autumn Collection paid social campaign\nLicensed work: One short-form video and three still images created for the campaign.\nGrant: Non-exclusive, non-transferable use on the licensee’s owned Instagram and TikTok channels and in paid social placements targeted to the United States.\nLicense term: July 1, 2026 through October 1, 2026, inclusive.\nCampaign fee: USD 4,000, payable within 30 days of invoice.\nClause 4 — Post-term use: Each paid placement that remains active after the license term carries an additional USD 10 per ad for each calendar day, starting the day after expiry and ending on the date documented in the usage record. Placements are counted separately.\nRenewal: Any extension or additional use requires written agreement by both parties.\nReview note: The displayed fee is a calculation from the dates in this example record; confirm the signed terms and source evidence before relying on it.'
    ELSE summary
END
WHERE case_id IN ('case-demo-001', 'case-demo-002', 'case-demo-003', 'case-demo-004');

UPDATE usage_evidence
SET label = CASE id
    WHEN 'evidence-demo-001' THEN 'Instagram Reel · Summer Essentials paid placement'
    WHEN 'evidence-demo-002' THEN 'Instagram post · Spring Launch product feature'
    WHEN 'evidence-demo-003' THEN 'Short-form video · Creator Partnership collection'
    WHEN 'evidence-demo-004' THEN 'Instagram Reel · Autumn Collection paid placement'
    ELSE label
END
WHERE id IN ('evidence-demo-001', 'evidence-demo-002', 'evidence-demo-003', 'evidence-demo-004');

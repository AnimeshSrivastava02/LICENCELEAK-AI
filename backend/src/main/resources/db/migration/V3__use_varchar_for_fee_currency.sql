ALTER TABLE fee_quote
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING btrim(currency)::VARCHAR(3);

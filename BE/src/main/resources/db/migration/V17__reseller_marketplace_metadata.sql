-- Move additive changes out of the already applied V5/V6 migrations.
ALTER TABLE products
    ADD COLUMN category_code VARCHAR(50),
    ADD COLUMN destination_code VARCHAR(100),
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    ADD CONSTRAINT chk_products_currency CHECK (currency ~ '^[A-Z]{3}$');

CREATE INDEX idx_products_category_code ON products(category_code);
CREATE INDEX idx_products_destination_code ON products(destination_code);

ALTER TABLE campaigns
    ADD COLUMN commission_currency VARCHAR(3),
    ADD COLUMN terms_text TEXT,
    ADD COLUMN terms_version INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_campaign_terms_version CHECK (terms_version > 0);

-- Legacy fixed-amount campaigns have no recorded currency. Do not guess it.
-- NOT VALID preserves those existing rows, but enforces the rule on new rows
-- and updates. Fill their verified currency before updating them, then run
-- VALIDATE CONSTRAINT in a subsequent migration once the data is reconciled.
ALTER TABLE campaigns ADD CONSTRAINT chk_campaign_commission_currency
    CHECK (
        (commission_type = 'PERCENTAGE' AND commission_currency IS NULL)
        OR
        (commission_type = 'FIXED_AMOUNT'
            AND commission_currency IS NOT NULL
            AND commission_currency ~ '^[A-Z]{3}$')
    ) NOT VALID;

-- V12: Create payouts and payout items
CREATE TABLE payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    payout_status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    payment_reference VARCHAR(255),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payout_status CHECK (
        payout_status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
    )
);
CREATE INDEX idx_payouts_merchant_reseller ON payouts(merchant_id, reseller_id);

CREATE TABLE payout_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_id UUID NOT NULL REFERENCES payouts(id) ON DELETE RESTRICT,
    commission_id UUID NOT NULL UNIQUE REFERENCES commissions(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payout_items_payout_id ON payout_items(payout_id);
CREATE INDEX idx_payout_items_commission_id ON payout_items(commission_id);

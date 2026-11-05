-- V3: Create reseller profiles
CREATE TABLE reseller_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    payout_info JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reseller_profiles_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING', 'CLOSED')),
    CONSTRAINT chk_reseller_payout_info_object CHECK (jsonb_typeof(payout_info) = 'object')
);
CREATE INDEX idx_reseller_profiles_user_id ON reseller_profiles(user_id);

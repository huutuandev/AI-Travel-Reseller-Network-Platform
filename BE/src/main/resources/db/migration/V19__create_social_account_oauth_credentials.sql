CREATE TABLE social_account_oauth_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    social_account_id UUID NOT NULL UNIQUE
        REFERENCES social_accounts(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token_ciphertext TEXT NOT NULL,
    refresh_token_ciphertext TEXT,
    token_expires_at TIMESTAMPTZ NOT NULL,
    refresh_token_expires_at TIMESTAMPTZ,
    scopes TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_social_oauth_provider CHECK (provider IN ('TIKTOK', 'FACEBOOK')),
    CONSTRAINT uq_social_oauth_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_social_oauth_token_expiry
    ON social_account_oauth_credentials(provider, token_expires_at);

CREATE TRIGGER trg_social_oauth_credentials_touch
BEFORE UPDATE ON social_account_oauth_credentials
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ============================================================================
-- COMPLETE SQL FILE (FULL CONTENT): AI TRAVEL ERD - 18 TABLES
-- PostgreSQL schema for a NEW EMPTY database; Flyway migration body without BEGIN/COMMIT.
-- If you see this header, you have the BEGINNING of the complete script.
-- Contents: 18 CREATE TABLE, PK/FK/UNIQUE/CHECK, functions, triggers, indexes.
-- Source: ERD approved 22/10/2026; generated copy for 29/10/2026.
-- This creates tables INSIDE an existing database; it does NOT CREATE DATABASE.
-- ============================================================================

-- AI Travel Reseller Network Platform
-- PostgreSQL schema recreated from the agreed 18-table ERD (22 Oct 2026).
-- Use only on a NEW, EMPTY database. This script NEVER drops tables or data.
-- Status enum CHECK constraints are intentionally deferred until Java enums are verified.
-- Agreed booking flow: AI Travel creates bookings.id in PENDING before merchant checkout;
-- merchant must return this same booking UUID in its verified callback/webhook.
-- Click events are aggregated in Redis; tracking_links.click_count is a persisted summary.
-- Not included because absent from approved ERD: merchant_resellers, ai_jobs,
-- tracking_click_daily, campaign_contents, checkout_sessions.


-- PostgreSQL 13+ has gen_random_uuid(); on older versions use pgcrypto/uuid-ossp.

-- 1. User, roles, merchant, reseller
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    scope VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Case-insensitive email uniqueness; SELECT lower(email) for matching if needed.
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    contact_email VARCHAR(255) NOT NULL,
    -- Phone widened to 50 per ERD description document, from 11 in the diagram.
    phone VARCHAR(50),
    address TEXT,
    subdomain VARCHAR(100) NOT NULL,
    custom_domain VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_merchants_subdomain_lower ON merchants (LOWER(subdomain));
CREATE UNIQUE INDEX uq_merchants_custom_domain_lower
    ON merchants (LOWER(custom_domain)) WHERE custom_domain IS NOT NULL;

CREATE TABLE merchant_branding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL UNIQUE REFERENCES merchants(id) ON DELETE RESTRICT,
    logo_url TEXT,
    favicon_url TEXT,
    primary_color VARCHAR(20) DEFAULT '#0B7F7B',
    secondary_color VARCHAR(20) DEFAULT '#F4B63F',
    custom_css TEXT,
    footer_text TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reseller_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    payout_info JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reseller_payout_info_object
        CHECK (jsonb_typeof(payout_info) = 'object')
);

CREATE TABLE social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    platform VARCHAR(50) NOT NULL,
    username VARCHAR(150) NOT NULL,
    profile_url TEXT,
    followers_count INTEGER NOT NULL DEFAULT 0 CHECK (followers_count >= 0),
    average_views INTEGER NOT NULL DEFAULT 0 CHECK (average_views >= 0),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_social_accounts_identity UNIQUE (reseller_id, platform, username)
);

-- 2. Products, campaigns, vouchers
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    images JSONB NOT NULL DEFAULT '[]'::jsonb,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    sale_price NUMERIC(12,2),
    availability_status VARCHAR(20) NOT NULL,
    official_checkout_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_merchant_slug UNIQUE (merchant_id, slug),
    CONSTRAINT chk_products_images_array CHECK (jsonb_typeof(images) = 'array'),
    CONSTRAINT chk_products_sale_price
        CHECK (sale_price IS NULL OR (sale_price >= 0 AND sale_price <= price))
);

CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    commission_type VARCHAR(20) NOT NULL,
    commission_rate NUMERIC(10,2) NOT NULL,
    cookie_window_days INTEGER NOT NULL DEFAULT 30,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_commission_rate CHECK (
        commission_rate >= 0
        AND (commission_type <> 'PERCENTAGE' OR commission_rate <= 100)
    ),
    CONSTRAINT chk_campaign_cookie_window CHECK (cookie_window_days > 0),
    CONSTRAINT chk_campaign_dates CHECK (
        start_date IS NULL OR end_date IS NULL OR end_date >= start_date
    )
);

CREATE TABLE campaign_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    commission_type_snapshot VARCHAR(20) NOT NULL,
    commission_rate_snapshot NUMERIC(10,2) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT uq_campaign_application UNIQUE (campaign_id, reseller_id),
    CONSTRAINT chk_commission_snapshot CHECK (
        commission_rate_snapshot >= 0
        AND (commission_type_snapshot <> 'PERCENTAGE' OR commission_rate_snapshot <= 100)
    )
);

CREATE TABLE tracking_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_application_id UUID NOT NULL REFERENCES campaign_applications(id) ON DELETE RESTRICT,
    tracking_code VARCHAR(100) NOT NULL UNIQUE,
    target_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    click_count INTEGER NOT NULL DEFAULT 0 CHECK (click_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vouchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    campaign_application_id UUID REFERENCES campaign_applications(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value >= 0),
    max_discount_amount NUMERIC(12,2),
    min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_order_amount >= 0),
    usage_limit INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    per_customer_limit INTEGER,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vouchers_merchant_code UNIQUE (merchant_id, code),
    CONSTRAINT chk_voucher_discount CHECK (
        discount_type <> 'PERCENTAGE' OR discount_value <= 100
    ),
    CONSTRAINT chk_voucher_max_discount CHECK (
        max_discount_amount IS NULL OR max_discount_amount >= 0
    ),
    CONSTRAINT chk_voucher_usage CHECK (
        usage_count >= 0
        AND (usage_limit IS NULL OR (usage_limit >= 0 AND usage_count <= usage_limit))
        AND (per_customer_limit IS NULL OR per_customer_limit > 0)
    ),
    CONSTRAINT chk_voucher_dates CHECK (
        starts_at IS NULL OR expires_at IS NULL OR expires_at > starts_at
    )
);

-- 3. Booking, commission, payout
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    customer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    tracking_link_id UUID REFERENCES tracking_links(id) ON DELETE RESTRICT,
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE commissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    payout_status VARCHAR(20) NOT NULL,
    payment_reference VARCHAR(255),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payout_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_id UUID NOT NULL REFERENCES payouts(id) ON DELETE RESTRICT,
    commission_id UUID NOT NULL UNIQUE REFERENCES commissions(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. AI content and knowledge
CREATE TABLE ai_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    prompt_used TEXT,
    generated_content JSONB,
    media_url TEXT,
    thumbnail_url TEXT,
    duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    generation_status VARCHAR(20) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    product_id UUID REFERENCES products(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Technical integrity for cross-table merchant/reseller boundaries.
CREATE OR REPLACE FUNCTION validate_booking_scope() RETURNS TRIGGER AS $$
DECLARE
    product_merchant UUID;
    linked_product UUID;
    linked_merchant UUID;
BEGIN
    SELECT p.merchant_id INTO product_merchant FROM products p WHERE p.id = NEW.product_id;
    IF product_merchant IS DISTINCT FROM NEW.merchant_id THEN
        RAISE EXCEPTION 'Booking merchant_id must match products.merchant_id';
    END IF;
    IF NEW.tracking_link_id IS NOT NULL THEN
        SELECT c.product_id, p.merchant_id INTO linked_product, linked_merchant
        FROM tracking_links t
        JOIN campaign_applications a ON a.id = t.campaign_application_id
        JOIN campaigns c ON c.id = a.campaign_id
        JOIN products p ON p.id = c.product_id
        WHERE t.id = NEW.tracking_link_id;
        IF linked_product IS DISTINCT FROM NEW.product_id
           OR linked_merchant IS DISTINCT FROM NEW.merchant_id THEN
            RAISE EXCEPTION 'Booking tracking link must belong to same product and merchant';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_validate_booking_scope
BEFORE INSERT OR UPDATE OF merchant_id, product_id, tracking_link_id ON bookings
FOR EACH ROW EXECUTE FUNCTION validate_booking_scope();

CREATE OR REPLACE FUNCTION validate_knowledge_document_scope() RETURNS TRIGGER AS $$
DECLARE
    product_merchant UUID;
BEGIN
    IF NEW.product_id IS NOT NULL THEN
        SELECT merchant_id INTO product_merchant FROM products WHERE id = NEW.product_id;
        IF product_merchant IS DISTINCT FROM NEW.merchant_id THEN
            RAISE EXCEPTION 'Knowledge document product must belong to its merchant';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_validate_knowledge_document_scope
BEFORE INSERT OR UPDATE OF merchant_id, product_id ON knowledge_documents
FOR EACH ROW EXECUTE FUNCTION validate_knowledge_document_scope();

CREATE OR REPLACE FUNCTION validate_voucher_scope() RETURNS TRIGGER AS $$
DECLARE
    application_merchant UUID;
BEGIN
    IF NEW.campaign_application_id IS NOT NULL THEN
        SELECT p.merchant_id INTO application_merchant
        FROM campaign_applications a
        JOIN campaigns c ON c.id = a.campaign_id
        JOIN products p ON p.id = c.product_id
        WHERE a.id = NEW.campaign_application_id;
        IF application_merchant IS DISTINCT FROM NEW.merchant_id THEN
            RAISE EXCEPTION 'Voucher application must belong to its merchant';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_validate_voucher_scope
BEFORE INSERT OR UPDATE OF merchant_id, campaign_application_id ON vouchers
FOR EACH ROW EXECUTE FUNCTION validate_voucher_scope();

CREATE OR REPLACE FUNCTION validate_commission_reseller() RETURNS TRIGGER AS $$
DECLARE
    source_reseller UUID;
BEGIN
    SELECT ca.reseller_id INTO source_reseller
    FROM bookings b
    JOIN tracking_links t ON t.id = b.tracking_link_id
    JOIN campaign_applications ca ON ca.id = t.campaign_application_id
    WHERE b.id = NEW.booking_id;
    IF source_reseller IS NULL OR source_reseller IS DISTINCT FROM NEW.reseller_id THEN
        RAISE EXCEPTION 'Commission reseller must match booking tracking source';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_validate_commission_reseller
BEFORE INSERT OR UPDATE OF booking_id, reseller_id ON commissions
FOR EACH ROW EXECUTE FUNCTION validate_commission_reseller();

CREATE OR REPLACE FUNCTION validate_payout_item_scope() RETURNS TRIGGER AS $$
DECLARE
    payout_merchant UUID;
    payout_reseller UUID;
    payout_currency CHAR(3);
    booking_merchant UUID;
    commission_reseller UUID;
    commission_amount NUMERIC(12,2);
    commission_currency CHAR(3);
BEGIN
    SELECT merchant_id, reseller_id, currency
    INTO payout_merchant, payout_reseller, payout_currency
    FROM payouts WHERE id = NEW.payout_id;
    SELECT b.merchant_id, c.reseller_id, c.amount, c.currency
    INTO booking_merchant, commission_reseller, commission_amount, commission_currency
    FROM commissions c JOIN bookings b ON b.id = c.booking_id
    WHERE c.id = NEW.commission_id;
    IF payout_merchant IS DISTINCT FROM booking_merchant
       OR payout_reseller IS DISTINCT FROM commission_reseller
       OR payout_currency IS DISTINCT FROM commission_currency THEN
        RAISE EXCEPTION 'Payout item must match commission merchant, reseller and currency';
    END IF;
    IF NEW.amount IS DISTINCT FROM commission_amount THEN
        RAISE EXCEPTION 'Partial payout is not enabled: item amount must equal commission';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_validate_payout_item_scope
BEFORE INSERT OR UPDATE OF payout_id, commission_id, amount ON payout_items
FOR EACH ROW EXECUTE FUNCTION validate_payout_item_scope();

-- Update timestamps on tables that declare updated_at.
CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_touch BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_merchants_touch BEFORE UPDATE ON merchants
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_branding_touch BEFORE UPDATE ON merchant_branding
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_reseller_touch BEFORE UPDATE ON reseller_profiles
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_products_touch BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_campaigns_touch BEFORE UPDATE ON campaigns
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_vouchers_touch BEFORE UPDATE ON vouchers
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_bookings_touch BEFORE UPDATE ON bookings
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_commissions_touch BEFORE UPDATE ON commissions
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_ai_contents_touch BEFORE UPDATE ON ai_contents
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_knowledge_touch BEFORE UPDATE ON knowledge_documents
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Useful FK indexes: PostgreSQL does not automatically index FK columns.
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_merchants_user_id ON merchants(user_id);
CREATE INDEX idx_social_accounts_reseller_id ON social_accounts(reseller_id);
CREATE INDEX idx_products_merchant_id ON products(merchant_id);
CREATE INDEX idx_campaigns_product_id ON campaigns(product_id);
CREATE INDEX idx_campaign_applications_reseller_id ON campaign_applications(reseller_id);
CREATE INDEX idx_tracking_links_application_id ON tracking_links(campaign_application_id);
CREATE INDEX idx_vouchers_merchant_id ON vouchers(merchant_id);
CREATE INDEX idx_vouchers_application_id ON vouchers(campaign_application_id);
CREATE INDEX idx_bookings_merchant_created ON bookings(merchant_id, created_at DESC);
CREATE INDEX idx_bookings_product_id ON bookings(product_id);
CREATE INDEX idx_bookings_tracking_link_id ON bookings(tracking_link_id);
CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX idx_commissions_reseller_id ON commissions(reseller_id);
CREATE INDEX idx_payouts_merchant_reseller ON payouts(merchant_id, reseller_id);
CREATE INDEX idx_payout_items_payout_id ON payout_items(payout_id);
CREATE INDEX idx_ai_contents_campaign_id ON ai_contents(campaign_id);
CREATE INDEX idx_knowledge_documents_merchant_id ON knowledge_documents(merchant_id);
CREATE INDEX idx_knowledge_documents_product_id ON knowledge_documents(product_id);

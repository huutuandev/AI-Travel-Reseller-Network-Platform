-- 1a. roles
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 1b. users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- 1c. user_roles
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT user_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 2. merchants
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255),
    logo_url TEXT,
    phone VARCHAR(20),
    address TEXT,
    website VARCHAR(255),
    verification_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT merchants_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT merchants_user_unique UNIQUE (user_id),
    CONSTRAINT merchants_status_check CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

-- 3. resellers
CREATE TABLE resellers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    referral_code VARCHAR(50),
    bank_account_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT resellers_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT resellers_user_unique UNIQUE (user_id),
    CONSTRAINT resellers_referral_unique UNIQUE (referral_code),
    CONSTRAINT resellers_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT resellers_referral_check CHECK (length(referral_code) >= 3 AND referral_code ~ '^[A-Z0-9_-]+$')
);

-- 4. products
CREATE TABLE products (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(255),
    category VARCHAR(50),
    description TEXT,
    original_price NUMERIC(12,2),
    sale_price NUMERIC(12,2) NOT NULL,
    booking_url TEXT,
    faq_structured JSONB,
    media_assets TEXT[],
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE RESTRICT,
    CONSTRAINT products_saleprice_check CHECK (sale_price > 0),
    CONSTRAINT products_originalprice_check CHECK (original_price IS NULL OR original_price >= sale_price),
    CONSTRAINT products_category_check CHECK (category IN ('VE_THAM_QUAN', 'BUFFET', 'TOUR_NGAY', 'SPA', 'XE_DUA_DON')),
    CONSTRAINT products_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT products_url_check CHECK (booking_url ~* '^https?://')
);

-- 5. campaigns
CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    commission_rate NUMERIC(5,2),
    reseller_split_rate NUMERIC(5,2) DEFAULT 60.00,
    guidelines TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT campaigns_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE RESTRICT,
    CONSTRAINT campaigns_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT campaigns_dates_check CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT campaigns_commission_check CHECK (commission_rate > 0 AND commission_rate <= 100.00),
    CONSTRAINT campaigns_split_check CHECK (reseller_split_rate > 0 AND reseller_split_rate <= 100.00),
    CONSTRAINT campaigns_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED'))
);

-- 6. contents
CREATE TABLE contents (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    persona VARCHAR(50),
    hook TEXT NOT NULL,
    script TEXT,
    caption TEXT NOT NULL,
    cta TEXT,
    hashtags VARCHAR(255),
    visual_prompt TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT contents_campaign_fk FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT contents_hook_check CHECK (length(hook) > 0),
    CONSTRAINT contents_caption_check CHECK (length(caption) > 0)
);

-- 7. tracking_links
CREATE TABLE tracking_links (
    id UUID PRIMARY KEY,
    reseller_id UUID NOT NULL,
    campaign_id UUID NOT NULL,
    product_id UUID NOT NULL,
    tracking_code VARCHAR(100) NOT NULL,
    landing_slug VARCHAR(150) NOT NULL,
    commission_snapshot NUMERIC(5,2) NOT NULL,
    reseller_split_snapshot NUMERIC(5,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tracking_links_reseller_fk FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE RESTRICT,
    CONSTRAINT tracking_links_campaign_fk FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE RESTRICT,
    CONSTRAINT tracking_links_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT tracking_links_campaign_reseller_unique UNIQUE (campaign_id, reseller_id),
    CONSTRAINT tracking_links_tracking_code_unique UNIQUE (tracking_code),
    CONSTRAINT tracking_links_landing_slug_unique UNIQUE (landing_slug),
    CONSTRAINT tracking_links_rates_check CHECK (commission_snapshot > 0 AND reseller_split_snapshot > 0)
);

-- 8. conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    tracking_link_id UUID NOT NULL,
    session_token VARCHAR(100),
    chat_history JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT conversations_tracking_link_fk FOREIGN KEY (tracking_link_id) REFERENCES tracking_links(id) ON DELETE CASCADE,
    CONSTRAINT conversations_session_link_unique UNIQUE (session_token, tracking_link_id)
);

-- 9. orders
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    tracking_link_id UUID NOT NULL,
    conversation_id UUID,
    booking_reference VARCHAR(100),
    amount NUMERIC(12,2),
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT orders_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE RESTRICT,
    CONSTRAINT orders_tracking_link_fk FOREIGN KEY (tracking_link_id) REFERENCES tracking_links(id) ON DELETE RESTRICT,
    CONSTRAINT orders_conversation_fk FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    CONSTRAINT orders_booking_merchant_unique UNIQUE (booking_reference, merchant_id),
    CONSTRAINT orders_amount_check CHECK (amount > 0),
    CONSTRAINT orders_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED', 'REFUNDED'))
);

-- 10. commissions
CREATE TABLE commissions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    reseller_id UUID NOT NULL,
    gross_commission NUMERIC(12,2),
    reseller_amount NUMERIC(12,2),
    platform_amount NUMERIC(12,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    eligible_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT commissions_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    CONSTRAINT commissions_order_unique UNIQUE (order_id),
    CONSTRAINT commissions_reseller_fk FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE RESTRICT,
    CONSTRAINT commissions_amounts_check CHECK (gross_commission = (reseller_amount + platform_amount) AND gross_commission >= 0 AND reseller_amount >= 0 AND platform_amount >= 0),
    CONSTRAINT commissions_status_check CHECK (status IN ('PENDING', 'ELIGIBLE', 'PAID', 'CANCELLED'))
);

-- Chỉ mục tối ưu
CREATE UNIQUE INDEX idx_tracking_links_code ON tracking_links(tracking_code);
CREATE INDEX idx_orders_tracking_link ON orders(tracking_link_id);
CREATE UNIQUE INDEX idx_commissions_order ON commissions(order_id);


-- =========================================================
-- SEED TEST DATA
-- AI TRAVEL RESELLER NETWORK PLATFORM
-- Environment: DEVELOPMENT
-- =========================================================


-- =========================================================
-- 1. ROLES
-- =========================================================

INSERT INTO roles (id, name, scope, description)
VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'PLATFORM_ADMIN',
    'GLOBAL',
    'Quản trị viên toàn nền tảng'
),
(
    '00000000-0000-0000-0000-000000000002',
    'ROLE_MERCHANT',
    'GLOBAL',
    'Quản lý sản phẩm và chiến dịch'
),
(
    '00000000-0000-0000-0000-000000000003',
    'ROLE_RESELLER',
    'GLOBAL',
    'Tiếp thị và phân phối sản phẩm'
)
ON CONFLICT (name) DO NOTHING;


-- =========================================================
-- 2. USERS
-- Password:
-- Admin: Admin@123
-- Merchant: Merchant@123
-- Reseller: Reseller@123
-- BCrypt strength: 10
-- =========================================================

INSERT INTO users (
    id, email, password_hash, full_name,
    phone, status, email_verified_at
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'admin@test.com',
    '$2a$10$jaZDPeZmkvtJkxln9GKq9udC4F5Suv.FY0tjmGsG/lG7Qq8jbXT8.',
    'Platform Administrator',
    '0905000001',
    'ACTIVE',
    NOW()
),
(
    '10000000-0000-0000-0000-000000000002',
    'merchant@test.com',
    '$2a$10$9EQaVVtXjl.q/WsgGawmjO8K3IDk6nB9esTH0CsaMtA545lkWyS1S',
    'Da Nang Travel Admin',
    '0905000002',
    'ACTIVE',
    NOW()
),
(
    '10000000-0000-0000-0000-000000000003',
    'reseller1@test.com',
    '$2a$10$Mqi9kLqPNgY4ShDb.XVOievR7LmP1ta7NbjnSslEBmlutkdVI1oj6',
    'Nguyen Van An',
    '0905000003',
    'ACTIVE',
    NOW()
),
(
    '10000000-0000-0000-0000-000000000004',
    'reseller2@test.com',
    '$2a$10$Mqi9kLqPNgY4ShDb.XVOievR7LmP1ta7NbjnSslEBmlutkdVI1oj6',
    'Tran Thi Binh',
    '0905000004',
    'ACTIVE',
    NOW()
),
(
    '10000000-0000-0000-0000-000000000005',
    'reseller3@test.com',
    '$2a$10$Mqi9kLqPNgY4ShDb.XVOievR7LmP1ta7NbjnSslEBmlutkdVI1oj6',
    'Le Van Cuong',
    '0905000005',
    'ACTIVE',
    NOW()
)
ON CONFLICT (id) DO NOTHING;


-- =========================================================
-- 3. USER ROLES
-- =========================================================

INSERT INTO user_roles (user_id, role_id)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001'
),
(
    '10000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002'
),
(
    '10000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000003'
),
(
    '10000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000003'
),
(
    '10000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000003'
)
ON CONFLICT (user_id, role_id) DO NOTHING;


-- =========================================================
-- 4. MERCHANT
-- =========================================================

INSERT INTO merchants (
    id, user_id, code, name, description,
    contact_email, phone, address,
    subdomain, status
)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'DN-TRAVEL-001',
    'Da Nang Travel',
    'Đơn vị cung cấp dịch vụ du lịch miền Trung.',
    'merchant@test.com',
    '0905000002',
    'Đà Nẵng, Việt Nam',
    'danangtravel',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;


-- =========================================================
-- 5. RESELLER PROFILES
-- =========================================================

INSERT INTO reseller_profiles (
    id, user_id, payout_info, status
)
VALUES
(
    '30000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000003',
    '{"bank_name":"Vietcombank","account_holder":"NGUYEN VAN AN","account_number":"0123456789"}',
    'ACTIVE'
),
(
    '30000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000004',
    '{"bank_name":"MB Bank","account_holder":"TRAN THI BINH","account_number":"0987654321"}',
    'ACTIVE'
),
(
    '30000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000005',
    '{"bank_name":"BIDV","account_holder":"LE VAN CUONG","account_number":"1122334455"}',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;


-- =========================================================
-- 6. PRODUCTS
-- =========================================================

INSERT INTO products (
    id, merchant_id, title, slug, description,
    images, price, sale_price,
    availability_status, official_checkout_url
)
VALUES
(
    '40000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'Tour Bà Nà Hills 1 ngày',
    'tour-ba-na-hills',
    'Khám phá Cầu Vàng, làng Pháp và Bà Nà Hills.',
    '["https://example.com/ba-na-hills.jpg"]'::jsonb,
    1200000,
    1050000,
    'AVAILABLE',
    'https://example.com/checkout/ba-na-hills'
),
(
    '40000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    'Tour phố cổ Hội An',
    'tour-pho-co-hoi-an',
    'Khám phá phố cổ Hội An và văn hóa địa phương.',
    '["https://example.com/hoi-an.jpg"]'::jsonb,
    650000,
    550000,
    'AVAILABLE',
    'https://example.com/checkout/hoi-an'
),
(
    '40000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'Tour khám phá Cố đô Huế',
    'tour-co-do-hue',
    'Tham quan Đại Nội, chùa Thiên Mụ và di tích lịch sử.',
    '["https://example.com/hue.jpg"]'::jsonb,
    950000,
    850000,
    'AVAILABLE',
    'https://example.com/checkout/hue'
),
(
    '40000000-0000-0000-0000-000000000004',
    '20000000-0000-0000-0000-000000000001',
    'Tour bán đảo Sơn Trà',
    'tour-ban-dao-son-tra',
    'Khám phá thiên nhiên và chùa Linh Ứng.',
    '["https://example.com/son-tra.jpg"]'::jsonb,
    450000,
    NULL,
    'AVAILABLE',
    'https://example.com/checkout/son-tra'
)
ON CONFLICT (id) DO NOTHING;


-- =========================================================
-- 7. CAMPAIGNS
-- PERCENTAGE: commission_currency = NULL
-- FIXED_AMOUNT: commission_currency = VND
-- =========================================================

INSERT INTO campaigns (
    id,
    product_id,
    title,
    description,
    commission_type,
    commission_rate,
    commission_currency,
    cookie_window_days,
    start_date,
    end_date,
    status
)
VALUES
(
    '50000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    'Summer Sale - Ba Na Hills',
    'Chiến dịch quảng bá tour Bà Nà Hills.',
    'PERCENTAGE',
    15,
    NULL,
    30,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '60 days',
    'ACTIVE'
),
(
    '50000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000002',
    'Discover Hoi An',
    'Chiến dịch quảng bá du lịch Hội An.',
    'PERCENTAGE',
    12,
    NULL,
    30,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '90 days',
    'ACTIVE'
),
(
    '50000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000003',
    'Explore Hue Heritage',
    'Chiến dịch quảng bá di sản Cố đô Huế.',
    'PERCENTAGE',
    10,
    NULL,
    45,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '90 days',
    'ACTIVE'
),
(
    '50000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000004',
    'Son Tra Nature Experience',
    'Chiến dịch khám phá thiên nhiên Sơn Trà.',
    'FIXED_AMOUNT',
    50000,
    'VND',
    30,
    NOW() + INTERVAL '10 days',
    NOW() + INTERVAL '100 days',
    'PENDING_REVIEW'
)
ON CONFLICT (id) DO NOTHING;

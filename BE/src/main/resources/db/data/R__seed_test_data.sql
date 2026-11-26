
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

-- Dữ liệu mẫu cho luồng Campaign Application -> Tracking Link -> Booking.
-- Dùng cùng dữ liệu gốc đã có trong R__seed_test_data.sql.

SET LOCAL search_path TO travel_v2, public;

-- Customer mẫu. Tài khoản bị vô hiệu hóa và không được gán role;
-- chỉ dùng để hiển thị thông tin customer trong Booking API.
INSERT INTO travel_v2.users (
    id,
    email,
    password_hash,
    full_name,
    phone,
    status
)
VALUES (
    '60000000-0000-4000-8000-000000000001',
    'booking.customer@example.test',
    '!DISABLED:booking-test-fixture-v1',
    'Booking Test Customer',
    '0905000099',
    'DISABLED'
)
ON CONFLICT DO NOTHING;


-- Đăng ký reseller vào campaign.
-- V15 tự kiểm tra và chụp commission rate từ campaign.
INSERT INTO travel_v2.campaign_applications (
    id,
    campaign_id,
    reseller_id,
    status,
    commission_type_snapshot,
    commission_rate_snapshot,
    applied_at,
    reviewed_at
)
VALUES
(
    '61000000-0000-4000-8000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'APPROVED',
    'PERCENTAGE',
    15,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '9 days'
),
(
    '61000000-0000-4000-8000-000000000002',
    '50000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000002',
    'APPROVED',
    'PERCENTAGE',
    15,
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '7 days'
),
(
    '61000000-0000-4000-8000-000000000003',
    '50000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000003',
    'APPROVED',
    'PERCENTAGE',
    12,
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '5 days'
),
(
    '61000000-0000-4000-8000-000000000004',
    '50000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    'APPROVED',
    'PERCENTAGE',
    10,
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '3 days'
)
ON CONFLICT (campaign_id, reseller_id) DO NOTHING;


-- Tạo tracking link cho các campaign application mẫu.
-- Reseller trên link phải trùng với reseller của application.
INSERT INTO travel_v2.tracking_links (
    id,
    campaign_application_id,
    reseller_id,
    tracking_code,
    target_url,
    status,
    click_count
)
VALUES
(
    '71000000-0000-4000-8000-000000000001',
    '61000000-0000-4000-8000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'TEST-BA-RESELLER-1',
    'https://example.com/checkout/ba-na-hills',
    'ACTIVE',
    125
),
(
    '71000000-0000-4000-8000-000000000002',
    '61000000-0000-4000-8000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    'TEST-BA-RESELLER-2',
    'https://example.com/checkout/ba-na-hills',
    'ACTIVE',
    80
),
(
    '71000000-0000-4000-8000-000000000003',
    '61000000-0000-4000-8000-000000000003',
    '30000000-0000-0000-0000-000000000003',
    'TEST-HOIAN-RESELLER-3',
    'https://example.com/checkout/hoi-an',
    'ACTIVE',
    54
),
(
    '71000000-0000-4000-8000-000000000004',
    '61000000-0000-4000-8000-000000000004',
    '30000000-0000-0000-0000-000000000001',
    'TEST-HUE-RESELLER-1',
    'https://example.com/checkout/hue',
    'ACTIVE',
    32
)
ON CONFLICT DO NOTHING;


-- Booking liên kết đúng merchant, product và tracking link.
-- V15 kiểm tra các liên kết này trước khi insert.
INSERT INTO travel_v2.bookings (
    id,
    merchant_id,
    customer_id,
    product_id,
    tracking_link_id,
    total_amount,
    currency,
    booking_status,
    created_at,
    updated_at
)
VALUES
(
    '9f000000-0000-4000-8000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    900000,
    'VND',
    'PENDING',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days'
),
(
    '9f000000-0000-4000-8000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000002',
    1250000,
    'VND',
    'CONFIRMED',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '4 days'
),
(
    '9f000000-0000-4000-8000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000002',
    '71000000-0000-4000-8000-000000000003',
    650000,
    'VND',
    'CANCELLED',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
),
(
    '9f000000-0000-4000-8000-000000000004',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000003',
    '71000000-0000-4000-8000-000000000004',
    1800000,
    'VND',
    'COMPLETED',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
),
(
    '9f000000-0000-4000-8000-000000000005',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    750000,
    'VND',
    'REFUNDED',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
)
ON CONFLICT (id) DO NOTHING;

-- =========================================================
-- Commission/Payout test fixtures
-- Requires the merchant, resellers, customer, campaigns,
-- tracking links, and first five bookings from this seed.
-- =========================================================

-- 1. Thêm booking cho reseller 1, 2 và 3.
INSERT INTO travel_v2.bookings (
    id, merchant_id, customer_id, product_id, tracking_link_id,
    total_amount, currency, booking_status, created_at, updated_at
)
VALUES
(
    '9f000000-0000-4000-8000-000000000006',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    2000000, 'VND', 'COMPLETED',
    NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'
),
(
    '9f000000-0000-4000-8000-000000000007',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    1000000, 'VND', 'COMPLETED',
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'
),
(
    '9f000000-0000-4000-8000-000000000008',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000002',
    850000, 'VND', 'COMPLETED',
    NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'
),
(
    '9f000000-0000-4000-8000-000000000009',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    1500000, 'VND', 'COMPLETED',
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'
),
(
    '9f000000-0000-4000-8000-000000000010',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000002',
    '71000000-0000-4000-8000-000000000003',
    900000, 'VND', 'PENDING',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
),
(
    '9f000000-0000-4000-8000-000000000011',
    '20000000-0000-0000-0000-000000000001',
    '60000000-0000-4000-8000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '71000000-0000-4000-8000-000000000001',
    1100000, 'VND', 'COMPLETED',
    NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
)
ON CONFLICT (id) DO NOTHING;


-- 2. Tạo commission mẫu.
-- Các amount được tính từ commission snapshot của campaign:
-- Campaign 1 = 15%, Campaign 2 = 12%, Campaign 3 = 10%.
INSERT INTO travel_v2.commissions (
    id, booking_id, reseller_id, amount, currency, status,
    created_at, updated_at
)
VALUES
-- PENDING booking, PENDING commission: thử chuyển sang APPROVED.
(
    'c1000000-0000-4000-8000-000000000001',
    '9f000000-0000-4000-8000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    135000, 'VND', 'PENDING', NOW(), NOW()
),
-- CONFIRMED booking, APPROVED commission.
(
    'c1000000-0000-4000-8000-000000000002',
    '9f000000-0000-4000-8000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    187500, 'VND', 'APPROVED', NOW(), NOW()
),
-- CANCELLED booking, PENDING commission: thử chuyển sang CANCELLED.
(
    'c1000000-0000-4000-8000-000000000003',
    '9f000000-0000-4000-8000-000000000003',
    '30000000-0000-0000-0000-000000000003',
    78000, 'VND', 'PENDING', NOW(), NOW()
),
-- COMPLETED booking, commission sẽ được đưa vào payout COMPLETED.
(
    'c1000000-0000-4000-8000-000000000004',
    '9f000000-0000-4000-8000-000000000004',
    '30000000-0000-0000-0000-000000000001',
    180000, 'VND', 'PAYABLE', NOW(), NOW()
),
-- REFUNDED booking, APPROVED commission: thử chuyển sang REVERSED.
(
    'c1000000-0000-4000-8000-000000000005',
    '9f000000-0000-4000-8000-000000000005',
    '30000000-0000-0000-0000-000000000001',
    112500, 'VND', 'APPROVED', NOW(), NOW()
),
-- Commission trong payout FAILED.
(
    'c1000000-0000-4000-8000-000000000006',
    '9f000000-0000-4000-8000-000000000006',
    '30000000-0000-0000-0000-000000000001',
    300000, 'VND', 'PAYABLE', NOW(), NOW()
),
-- PAYABLE, chưa thuộc payout: dùng để tạo payout batch.
(
    'c1000000-0000-4000-8000-000000000007',
    '9f000000-0000-4000-8000-000000000007',
    '30000000-0000-0000-0000-000000000001',
    150000, 'VND', 'PAYABLE', NOW(), NOW()
),
-- Commission trong payout PROCESSING.
(
    'c1000000-0000-4000-8000-000000000008',
    '9f000000-0000-4000-8000-000000000008',
    '30000000-0000-0000-0000-000000000002',
    127500, 'VND', 'PAYABLE', NOW(), NOW()
),
-- PAYABLE, cùng reseller 1: tạo payout batch cùng commission 000007.
(
    'c1000000-0000-4000-8000-000000000009',
    '9f000000-0000-4000-8000-000000000009',
    '30000000-0000-0000-0000-000000000001',
    225000, 'VND', 'PAYABLE', NOW(), NOW()
),
-- PENDING booking và commission.
(
    'c1000000-0000-4000-8000-000000000010',
    '9f000000-0000-4000-8000-000000000010',
    '30000000-0000-0000-0000-000000000003',
    108000, 'VND', 'PENDING', NOW(), NOW()
),
-- COMPLETED booking, APPROVED commission: thử chuyển sang PAYABLE.
(
    'c1000000-0000-4000-8000-000000000011',
    '9f000000-0000-4000-8000-000000000011',
    '30000000-0000-0000-0000-000000000001',
    165000, 'VND', 'APPROVED', NOW(), NOW()
)
ON CONFLICT DO NOTHING;


-- 3. Tạo ba payout ở các trạng thái PROCESSING, FAILED và COMPLETED.
-- Ban đầu đều PROCESSING để trigger payout_items chấp nhận các item.
INSERT INTO travel_v2.payouts (
    id, merchant_id, reseller_id, total_amount, currency,
    payout_status, payment_reference, processed_at, created_at
)
VALUES
(
    'd1000000-0000-4000-8000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    0, 'VND', 'PROCESSING', NULL, NULL, NOW() - INTERVAL '3 days'
),
(
    'd1000000-0000-4000-8000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    0, 'VND', 'PROCESSING', NULL, NULL, NOW() - INTERVAL '2 days'
),
(
    'd1000000-0000-4000-8000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000002',
    0, 'VND', 'PROCESSING', NULL, NULL, NOW() - INTERVAL '1 day'
)
ON CONFLICT (id) DO NOTHING;


-- 4. Gắn commission vào payout.
-- Trigger sẽ tự lấy amount từ commission và cập nhật tổng payout.
INSERT INTO travel_v2.payout_items (
    id, payout_id, commission_id, amount
)
SELECT
    'e1000000-0000-4000-8000-000000000001',
    p.id,
    cm.id,
    cm.amount
FROM travel_v2.payouts p
JOIN travel_v2.commissions cm
  ON cm.id = 'c1000000-0000-4000-8000-000000000004'
WHERE p.id = 'd1000000-0000-4000-8000-000000000001'
  AND p.payout_status = 'PROCESSING'
  AND cm.status = 'PAYABLE'
  AND NOT EXISTS (
      SELECT 1 FROM travel_v2.payout_items pi
      WHERE pi.commission_id = cm.id
  )
ON CONFLICT (commission_id) DO NOTHING;

INSERT INTO travel_v2.payout_items (
    id, payout_id, commission_id, amount
)
SELECT
    'e1000000-0000-4000-8000-000000000002',
    p.id,
    cm.id,
    cm.amount
FROM travel_v2.payouts p
JOIN travel_v2.commissions cm
  ON cm.id = 'c1000000-0000-4000-8000-000000000006'
WHERE p.id = 'd1000000-0000-4000-8000-000000000002'
  AND p.payout_status = 'PROCESSING'
  AND cm.status = 'PAYABLE'
  AND NOT EXISTS (
      SELECT 1 FROM travel_v2.payout_items pi
      WHERE pi.commission_id = cm.id
  )
ON CONFLICT (commission_id) DO NOTHING;

INSERT INTO travel_v2.payout_items (
    id, payout_id, commission_id, amount
)
SELECT
    'e1000000-0000-4000-8000-000000000003',
    p.id,
    cm.id,
    cm.amount
FROM travel_v2.payouts p
JOIN travel_v2.commissions cm
  ON cm.id = 'c1000000-0000-4000-8000-000000000008'
WHERE p.id = 'd1000000-0000-4000-8000-000000000003'
  AND p.payout_status = 'PROCESSING'
  AND cm.status = 'PAYABLE'
  AND NOT EXISTS (
      SELECT 1 FROM travel_v2.payout_items pi
      WHERE pi.commission_id = cm.id
  )
ON CONFLICT (commission_id) DO NOTHING;


-- 5. Đánh dấu commission/payout đầu tiên đã thanh toán thành công.
UPDATE travel_v2.commissions cm
SET status = 'PAID', updated_at = NOW()
FROM travel_v2.payout_items pi
WHERE pi.commission_id = cm.id
  AND pi.payout_id = 'd1000000-0000-4000-8000-000000000001'
  AND cm.status = 'PAYABLE';

UPDATE travel_v2.payouts
SET payout_status = 'COMPLETED',
    payment_reference = 'TEST-TRANSFER-COMPLETED-001',
    processed_at = NOW()
WHERE id = 'd1000000-0000-4000-8000-000000000001'
  AND EXISTS (
      SELECT 1 FROM travel_v2.payout_items pi
      WHERE pi.payout_id = travel_v2.payouts.id
  );

-- Payout thứ hai là FAILED, giữ commission ở trạng thái PAYABLE.
UPDATE travel_v2.payouts
SET payout_status = 'FAILED',
    payment_reference = 'TEST-TRANSFER-FAILED-002',
    processed_at = NULL
WHERE id = 'd1000000-0000-4000-8000-000000000002'
  AND EXISTS (
      SELECT 1 FROM travel_v2.payout_items pi
      WHERE pi.payout_id = travel_v2.payouts.id
  );

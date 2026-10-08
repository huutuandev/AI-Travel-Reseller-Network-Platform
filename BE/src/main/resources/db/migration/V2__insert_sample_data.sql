-- 1. Insert Roles
INSERT INTO roles (id, name, description) 
VALUES ('a0000000-0000-0000-0000-000000000001', 'RESELLER', 'Vai trò Reseller'),
       ('a0000000-0000-0000-0000-000000000002', 'MERCHANT', 'Vai trò Merchant')
ON CONFLICT (name) DO NOTHING;

-- 2. Insert Users
INSERT INTO users (id, email, password_hash, full_name, phone, status)
VALUES ('b0000000-0000-0000-0000-000000000001', 'reseller@example.com', 'bcrypt_hash_placeholder', 'Nguyen Van Reseller', '0901234567', 'ACTIVE'),
       ('b0000000-0000-0000-0000-000000000002', 'merchant@example.com', 'bcrypt_hash_placeholder', 'Tran Thi Merchant', '0912345678', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

-- 3. Insert User Roles
INSERT INTO user_roles (user_id, role_id)
VALUES ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001'),
       ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- 4. Insert Merchant
INSERT INTO merchants (id, user_id, name, logo_url, phone, address, website, verification_status)
VALUES ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'Bana Hills', 'https://example.com/logo.png', '0912345678', 'Da Nang', 'https://banahills.sunworld.vn', 'VERIFIED')
ON CONFLICT (user_id) DO NOTHING;

-- 5. Insert Reseller
INSERT INTO resellers (id, user_id, referral_code, bank_account_name, bank_account_number, bank_name, status)
VALUES ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'RESELLER_001', 'NGUYEN VAN RESELLER', '1234567890', 'Vietcombank', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

-- 6. Insert Product
INSERT INTO products (id, merchant_id, name, category, description, original_price, sale_price, booking_url, status)
VALUES ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Vé Cáp Treo Bana Hills', 'VE_THAM_QUAN', 'Vé tham quan Bana Hills 1 ngày trọn gói.', 950000, 900000, 'https://banahills.example.com/booking', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 7. Insert Campaign
INSERT INTO campaigns (id, merchant_id, product_id, name, start_date, end_date, commission_rate, reseller_split_rate, guidelines, status)
VALUES ('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Chiến dịch bùng nổ Bana Hills Hè 2026', '2026-05-01', '2026-09-30', 10.00, 60.00, 'Chỉ áp dụng cho vé người lớn.', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

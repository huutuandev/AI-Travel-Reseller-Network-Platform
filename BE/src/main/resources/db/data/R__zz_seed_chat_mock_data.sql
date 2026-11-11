-- Mock chat data for Swagger testing.
-- Runs after R__seed_test_data.sql because "zz_" sorts after "seed_test_data".
-- These are development fixtures, not production tracking links.

-- Approved reseller applications for three existing active campaigns.
INSERT INTO campaign_applications (
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
    '60000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'APPROVED',
    'PERCENTAGE',
    15,
    NOW(),
    NOW()
),
(
    '60000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    'APPROVED',
    'PERCENTAGE',
    12,
    NOW(),
    NOW()
),
(
    '60000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000003',
    'APPROVED',
    'PERCENTAGE',
    10,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;


-- Mock tracking codes to enter in the Swagger trackingLink field.
INSERT INTO tracking_links (
    id,
    campaign_application_id,
    reseller_id,
    tracking_code,
    target_url,
    status
)
VALUES
(
    '70000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'MOCK-BANA-001',
    'https://aitravel.vn/products/tour-ba-na-hills',
    'ACTIVE'
),
(
    '70000000-0000-0000-0000-000000000002',
    '60000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    'MOCK-HOIAN-001',
    'https://aitravel.vn/products/tour-pho-co-hoi-an',
    'ACTIVE'
),
(
    '70000000-0000-0000-0000-000000000003',
    '60000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000003',
    'MOCK-HUE-001',
    'https://aitravel.vn/products/tour-co-do-hue',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;


-- Product-specific knowledge for RAG.
INSERT INTO knowledge_documents (
    id,
    merchant_id,
    product_id,
    title,
    content,
    status
)
VALUES
(
    '80000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    'Thông tin kiểm thử — Tour Bà Nà Hills 1 ngày',
    'Dữ liệu kiểm thử. Sản phẩm: Tour Bà Nà Hills 1 ngày. '
    || 'Mô tả: Khám phá Cầu Vàng, làng Pháp và Bà Nà Hills. '
    || 'Giá niêm yết trong dữ liệu mẫu: 1.200.000 VND. '
    || 'Giá ưu đãi trong dữ liệu mẫu: 1.050.000 VND. '
    || 'Đường dẫn checkout mẫu: https://example.com/checkout/ba-na-hills',
    'ACTIVE'
),
(
    '80000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000002',
    'Thông tin kiểm thử — Tour phố cổ Hội An',
    'Dữ liệu kiểm thử. Sản phẩm: Tour phố cổ Hội An. '
    || 'Mô tả: Khám phá phố cổ Hội An và văn hóa địa phương. '
    || 'Giá niêm yết trong dữ liệu mẫu: 650.000 VND. '
    || 'Giá ưu đãi trong dữ liệu mẫu: 550.000 VND. '
    || 'Đường dẫn checkout mẫu: https://example.com/checkout/hoi-an',
    'ACTIVE'
),
(
    '80000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000003',
    'Thông tin kiểm thử — Tour khám phá Cố đô Huế',
    'Dữ liệu kiểm thử. Sản phẩm: Tour khám phá Cố đô Huế. '
    || 'Mô tả: Tham quan Đại Nội, chùa Thiên Mụ và di tích lịch sử. '
    || 'Giá niêm yết trong dữ liệu mẫu: 950.000 VND. '
    || 'Giá ưu đãi trong dữ liệu mẫu: 850.000 VND. '
    || 'Đường dẫn checkout mẫu: https://example.com/checkout/hue',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;
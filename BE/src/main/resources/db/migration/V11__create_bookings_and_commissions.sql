-- V11: Create bookings and commissions
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_status CHECK (
        booking_status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'REFUNDED')
    )
);
CREATE INDEX idx_bookings_merchant_created ON bookings(merchant_id, created_at DESC);
CREATE INDEX idx_bookings_product_id ON bookings(product_id);
CREATE INDEX idx_bookings_tracking_link_id ON bookings(tracking_link_id);
CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);

CREATE TABLE commissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE RESTRICT,
    reseller_id UUID NOT NULL REFERENCES reseller_profiles(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_commissions_status CHECK (
        status IN ('PENDING', 'APPROVED', 'PAYABLE', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'REVERSED')
    )
);
CREATE INDEX idx_commissions_reseller_id ON commissions(reseller_id);
CREATE INDEX idx_commissions_booking_id ON commissions(booking_id);

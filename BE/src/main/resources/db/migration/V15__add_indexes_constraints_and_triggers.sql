-- V15: Functions, triggers, and technical constraints

-- 1. Timestamp update trigger
CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := clock_timestamp();
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

CREATE TRIGGER trg_social_accounts_touch BEFORE UPDATE ON social_accounts
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_products_touch BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_campaigns_touch BEFORE UPDATE ON campaigns
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_vouchers_touch BEFORE UPDATE ON vouchers
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_rcp_touch BEFORE UPDATE ON reseller_campaign_posts
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_bookings_touch BEFORE UPDATE ON bookings
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_commissions_touch BEFORE UPDATE ON commissions
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_ai_contents_touch BEFORE UPDATE ON ai_contents
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TRIGGER trg_knowledge_touch BEFORE UPDATE ON knowledge_documents
FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- 2. Campaign application snapshot triggers
CREATE OR REPLACE FUNCTION copy_commission_snapshot() RETURNS TRIGGER AS $$
DECLARE
    camp_type VARCHAR(20);
    camp_rate NUMERIC(10,2);
BEGIN
    SELECT commission_type, commission_rate
    INTO camp_type, camp_rate
    FROM campaigns WHERE id = NEW.campaign_id;

    IF camp_type IS NULL THEN
        RAISE EXCEPTION 'Referenced campaign % not found', NEW.campaign_id;
    END IF;

    IF NEW.commission_type_snapshot IS NULL THEN
        NEW.commission_type_snapshot := camp_type;
    END IF;
    IF NEW.commission_rate_snapshot IS NULL THEN
        NEW.commission_rate_snapshot := camp_rate;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_copy_commission_snapshot
BEFORE INSERT ON campaign_applications
FOR EACH ROW EXECUTE FUNCTION copy_commission_snapshot();

CREATE OR REPLACE FUNCTION protect_commission_snapshot() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.commission_type_snapshot IS DISTINCT FROM OLD.commission_type_snapshot
       OR NEW.commission_rate_snapshot IS DISTINCT FROM OLD.commission_rate_snapshot THEN
        RAISE EXCEPTION 'Commission snapshot values cannot be modified once set';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_commission_snapshot
BEFORE UPDATE OF commission_type_snapshot, commission_rate_snapshot ON campaign_applications
FOR EACH ROW EXECUTE FUNCTION protect_commission_snapshot();

-- 3. Scope validation triggers
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

CREATE OR REPLACE FUNCTION validate_tracking_link_reseller() RETURNS TRIGGER AS $$
DECLARE
    app_reseller UUID;
BEGIN
    SELECT ca.reseller_id INTO app_reseller
    FROM campaign_applications ca
    WHERE ca.id = NEW.campaign_application_id;

    IF app_reseller IS DISTINCT FROM NEW.reseller_id THEN
        RAISE EXCEPTION 'Tracking link reseller_id must match campaign application reseller';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_tracking_link_reseller
BEFORE INSERT OR UPDATE OF campaign_application_id, reseller_id ON tracking_links
FOR EACH ROW EXECUTE FUNCTION validate_tracking_link_reseller();

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
    IF source_reseller IS NOT NULL AND source_reseller IS DISTINCT FROM NEW.reseller_id THEN
        RAISE EXCEPTION 'Commission reseller must match booking tracking source';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_commission_reseller
BEFORE INSERT OR UPDATE OF booking_id, reseller_id ON commissions
FOR EACH ROW EXECUTE FUNCTION validate_commission_reseller();

-- 4. Payout item validation and auto total triggers
CREATE OR REPLACE FUNCTION validate_payout_item() RETURNS TRIGGER AS $$
DECLARE
    p_merchant UUID;
    p_reseller UUID;
    p_currency CHAR(3);
    p_status VARCHAR(20);
    b_merchant UUID;
    c_reseller UUID;
    c_amount NUMERIC(12,2);
    c_currency CHAR(3);
    c_status VARCHAR(20);
BEGIN
    SELECT merchant_id, reseller_id, currency, payout_status
    INTO p_merchant, p_reseller, p_currency, p_status
    FROM payouts WHERE id = NEW.payout_id;

    IF p_status <> 'PROCESSING' THEN
        RAISE EXCEPTION 'Cannot add or modify items on payout with status % (must be PROCESSING)', p_status;
    END IF;

    SELECT b.merchant_id, c.reseller_id, c.amount, c.currency, c.status
    INTO b_merchant, c_reseller, c_amount, c_currency, c_status
    FROM commissions c JOIN bookings b ON b.id = c.booking_id
    WHERE c.id = NEW.commission_id;

    IF c_status <> 'PAYABLE' THEN
        RAISE EXCEPTION 'Only PAYABLE commissions can be added to payouts (commission status: %)', c_status;
    END IF;

    IF p_merchant IS DISTINCT FROM b_merchant
       OR p_reseller IS DISTINCT FROM c_reseller
       OR p_currency IS DISTINCT FROM c_currency THEN
        RAISE EXCEPTION 'Payout item must match commission merchant, reseller and currency';
    END IF;

    -- Automatically set item amount to commission amount to avoid client tampering
    NEW.amount := c_amount;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_payout_item
BEFORE INSERT OR UPDATE OF payout_id, commission_id, amount ON payout_items
FOR EACH ROW EXECUTE FUNCTION validate_payout_item();

CREATE OR REPLACE FUNCTION refresh_payout_total() RETURNS TRIGGER AS $$
DECLARE
    target_payout_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_payout_id := OLD.payout_id;
    ELSE
        target_payout_id := NEW.payout_id;
    END IF;

    UPDATE payouts
    SET total_amount = COALESCE((
        SELECT SUM(amount) FROM payout_items WHERE payout_id = target_payout_id
    ), 0)
    WHERE id = target_payout_id;

    IF TG_OP = 'UPDATE' AND OLD.payout_id IS DISTINCT FROM NEW.payout_id THEN
        UPDATE payouts
        SET total_amount = COALESCE((
            SELECT SUM(amount) FROM payout_items WHERE payout_id = OLD.payout_id
        ), 0)
        WHERE id = OLD.payout_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_refresh_payout_total
AFTER INSERT OR UPDATE OR DELETE ON payout_items
FOR EACH ROW EXECUTE FUNCTION refresh_payout_total();

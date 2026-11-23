-- The application uses the travel_v2 schema explicitly, but the V15 trigger
-- resolves an unqualified campaigns table through the connection search_path.
CREATE OR REPLACE FUNCTION travel_v2.copy_commission_snapshot() RETURNS TRIGGER AS $$
DECLARE
    camp_type VARCHAR(20);
    camp_rate NUMERIC(10,2);
BEGIN
    SELECT commission_type, commission_rate
    INTO camp_type, camp_rate
    FROM travel_v2.campaigns WHERE id = NEW.campaign_id;

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

-- DEVELOPMENT ONLY: merchant-list-fixture-20261029-v1
-- Not a Flyway migration. Do not place this file in db/redesign or db/migration.
-- Requires V1 + V2 in travel_v2, database mydatabase.
-- Review connection first. Run the entire file in one session.
-- Default PREVIEW does not write rows. Change action below to SEED or CLEANUP explicitly.
-- With psql use: psql -X -v ON_ERROR_STOP=1 -d mydatabase -f BE/scripts/dev/merchant_test_data.sql
-- Supply host/user through your normal connection configuration; never put passwords here.
-- SEED inserts exactly 15 users + 15 merchants. A repeated SEED fails atomically;
-- it never upserts/overwrites an existing record. CLEANUP then SEED resets this fixture.
-- CLEANUP deletes only positively identified fixture merchants/users; no CASCADE.
-- Related products, branding, events, etc. prevent deletion through existing FK RESTRICT.
-- In particular, status API tests may create platform_events: review them separately.
-- This script deliberately does not delete those events or other dependent records.
-- A failed statement requires ROLLBACK in interactive clients before continuing.
-- Constraints checked against source V1/V2, not against the live server in this task:
-- users: UUID PK/default; email NOT NULL + unique lower(email); password_hash/full_name/
-- status NOT NULL; created_at/updated_at NOT NULL/default now(); phone/email_verified_at nullable.
-- merchants: UUID PK/default; user_id NOT NULL FK users ON DELETE RESTRICT (not UNIQUE);
-- code NOT NULL UNIQUE; name/contact_email/subdomain/status NOT NULL;
-- lower(subdomain) UNIQUE; lower(custom_domain) UNIQUE WHERE non-null;
-- description/phone/address/custom_domain nullable; timestamps NOT NULL/default now();
-- version BIGINT NOT NULL DEFAULT 0 from V2.
-- Password marker is deliberately NOT a valid BCrypt hash and has no matching password.
-- All fixture users are also SUSPENDED and have no role. They are FK owners only.

BEGIN;
DO $fixture$
DECLARE
    action CONSTANT text := 'SEED'; -- PREVIEW | SEED | CLEANUP
    marker CONSTANT text := 'DEV_ONLY:merchant-list-fixture-20261029-v1';
    names CONSTANT text[] := ARRAY[
        'Amber Travel Demo', 'Birch Travel Demo', 'Cobalt Travel Demo',
        'Dune Travel Demo', 'Ember Travel Demo', 'Fern Travel Demo',
        'Grove Tours Demo', 'Harbor Journeys Demo', 'Indigo Escapes Demo',
        'Juniper Travel Demo', 'Kite Travel Demo', 'Lagoon Travel Demo',
        'Maple Holidays Demo', 'Nimbus Tours Demo', 'Opal Adventures Demo'
    ];
    n integer;
    suffix text;
    fixture_user_id uuid;
    fixture_merchant_id uuid;
    owner_email text;
    tenant_code text;
    tenant_subdomain text;
    created_time timestamptz;
BEGIN
    IF action NOT IN ('PREVIEW', 'SEED', 'CLEANUP') THEN
        RAISE EXCEPTION 'Invalid fixture action';
    END IF;
    IF current_database() <> 'mydatabase' THEN
        RAISE EXCEPTION 'Wrong database: expected mydatabase. Verify the DEVELOPMENT connection.';
    END IF;
    IF to_regclass('travel_v2.users') IS NULL OR to_regclass('travel_v2.merchants') IS NULL
       OR to_regclass('travel_v2.platform_events') IS NULL
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema='travel_v2' AND table_name='merchants' AND column_name='version') THEN
        RAISE EXCEPTION 'Required V1/V2 objects are missing; this script never runs migrations.';
    END IF;
    IF action = 'PREVIEW' THEN
        RAISE NOTICE 'PREVIEW only. Review SELECT output. Choose SEED or CLEANUP explicitly after verifying development environment.';
        RETURN;
    END IF;

    FOR n IN 1..15 LOOP
        suffix := lpad(n::text, 2, '0');
        fixture_user_id := ('c80f2026-1029-4000-8000-' || lpad(n::text,12,'0'))::uuid;
        fixture_merchant_id := ('c80f2026-1029-4000-9000-' || lpad(n::text,12,'0'))::uuid;
        owner_email := 'dev-merchant-fixture-' || suffix || '@example.test';
        tenant_code := 'DEV_MERCHANT_FIXTURE_20261029_' || suffix;
        tenant_subdomain := 'dev-merchant-fixture-20261029-' || suffix;
        created_time := TIMESTAMPTZ '2026-10-01 09:00:00+07' + (n - 1) * INTERVAL '1 day';

        IF action = 'SEED' THEN
            -- Plain INSERT: any UUID/email/code/subdomain collision aborts the entire transaction.
            INSERT INTO travel_v2.users
                (id,email,password_hash,full_name,status,created_at,updated_at)
            VALUES
                (fixture_user_id,owner_email,'!DISABLED:merchant-list-fixture-20261029-v1',
                 'Development Fixture Owner ' || suffix,'SUSPENDED',created_time,created_time);

            INSERT INTO travel_v2.merchants
                (id,user_id,code,name,description,contact_email,subdomain,status,
                 created_at,updated_at,version)
            VALUES
                (fixture_merchant_id,fixture_user_id,tenant_code,names[n],marker,
                 'dev-merchant-contact-' || suffix || '@example.test',tenant_subdomain,
                 CASE WHEN n <= 9 THEN 'ACTIVE' ELSE 'SUSPENDED' END,
                 created_time,created_time,0);
        ELSE
            -- Do not delete an existing row unless ALL identity markers match.
            -- Merchant status/version may change during API tests; neither is an identity marker.
            IF EXISTS (SELECT 1 FROM travel_v2.merchants m WHERE m.id=fixture_merchant_id)
               AND NOT EXISTS (
                   SELECT 1 FROM travel_v2.merchants m
                   WHERE m.id=fixture_merchant_id AND m.user_id=fixture_user_id AND m.code=tenant_code
                     AND m.subdomain=tenant_subdomain AND m.description=marker
                     AND m.contact_email='dev-merchant-contact-' || suffix || '@example.test'
               ) THEN
                RAISE EXCEPTION 'Merchant identity mismatch at fixture index %. Nothing will be committed.', n;
            END IF;
            IF EXISTS (SELECT 1 FROM travel_v2.users u WHERE u.id=fixture_user_id)
               AND NOT EXISTS (
                   SELECT 1 FROM travel_v2.users u WHERE u.id=fixture_user_id AND u.email=owner_email
                     AND u.full_name='Development Fixture Owner ' || suffix
                     AND u.password_hash='!DISABLED:merchant-list-fixture-20261029-v1'
                     AND u.status='SUSPENDED'
               ) THEN
                RAISE EXCEPTION 'User identity mismatch at fixture index %. Nothing will be committed.', n;
            END IF;
            -- Protect even ON DELETE SET NULL relationships from implicit changes.
            IF EXISTS (SELECT 1 FROM travel_v2.bookings b WHERE b.customer_id=fixture_user_id)
               OR EXISTS (SELECT 1 FROM travel_v2.user_roles ur WHERE ur.user_id=fixture_user_id)
               OR EXISTS (SELECT 1 FROM travel_v2.reseller_profiles r WHERE r.user_id=fixture_user_id)
               OR EXISTS (SELECT 1 FROM travel_v2.platform_settings s WHERE s.updated_by=fixture_user_id)
               OR EXISTS (SELECT 1 FROM travel_v2.platform_events e WHERE e.actor_user_id=fixture_user_id)
               OR EXISTS (SELECT 1 FROM travel_v2.merchants m WHERE m.user_id=fixture_user_id AND m.id<>fixture_merchant_id) THEN
                RAISE EXCEPTION 'Fixture user % has dependent data; cleanup refused.', n;
            END IF;
            DELETE FROM travel_v2.merchants m
            WHERE m.id=fixture_merchant_id AND m.user_id=fixture_user_id AND m.code=tenant_code
              AND m.subdomain=tenant_subdomain AND m.description=marker;
            DELETE FROM travel_v2.users u
            WHERE u.id=fixture_user_id AND u.email=owner_email
              AND u.full_name='Development Fixture Owner ' || suffix
              AND u.password_hash='!DISABLED:merchant-list-fixture-20261029-v1'
              AND u.status='SUSPENDED';
        END IF;
    END LOOP;
    RAISE NOTICE 'Fixture action % completed inside transaction.', action;
END;
$fixture$;

-- Exact namespace/identity matching; never select password_hash or credentials.
-- After SEED: users=15, merchants=15, active=9, suspended=6, initial_versions=15.
-- After CLEANUP: all counts below must be zero.
WITH expected AS (
    SELECT ('c80f2026-1029-4000-8000-' || lpad(n::text,12,'0'))::uuid AS uid,
           ('c80f2026-1029-4000-9000-' || lpad(n::text,12,'0'))::uuid AS mid,
           lpad(n::text,2,'0') AS suffix
    FROM generate_series(1,15) n
)
SELECT
    (SELECT count(*) FROM expected e JOIN travel_v2.users u ON u.id=e.uid
     WHERE u.email='dev-merchant-fixture-' || e.suffix || '@example.test') AS fixture_users,
    count(m.id) AS fixture_merchants,
    count(*) FILTER (WHERE m.status='ACTIVE') AS active_merchants,
    count(*) FILTER (WHERE m.status='SUSPENDED') AS suspended_merchants,
    count(*) FILTER (WHERE m.version=0) AS initial_versions
FROM expected e LEFT JOIN travel_v2.merchants m
  ON m.id=e.mid AND m.user_id=e.uid
 AND m.code='DEV_MERCHANT_FIXTURE_20261029_' || e.suffix
 AND m.description='DEV_ONLY:merchant-list-fixture-20261029-v1';

-- Listing in the API's order. Expected 15 rows; newest is fixture 15.
SELECT m.id,m.code,m.name,m.subdomain,m.status,m.created_at,m.version
FROM generate_series(1,15) n
JOIN travel_v2.merchants m
 ON m.id=('c80f2026-1029-4000-9000-' || lpad(n::text,12,'0'))::uuid
AND m.code='DEV_MERCHANT_FIXTURE_20261029_' || lpad(n::text,2,'0')
WHERE m.description='DEV_ONLY:merchant-list-fixture-20261029-v1'
ORDER BY m.created_at DESC,m.id;

-- Name-search test: expected 9 Travel merchants (6 ACTIVE, 3 SUSPENDED).
SELECT m.status,count(*) AS travel_matches
FROM generate_series(1,15) n
JOIN travel_v2.merchants m
 ON m.id=('c80f2026-1029-4000-9000-' || lpad(n::text,12,'0'))::uuid
AND m.code='DEV_MERCHANT_FIXTURE_20261029_' || lpad(n::text,2,'0')
WHERE m.description='DEV_ONLY:merchant-list-fixture-20261029-v1'
  AND strpos(lower(m.name),'travel')>0
GROUP BY m.status ORDER BY m.status;

-- Expected 0: fixture users must not have ANY assigned role, including ROLE_ADMIN.
SELECT count(*) AS fixture_role_assignments
FROM generate_series(1,15) n
JOIN travel_v2.users u
 ON u.id=('c80f2026-1029-4000-8000-' || lpad(n::text,12,'0'))::uuid
AND u.email='dev-merchant-fixture-' || lpad(n::text,2,'0') || '@example.test'
JOIN travel_v2.user_roles ur ON ur.user_id=u.id;
COMMIT;
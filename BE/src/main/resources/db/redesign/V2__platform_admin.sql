ALTER TABLE merchants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE platform_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    route_mode VARCHAR(30) NOT NULL DEFAULT 'SUBDOMAIN_PREVIEW'
        CHECK (route_mode = 'SUBDOMAIN_PREVIEW'),
    base_domain VARCHAR(253) NOT NULL DEFAULT 'aitravel.example',
    caption_template TEXT NOT NULL DEFAULT '',
    video_template TEXT NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
INSERT INTO platform_settings(id) VALUES (1);

CREATE TABLE platform_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID REFERENCES merchants(id) ON DELETE RESTRICT,
    actor_user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    event_type VARCHAR(80) NOT NULL,
    area VARCHAR(40) NOT NULL,
    result VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_platform_events_time ON platform_events(created_at DESC, id);
CREATE INDEX idx_platform_events_merchant_time ON platform_events(merchant_id, created_at DESC, id);

INSERT INTO roles(name, scope, description)
VALUES ('ROLE_ADMIN', 'PLATFORM', 'Platform administrator')
ON CONFLICT (name) DO NOTHING;

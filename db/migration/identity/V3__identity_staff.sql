-- Phase 1 identity: per-shop staff users (Keycloak subjects mapped to a shop) and their RBAC roles.
-- These are TENANT tables — sharded and indexed by shop_id. Token issuance/validation is delegated to
-- Keycloak at the gateway; staff_user is the authorization model the monolith reads, not an
-- authentication store. Versioned V3 so it never collides with the other per-shard tenant migrations
-- (platform V1, hello V2) when they share one physical database in dev/test; control is V100+.

CREATE TABLE staff_user (
    shop_id      BIGINT       NOT NULL,
    subject      TEXT         NOT NULL,   -- Keycloak 'sub' claim; unique per shop
    display_name TEXT         NOT NULL,
    email        TEXT         NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_staff_user PRIMARY KEY (shop_id, subject),
    CONSTRAINT uq_staff_user_email UNIQUE (shop_id, email)
);
-- Every tenant table is indexed by shop_id.
CREATE INDEX idx_staff_user_shop ON staff_user (shop_id);

CREATE TABLE staff_user_role (
    shop_id BIGINT      NOT NULL,
    subject TEXT        NOT NULL,
    role    VARCHAR(32) NOT NULL,
    CONSTRAINT pk_staff_user_role PRIMARY KEY (shop_id, subject, role),
    -- Same-shard composite FK (shop_id is part of the key) — never a cross-shard reference.
    CONSTRAINT fk_staff_user_role_user FOREIGN KEY (shop_id, subject)
        REFERENCES staff_user (shop_id, subject) ON DELETE CASCADE
);
CREATE INDEX idx_staff_user_role_shop ON staff_user_role (shop_id);

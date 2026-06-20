-- Control-plane schema: GLOBAL and NON-sharded. In production it lives on a dedicated control
-- database; in local/dev and single-node Testcontainers it shares shard 0's Postgres. It is NOT
-- sharded by shop_id because this is precisely how a shop is resolved to a shard — it cannot itself
-- require the shard. Versioned at V100 so it never collides with the per-shard tenant migrations
-- (platform V1, hello V2) when both share one physical database in dev/test.

-- The shop registry: the system of record for tenants. id IS the ShopId value.
CREATE TABLE shop_registry (
    id             BIGSERIAL    NOT NULL,
    name           TEXT         NOT NULL,
    plan           VARCHAR(32)  NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    locale         VARCHAR(35)  NOT NULL,
    primary_domain TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_shop_registry PRIMARY KEY (id),
    CONSTRAINT uq_shop_registry_primary_domain UNIQUE (primary_domain)
);

-- host -> shop map for storefront tenant resolution. Hosts are globally unique.
CREATE TABLE shop_domain (
    host       TEXT     NOT NULL,
    shop_id    BIGINT   NOT NULL,
    is_primary BOOLEAN  NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_shop_domain PRIMARY KEY (host),
    CONSTRAINT fk_shop_domain_shop FOREIGN KEY (shop_id) REFERENCES shop_registry (id)
);
CREATE INDEX idx_shop_domain_shop ON shop_domain (shop_id);

-- shop -> shard map. Source of truth for routing (Phase 1 still computes it deterministically).
CREATE TABLE shop_shard_assignment (
    shop_id     BIGINT       NOT NULL,
    shard_index INTEGER      NOT NULL,
    assigned_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_shop_shard_assignment PRIMARY KEY (shop_id),
    CONSTRAINT fk_shop_shard_assignment_shop FOREIGN KEY (shop_id) REFERENCES shop_registry (id)
);

-- Control-plane transactional outbox. Written in the same control transaction as the shop change
-- that raised the event (e.g. ShopCreated); drained idempotently by the job-engine (keyed event_id).
CREATE TABLE control_outbox (
    event_id    UUID         NOT NULL,
    shop_id     BIGINT       NOT NULL,
    event_type  TEXT         NOT NULL,
    payload     TEXT         NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_control_outbox PRIMARY KEY (event_id)
);
CREATE INDEX idx_control_outbox_pending ON control_outbox (status, occurred_at);

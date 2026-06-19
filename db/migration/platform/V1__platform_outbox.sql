-- Transactional outbox. Written in the same transaction as the state change that raised the
-- event; drained asynchronously and idempotently by the job-engine (keyed on event_id).
CREATE TABLE platform_outbox (
    event_id    UUID         NOT NULL,
    shop_id     BIGINT       NOT NULL,
    event_type  TEXT         NOT NULL,
    payload     TEXT         NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_platform_outbox PRIMARY KEY (event_id)
);

-- Drain query filters by status + orders by time.
CREATE INDEX idx_platform_outbox_pending ON platform_outbox (status, occurred_at);
-- Every tenant-bearing table is indexed by shop_id.
CREATE INDEX idx_platform_outbox_shop ON platform_outbox (shop_id);

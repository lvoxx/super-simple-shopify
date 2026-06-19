-- Phase 0 vertical-slice table. A trivial tenant row the hello endpoint reads to prove the
-- tenant -> shard -> read path. shop_id is the tenant key (mandatory on every tenant table).
CREATE TABLE hello_message (
    shop_id BIGINT NOT NULL,
    message TEXT   NOT NULL,
    CONSTRAINT pk_hello_message PRIMARY KEY (shop_id)
);

-- Seed a greeting for the demo tenant (shop 1).
INSERT INTO hello_message (shop_id, message) VALUES (1, 'Hello from shop 1');

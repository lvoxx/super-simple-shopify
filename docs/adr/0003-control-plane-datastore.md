# ADR 0003 — A separate control-plane datastore for the shop registry and shard map

- Status: Accepted
- Date: 2026-06-20

## Context

Phase 1 introduces shops (tenants). Every tenant table is sharded by `shop_id`, and
`platform-persistence` routes each query to a shard via `TenantContext` (`ROADMAP §1c`). But two
pieces of data cannot live on a shard:

1. **The shop registry** — to serve a storefront request you must resolve a host/domain to a `shop_id`
   *before* any tenant context exists. A sharded lookup is circular: you would need the shard to find
   the shard.
2. **The shop→shard map** — the very mapping that says which shard owns a shop.

`ROADMAP §1 Phase 1` calls this "a shop → shard mapping table on a control plane."

## Decision

Introduce a single, **non-sharded control-plane datastore**, wired in `platform-persistence`
alongside the tenant routing datasource:

- `ControlPlaneConfig` defines a second `DataSource` (`controlDataSource`), its own
  `SqlSessionFactory`/`SqlSessionTemplate`, and a named `controlTransactionManager`. Connection
  settings come from `platform.persistence.control.*`; when absent (local/dev, single-node tests) it
  falls back to shard 0's Postgres.
- Mappers opt in with `@ControlMapper` (deliberately **not** meta-annotated `@Mapper`), so the tenant
  mapper scanner ignores them and they bind only to the control factory. A mapper is tenant **or**
  control, never both.
- Because a second `DataSource` makes Spring Boot's single-candidate MyBatis/DataSource/transaction
  autoconfiguration back off, the **tenant** MyBatis stack (autoconfigured in Phase 0) is now declared
  explicitly in `PersistenceConfig` as the `@Primary` beans.
- Control-plane tables: `shop_registry`, `shop_domain`, `shop_shard_assignment`, `control_outbox`
  (`db/migration/control`, versioned `V100+` to avoid colliding with per-shard migrations).
- Shop lifecycle events use a **control-plane outbox** (`ControlPlaneEventPublisher` →
  `control_outbox`, drained by `ControlOutboxDrainer`). This keeps shop creation a single-datasource
  transaction (shop + shard assignment + event commit together) — **no distributed transaction** to a
  tenant shard.
- Shard assignment is **recorded** on shop creation but routing keeps using the deterministic
  `ShardResolver` (`shopId mod shardCount`); Phase 8 switches routing to a cached lookup of
  `shop_shard_assignment`.

## Consequences

- The "no cross-shard join/FK on the transactional path" rule is honoured: the FKs in the control
  schema are within the single control database, not across shards.
- The control plane is a new operational surface: it migrates **once** per deploy
  (`infra/migration/migrate-control.sh`), separately from `migrate-all-shards.sh`.
- Provisioning endpoints (`/api/v1/control/**`) are intentionally non-tenant: the tenant-binding
  filter skips them. In production they sit behind the gateway + Keycloak (operators only); the
  monolith is not its own resource server.
- Two datasources add wiring, but the boundary is clean and the routing hot path is unchanged.

# ADR 0003 — `shop_id` is the sharding key

- Status: Accepted
- Date: 2026-06-19

## Context

`ROADMAP.md §5` calls out the sharding-key choice as ADR-worthy, and `README.md` ("Scale Data
First") mandates that every tenant table carry `shop_id` from day one. We need one explicit,
recorded decision the whole data layer routes on.

## Decision

The shard key is **`shop_id`** (the `ShopId` tenant identifier).

- `ShardResolver` maps a shop to a shard with `Math.floorMod(shopId, shardCount)` — deterministic,
  so a shop's data always lives on one shard.
- `TenantRoutingDataSource` derives the active shard from `TenantContext` (the bound `ShopId`),
  so application code never selects a datasource and a missing tenant is a hard failure.
- Phase 0 runs `shard-count=1`; the routing path is identical for `shard-count > 1` (proven in
  Phase 8). No cross-shard joins or FKs on the transactional path.

## Consequences

- A single shop never spans shards, so all of a shop's transactional data is co-located and
  joinable within its shard.
- Rebalancing/splitting a hot shop is out of scope (a shop is the unit of locality); if one shop
  ever outgrows a shard, that is a new ADR.
- Cross-shop analytics must go through async/replica/projection paths, never a transactional
  cross-shard query.

# ADR 0002 — Flyway (not Liquibase) for schema migrations

- Status: Accepted
- Date: 2026-06-19

## Context

`ROADMAP.md` and `README.md §4/§8` require versioned, per-module, forward-only migrations
**applied by a dedicated infra migration container — never by the Spring app at startup**.
They name "Flyway/Liquibase" without picking one. We must pick one to scaffold the
migration container and the `db/migration/<module>` layout.

## Decision

Use **Flyway**.

- Migrations are plain, reviewable **SQL** — the same ethos that made us choose MyBatis over
  JPA (`README.md §4`): explicit SQL, no abstraction hiding the statement that runs.
- Per-module scripts live under `db/migration/<module>` and are applied **shard-aware**
  across every shard by the infra migration container/job (Phase 9 wires it into the
  pipeline; Phase 0 only scaffolds it).
- The Spring app declares **no** Flyway runner bean and sets nothing that runs migrations at
  startup; it assumes the schema already exists.

## Consequences

- Flyway's SQL-first model maps cleanly onto "one folder of `V__*.sql` per module".
- Repeatable/undo features are deliberately unused — migrations are forward-only.
- If a future need (complex DB-vendor-neutral changesets) justifies Liquibase, that is a new
  ADR superseding this one.

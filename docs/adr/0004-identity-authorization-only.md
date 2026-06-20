# ADR 0004 — `identity` models authorization only; authentication is delegated to Keycloak

- Status: Accepted
- Date: 2026-06-20

## Context

`ROADMAP.md` Phase 1 lists the `identity` deliverable as "StaffUser, Session,
ApiToken, Role/RBAC; **login + token issuance**". Taken literally that makes each
module — or at least `identity` — an authentication authority that mints and
validates credentials/tokens.

The deployed topology puts **Keycloak as a sidecar in the API gateway**. The gateway
authenticates every request at the edge and forwards trusted identity (shop id, user
subject, roles) into the monolith via injected headers. Having `identity` (or any
module) also issue/validate tokens would duplicate that responsibility, pull OAuth2
resource-server wiring into every module, and create two sources of truth for "who is
this caller".

## Decision

`identity` is an **authorization** module, not an authentication one.

1. It models **StaffUser** (a per-shop identity keyed by the Keycloak `subject`/`sub`
   claim) and the **RBAC roles** that user holds, and exposes `IdentityFacade` for
   other modules and the admin surface to resolve "who, in this shop, may do what".
2. It does **not** persist `Session` or `ApiToken`, and does **not** implement login or
   token issuance/validation. Those are Keycloak's job at the gateway. No module adds
   `spring-boot-starter-oauth2-resource-server`.
3. `staff_user` / `staff_user_role` are **tenant** tables (sharded + indexed by
   `shop_id`); `findBySubject` is tenant-scoped via `TenantContext`.
4. `StaffRole` is declared in `identity-api` (which may depend only on `platform-core`),
   intentionally mirroring `platform-security`'s coarse `Role`. The two are reconciled
   when the admin-auth wiring populates the security `Principal` from forwarded
   identity — a later Phase 1 slice — not by a cross-module dependency.

## Consequences

- The roadmap's "Session, ApiToken, login + token issuance" wording is **superseded**
  for this build: that surface lives in Keycloak, and the roadmap line is annotated
  accordingly.
- Modules stay free of per-module token validation; identity stays a small, tenant-scoped
  authorization model.
- A follow-up slice still owes: mapping forwarded roles onto the security `Principal`,
  and storefront host/domain → shop tenant resolution (`StoreFacade.findByDomain`).
- If a non-Keycloak credential ever becomes necessary (e.g. machine API tokens not
  modellable in Keycloak), that is a new decision requiring its own ADR.

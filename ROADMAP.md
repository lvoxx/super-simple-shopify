# ROADMAP — Build Checklist & Delivery Plan

This file is the **operating contract** for building the Shopify Clone modular monolith.

It has two jobs:
1. **The Gate** (§1–§3) — a checklist to run **before every implementation turn**, whether the author is a human dev or Claude. If any item fails, stop and fix the plan before writing code.
2. **The Plan** (§4) — the phased, checkbox-tracked delivery sequence with a Definition of Done per phase.

> Architecture rationale lives in [`README.md`](./README.md). This file is about *what to do next* and *what not to break*.

---

## 1. The Pre-Implementation Gate ✅

**Run this before writing or generating any code. Every turn. No exceptions.**

### 1a. Scope & placement
- [ ] I can name the **single module** this change belongs to. (If it spans modules, I'll split it.)
- [ ] The change lives in `*-impl` (or `*-api` if it's a contract change), **never** in another module's internals.
- [ ] If new behavior is cross-cutting (logging, validation, persistence base, ids, money), it belongs in `platform-*`, not copied into a module.

### 1b. Boundaries
- [ ] This module does **not** import another module's `*-impl` / `internal` package. (Maven won't even let it — confirm the dependency I'm about to add is an `*-api`/`*-spi`/`platform-*`.)
- [ ] Any data crossing a module boundary is a **record DTO** in `*-api`, not a JPA entity.
- [ ] Cross-module communication uses the right channel:
  - Needs an immediate answer AND must roll back together → **inject the `*-api` interface** (sync).
  - Side effect that must not fail/block the request → **publish a domain event** (async, via outbox).

### 1c. Data (Scale Data First)
- [ ] Every new tenant table has `shop_id NOT NULL` + an index on it.
- [ ] No query joins or FKs across shards. Cross-shop reads are async/replica-only.
- [ ] Reads default to read-only transactions; only command handlers open write transactions.
- [ ] If this read is hot and slow-changing, I've decided its **cache key, TTL, and the event that invalidates it**.

### 1d. Async & jobs
- [ ] Side effects (email, webhook, indexing, projections) are **events drained by `job-engine`**, not inline calls.
- [ ] Every job/event handler I add is **idempotent** (keyed on a stable id; re-delivery is a no-op).
- [ ] The event is published through the **outbox** (same transaction as the state change).

### 1e. Tenancy & context
- [ ] The path sets/propagates `TenantContext` (scoped value). Background jobs re-establish it from the payload.
- [ ] No silent fallback when tenant is missing — it's a hard failure.

### 1f. Tests & enforcement
- [ ] I'll add/extend a **module-slice test** that passes with only this module + `platform-*` on the classpath.
- [ ] `ApplicationModules.verify()` and ArchUnit rules will still pass.
- [ ] I own the Flyway migration under `db/migration/<module>` and haven't touched another module's schema.

### 1g. Monolith discipline
- [ ] I am **not** introducing a second deployable, a network hop, or a distributed transaction.
- [ ] If I *think* this module should be extracted, I've checked it against the §4 Phase 12 triggers — and it almost certainly is **not** time yet.

### 1h. Frontend work (applies during Phase 11 only)
- [ ] I'm calling **only** endpoints that exist in the **frozen v1 OpenAPI spec** — no undocumented or unreleased endpoints.
- [ ] Request/response types come from the **generated typed client**, not hand-written shapes that can drift.
- [ ] The frontend treats the backend as a black box via the published contract — no assumptions about modules, shards, or internals.
- [ ] Errors are handled from the documented `ProblemDetail` model; pagination, rate-limit, and idempotency headers are respected as documented.

> If three or more boxes above are unclear, the design isn't ready. **Stop and ask / redesign.** See §3 for hard stop triggers.

---

## 2. Definition of Done (every change)

A change is Done only when **all** of these hold:

- [ ] Code compiles in the reactor (`./mvnw verify`) with boundary checks green.
- [ ] Module-slice tests + relevant integration tests (Testcontainers) pass.
- [ ] `verify()` (Modulith) + ArchUnit boundary rules pass.
- [ ] New tenant tables carry `shop_id` + index; migration is module-owned and forward-only.
- [ ] New side effects go through the outbox; handlers are idempotent.
- [ ] Public surface changes are in `*-api` only; DTOs are records; events are sealed.
- [ ] Observability: new paths emit metrics/traces tagged with `module` + `shop_id`.
- [ ] README/module docs updated **in the same PR** if the contract or shape changed.

---

## 3. Hard Stop — Ask Before Proceeding 🛑

Stop and get explicit sign-off if a change would:

- Add a **second deployable** or split the monolith.
- Introduce a **synchronous cross-module call that can fail the request** without a clear "must roll back together" reason.
- Add a **cross-shard join, query, or FK** on a transactional path.
- Put a **JPA entity or persistence type in a `*-api`** module.
- Make a module depend on another module's `*-impl`.
- Add a side effect **inline** in a request handler instead of via an event.
- Introduce a **distributed transaction** or two-phase commit.
- Add a new third-party dependency **not pinned in `platform-bom`**.
- Build frontend against an **endpoint not in the frozen v1 OpenAPI spec**, or change a published `*-api` contract without a **version bump**.

---

## 4. Phased Delivery Plan

Phases are roughly sequential; later phases assume earlier ones are Done. Check boxes as you land them.
Each phase ends with a **Phase DoD** — don't start the next phase until it's met.

---

### Phase 0 — Foundation & Guardrails
*Goal: the skeleton that makes every later phase safe. No business features yet.*

- [ ] Reactor `pom.xml` with `<modules>`, `pluginManagement`, `maven-enforcer-plugin`.
- [ ] `platform-bom` pinning all internal + third-party versions (Spring Boot 4.x, Modulith, Flyway, Testcontainers…).
- [ ] `platform-core`: `Money`, `Result<T>`, typed `Id` value objects, `Clock`, sealed `DomainEvent` base.
- [ ] `platform-persistence`: shard-aware datasource routing, base repository, JPA/auditing config, Flyway runner.
- [ ] `platform-events`: event publisher + **transactional outbox** + externalization SPI (no-op for now).
- [ ] `platform-security`: auth filter scaffold, `Principal`, `TenantContext` as a **scoped value**, RBAC primitives.
- [ ] `platform-web`: `ProblemDetail` exception handling, API versioning, pagination, rate-limit filter.
- [ ] `platform-observability`: OTel tracing, Micrometer, structured JSON logging with `module`/`shop_id` tags.
- [ ] `platform-jobs`: `@Job`, `JobScheduler`, retry/backoff/dead-letter contracts.
- [ ] `job-engine`: worker runtime that drains the outbox and runs `@Job` handlers idempotently.
- [ ] `app/shopify-application`: bootstrap, profiles (`local`/`test`/`staging`/`prod`), `spring.threads.virtual.enabled=true`.
- [ ] CI: `mvn verify`, **`ApplicationModules.verify()`** test, ArchUnit ruleset, Testcontainers in CI.
- [ ] One **vertical "hello tenant" slice**: resolve tenant → route to shard → read a trivial row → emit an event → job logs it. Proves the whole spine end to end.

**Phase 0 DoD:** empty app boots on `local`; tenant context → shard routing works with `shard-count=1`; outbox→job round-trips an event; boundary verification + ArchUnit run in CI and can fail a bad PR.

---

### Phase 1 — Identity & Tenancy
*Goal: shops exist, staff can authenticate, every request is tenant-scoped.*

- [ ] `store`: Shop aggregate, settings, domains, plan, locale; `ShopCreated` event.
- [ ] `identity`: StaffUser, Session, ApiToken, Role/RBAC; login + token issuance.
- [ ] Tenant resolution: host/domain → shop for storefront; auth → shop for admin.
- [ ] Shard assignment on shop creation (shop → shard mapping table on a control plane).
- [ ] Admin auth wired through `platform-security`; `TenantContext` populated per request.

**Phase 1 DoD:** can create a shop, authenticate a staff user, and every downstream call has a guaranteed `shop_id` routed to the correct shard.

---

### Phase 2 — Catalog & Inventory
*Goal: merchants can model what they sell and how much is in stock.*

- [ ] `catalog`: Product, Variant, Option, Collection; publish/unpublish; `ProductPublished`/`VariantUpdated`.
- [ ] `inventory`: Location, StockLevel, Reservation; adjustments; `StockAdjusted`/`ReservationExpired`.
- [ ] Catalog read cache (published products) with TTL + invalidation on `ProductPublished`.
- [ ] `catalog` ↔ `inventory` integration via events only (catalog doesn't write stock).

**Phase 2 DoD:** products and variants are CRUD-able and publishable; stock levels track per location; published-product reads are cached and correctly invalidated.

---

### Phase 3 — Pricing & Cart
*Goal: buyers can assemble a cart and see correct prices/discounts.*

- [ ] `pricing`: PriceList, Discount, Promotion; price resolution; `PriceChanged`/`DiscountApplied`.
- [ ] `cart`: Cart + LineItem, Redis-backed (transient); add/update/remove; `CartCheckedOut`.
- [ ] Cart pulls variant info from `catalog-api` and prices from `pricing-api` (sync reads).
- [ ] Cart never persists authoritative price — it re-resolves at checkout.

**Phase 3 DoD:** a cart computes a correct, discount-aware subtotal by composing `catalog` + `pricing` through their APIs; cart state survives in Redis without hitting a shard for every keystroke.

---

### Phase 4 — Checkout, Orders & Payments
*Goal: the money path. A cart becomes a paid order.*

- [ ] `checkout`: CheckoutSession; **structured-concurrency fan-out** to pricing + inventory + tax for totals; `CheckoutCompleted`.
- [ ] Inventory **reservation** during checkout (released on expiry via job).
- [ ] `orders`: Order, OrderLine, TransactionLedger, Return; `OrderPlaced`/`OrderCancelled`/`OrderRefunded`.
- [ ] `payments`: PaymentIntent, Refund; `payments-spi` gateway port + one adapter; `PaymentCaptured`/`PaymentFailed`.
- [ ] Saga via events: `CheckoutCompleted` → reserve → `PaymentCaptured` → `OrderPlaced` → release/confirm. **No distributed transaction** — each step is a local TX + event, idempotent and compensatable.

**Phase 4 DoD:** a buyer completes checkout, payment is captured through the SPI adapter, an order is created, inventory is decremented, and a failed payment cleanly compensates (reservation released, no orphan order) — all via idempotent event handlers.

---

### Phase 5 — Fulfillment, Shipping & Tax
*Goal: orders can be taxed, rated, and fulfilled.*

- [ ] `tax`: TaxRate, Exemption; calculation feeding checkout totals; `TaxCalculated`.
- [ ] `shipping`: ShippingZone, Rate, Carrier, Fulfillment; rate calc at checkout; `FulfillmentCreated`/`Shipped`.
- [ ] Fulfillment side effects (label, carrier call, customer notify) run as async jobs.

**Phase 5 DoD:** checkout totals include correct tax + shipping; an order can be fulfilled and marked shipped, with carrier/customer side effects handled by `job-engine`.

---

### Phase 6 — Customers, Notifications & Search
*Goal: buyer accounts, communications, and discovery.*

- [ ] `customers`: Customer, Address, Segment, Consent; `CustomerRegistered`/`AddressChanged`.
- [ ] `notifications`: Template, WebhookSubscription, DeliveryLog; email/SMS + merchant webhooks; all async.
- [ ] `search`: ProductIndex/OrderIndex as **event-driven projections** (Postgres FTS first; OpenSearch later if volume demands).
- [ ] `search` consumes `ProductPublished`/`OrderPlaced` etc. — it owns no source-of-truth data.

**Phase 6 DoD:** order/customer lifecycle events trigger emails + webhooks reliably; product search returns published products and stays in sync via projections; `search` is a pure consumer.

---

### Phase 7 — Storefront & Admin APIs
*Goal: the two public surfaces, composed from module APIs.*

- [ ] Storefront API (read-optimized, replica-targeted, cache-heavy): browse, cart, checkout.
- [ ] Admin API (write-capable, RBAC-gated): manage catalog, inventory, orders, settings.
- [ ] API versioning via `platform-web`; consistent `ProblemDetail` errors; rate limiting per shop.
- [ ] Surfaces only call module `*-api`s — no controller reaches into persistence.

**Phase 7 DoD:** a storefront client can browse → cart → checkout end to end against replicas+cache; an admin client can run a shop; both go exclusively through `*-api`.

---

### Phase 8 — Scale & Hardening
*Goal: prove the "Scale Data First" design under load before it's needed.*

- [ ] Run with **`shard-count > 1`** in staging; verify routing, no cross-shard leakage.
- [ ] Read replicas wired for storefront/report paths; read-your-writes paths excluded.
- [ ] Redis cache hit-rate targets + invalidation correctness under churn.
- [ ] `job-engine` hardened: backpressure, DLQ alerting, poison-message handling, queue-depth metrics.
- [ ] Load test the money path (checkout→payment→order) at target concurrency on virtual threads.
- [ ] Observability dashboards per module; SLOs for checkout latency and job lag.

**Phase 8 DoD:** the system runs correctly across multiple shards with replicas and caching, sustains target load, and every module is independently observable.

---

> **Delivery gate from here on:** the frontend is built **last**, and the order is strict —
> **Phase 9 (deploy)** → **Phase 10 (API docs frozen)** → **Phase 11 (Vue 3 frontend)**.
> No frontend code is written until the backend is fully developed, deployed, and its contract is published and frozen.

---

### Phase 9 — Infrastructure, DevOps & Production Deployment
*Goal: the backend runs in production — reproducibly, observably, across the sharded data tier. This is the milestone that unlocks frontend work.*
*Prerequisite: Phases 0–8 Done.*

- [ ] **IaC** for the full topology (CDN, load balancer, app replicas, sharded Postgres primaries + read replicas, Redis, object storage) — version-controlled (e.g. Terraform).
- [ ] **Container image**: multi-stage build on a JDK 25 runtime; one image for the single deployable; virtual threads enabled.
- [ ] **Orchestration**: containers behind the LB with rolling, zero-downtime deploys (e.g. Kubernetes + Helm + GitOps/ArgoCD, or a simpler container host — monolith-first doesn't require a service mesh).
- [ ] **CI/CD pipeline**: commit → `mvn verify` (incl. boundary + ArchUnit) → image build → **staging** → smoke → **prod**, with a manual gate before prod.
- [ ] **Migrations on deploy**: Flyway runs forward-only, **shard-aware across all shards**, as a gated pipeline step.
- [ ] **Config & secrets** per environment via a secrets manager; nothing sensitive in images or VCS.
- [ ] **Observability stack deployed**: metrics/traces/logs backends, per-module dashboards, alerts (checkout-latency SLO, job lag, DLQ size, shard/replica health).
- [ ] **Resilience**: readiness gated on shard reachability; replica/HPA autoscaling; graceful shutdown drains in-flight requests + job workers.
- [ ] **Backups & DR**: automated backups + PITR **per shard**; a restore drill executed; runbooks for add-shard, replica failover, DLQ drain.
- [ ] **Production smoke/canary** of the money path (checkout → payment → order) after deploy.

**Phase 9 DoD:** the backend is live in production across N shards with replicas, Redis, CDN, and job workers; CI/CD promotes commit → staging → prod with shard-wide migrations; observability + alerting are operational; a backup restore has been verified. **Backend, infrastructure, and DevOps are now "fully developed and deployed" — the gate to begin API docs is open.**

---

### Phase 10 — API Documentation & Contract Freeze
*Goal: a complete, accurate, versioned API contract the frontend can build against without reading backend code. Generated from the deployed app so it reflects reality.*
*Prerequisite: Phase 9 Done (backend deployed).*

- [ ] **OpenAPI 3.1** generated from the running app (springdoc) for **both** surfaces: Storefront API and Admin API.
- [ ] **Auth documented**: staff/admin auth, storefront session/token, API tokens, refresh, and RBAC scopes per endpoint.
- [ ] **Cross-cutting documented**: `ProblemDetail` error model, pagination, rate-limit headers, **idempotency keys**, and the **API versioning** scheme.
- [ ] **Per-resource**: request/response schemas (the record DTOs), examples, and exhaustive status codes.
- [ ] **Webhook catalog**: every merchant-facing event payload from `notifications` (name, schema, delivery + retry semantics).
- [ ] **Published portal** (Swagger UI / Redoc / Scalar) reachable in staging, kept in sync with each deploy.
- [ ] **Contract tests** so the spec cannot drift from the implementation (spec ↔ controller verification in CI).
- [ ] **Typed client generated** from the spec (e.g. openapi-typescript / orval) for the frontend to consume.
- [ ] **v1 baseline frozen**: this is the contract the frontend targets. Breaking changes after freeze require a **version bump**, not an edit.

**Phase 10 DoD:** complete, accurate OpenAPI 3.1 for Storefront + Admin is published from the deployed app, with auth/errors/pagination/webhooks covered; contract tests are green; a **frozen v1** baseline and a **generated typed client** are ready. **Frontend development may now begin.**

---

### Phase 11 — Vue.js 3 Frontend
*Goal: the buyer storefront and merchant admin UIs, built against the frozen v1 contract via the typed client.*
*Prerequisite: Phase 10 Done (contract frozen + typed client published).*

- [ ] **Project setup**: Vue 3 + `<script setup>` Composition API + **TypeScript**, Vite build, Pinia (state), Vue Router, Vue I18n (shop locales).
- [ ] **API layer**: integrate the **generated typed client**; wrap server-state with caching (e.g. TanStack Query for Vue) — no hand-rolled fetch shapes.
- [ ] **Auth integration**: token/session handling + refresh; **RBAC-driven UI** (hide/disable by scope) for admin.
- [ ] **Two apps**:
  - [ ] **Storefront (buyer)** — browse catalog → cart → checkout; read-heavy, hits the storefront API (replica/cache-backed). SEO/SSR matters → **Nuxt 3 recommended** for this surface.
  - [ ] **Admin (merchant)** — catalog, inventory, orders, settings; RBAC-gated SPA.
- [ ] **Tenant/i18n**: shop context drives storefront theming + locale.
- [ ] **Error UX**: mapped from the documented `ProblemDetail`; respect rate-limit/idempotency semantics; optimistic updates only where safe.
- [ ] **E2E tests** (Playwright/Cypress) against staging for the core buyer + merchant flows.
- [ ] **Build & deploy**: static assets / SSR host shipped via the **CDN** in the Phase 9 topology; frontend CI/CD wired.

**Phase 11 DoD:** storefront and admin apps run against the deployed backend exclusively through the typed client; core buyer (browse → cart → checkout) and merchant (catalog → inventory → orders) flows work end to end with auth, i18n, and RBAC; E2E suites are green; assets serve via CDN.

---

### Phase 12 — Extraction Readiness (Conditional)
*Do NOT start unless a real trigger fires. Premature extraction violates the whole design.*

**Extraction trigger (need at least one, backed by metrics):**
- [ ] A module must scale independently of the rest (e.g. `search` CPU dominates).
- [ ] A module needs an independent release cadence or dedicated team.
- [ ] A module's data wants different locality/store than its shard.

**If (and only if) triggered:**
- [ ] Confirm consumers depend only on the module's `*-api` (they already should).
- [ ] Switch event publication from in-process to **externalized** (Modulith externalization → Kafka).
- [ ] Replace the in-process `*-api` bean with an HTTP/gRPC client behind the **same interface**.
- [ ] Carve the module's schema onto its own datastore; backfill + dual-write cutover.
- [ ] Stand up the new deployable; route traffic; decommission the in-process path.

**Phase 9 DoD:** exactly one module runs as a separate service, consumers are unchanged at the source level (same `*-api`), and the monolith still owns everything that lacked a trigger.

---

## 5. Decision Log (ADRs)

Record every non-trivial choice as a short ADR under `docs/adr/NNNN-title.md` (context → decision → consequences). Examples worth an ADR: choosing the sharding key, the job-queue backend, when a module first needed an `spi`, any deviation from this roadmap. **An ADR is how we change the rules on purpose instead of by drift.**

---

*The Gate (§1) is not paperwork — it's the thing that keeps a fast monolith from rotting into a distributed ball of mud. Run it every turn.*

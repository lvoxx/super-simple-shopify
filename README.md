# Shopify Clone — Modular Monolith

A multi-tenant commerce platform built as a **modular monolith** on **Java 25** and **Spring Boot 4.x**.
One deployable artifact, hard internal boundaries, and a data layer designed to shard from day one — so the system stays cheap and fast to build now, and can shed modules into services later *without a rewrite*.

> Read this README to understand **how the system is shaped and why**.
> Before writing any code, read [`ROADMAP.md`](./ROADMAP.md) — it is the per-change checklist that keeps us inside these boundaries.

---

## 1. TL;DR

- **One process, many modules.** Each bounded context is a Maven module with a public `api`/`spi` surface and a sealed `impl`. Modules never reach into each other's internals.
- **Monolith first, services later.** We deliberately avoid microservices until a module *earns* extraction (independent scaling, independent release cadence, or team ownership). The boundaries make extraction a deployment change, not a redesign.
- **The database is the bottleneck, not the app.** We design for **sharding by `shop_id`** and read replicas from the start, even while running a single Postgres instance locally.
- **Requests stay thin.** Anything that can be deferred (email, webhooks, indexing, fulfillment side effects) goes through the **transactional outbox → background job engine**. HTTP threads do the minimum and return.
- **Boundaries are enforced by the build,** not by good intentions: Maven module visibility + Spring Modulith verification + ArchUnit tests fail CI when a boundary is crossed.

---

## 2. Architectural Principles

These five principles are not slogans — each one maps to a concrete, enforced practice in this repo.

| Principle | What it means here | How it's enforced |
|---|---|---|
| **Monolith First** | One deployable. No network hop, no service mesh, no distributed transaction until proven necessary. | A single `app/` bootstrap module assembles everything. No module ships its own deployable. |
| **Modular Design** | Bounded contexts with explicit `api`/`spi`/`impl` separation. Cross-module calls go through published interfaces or events only. | Maven module graph + Spring Modulith `verify()` + ArchUnit rules in CI. |
| **Scale Data First** | Shard key (`shop_id`) is baked into every aggregate from the first migration. No cross-shard joins on the transactional path. | Sharding-aware datasource routing in `platform-persistence`; lint check for `shop_id` on tenant tables. |
| **Async Processing** | Side effects are events, not inline calls. Requests commit their own state + an outbox row, then return. | Transactional outbox in `platform-events`; `job-engine` drains it. Synchronous fan-out only via structured concurrency where latency demands it. |
| **Incremental Extraction** | We never build a service speculatively. A module is extracted only when it hits an extraction trigger (see §16). | Extraction checklist gated on real metrics, not architecture taste. |

---

## 3. Reference Architecture

```
                ┌─────────────┐
                │     CDN     │  static assets, storefront media (S3/CloudFront-style)
                └─────┬───────┘
                      │
                ┌─────▼────────┐
                │ Load Balancer│  TLS termination, health checks, sticky-free
                └─────┬────────┘
                      │
             ┌────────▼─────────┐
             │   Monolith App   │  Spring Boot 4.x, virtual-thread request model
             │  (Modular Design)│  modules wired in-process via published APIs + events
             └────────┬─────────┘
                      │  shard router resolves shop_id → shard datasource
      ┌───────────────┼───────────────┐
      │               │               │
┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐
│ Shard DB 1│   │ Shard DB 2│   │ Shard DB N│   Postgres, one shop lives entirely on one shard
│ (+replica)│   │ (+replica)│   │ (+replica)│   read replicas for read-heavy paths
└───────────┘   └───────────┘   └───────────┘

        ┌──────────────────────────────┐
        │     Background Job System    │  outbox → queue → workers (idempotent)
        │  (workers + queue + retries) │  email, webhooks, indexing, fulfillment side effects
        └──────────────────────────────┘

        ┌──────────────────────────────┐
        │   Redis   │  cache + cart + rate-limit + (initial) job queue
        └──────────────────────────────┘
```

**Key property:** every transactional write path touches exactly **one shard**. A shop's products, orders, inventory, and customers all live on the same shard, so there are no distributed transactions and no cross-shard joins in the hot path. Cross-shard work (platform-wide analytics, search indexing) is async and read-only.

---

## 4. Technology Stack

| Concern | Choice | Why |
|---|---|---|
| Language | **Java 25 (LTS)** | Virtual threads, records, sealed types, pattern matching, **scoped values** for context propagation. |
| Framework | **Spring Boot 4.x** (Spring Framework 7) | First-class Java 25 support; fully modularized starter jars align with our module-per-context model. |
| Modularity | **Spring Modulith** | Runtime boundary verification, module events, documentation generation, externalization hooks for future extraction. |
| Build | **Maven (multi-module reactor)** | Compile-time boundary enforcement via the module graph + an internal BOM. |
| Persistence | **PostgreSQL** + **MyBatis (MyBatis-Spring)** | Explicit, reviewable SQL and full control over shard-aware queries — no ORM lazy-loading or hidden N+1 surprises. JPA is intentionally **not** used. |
| Mapping | **ModelMapper** | DTO ↔ domain mapping, kept out of controllers and persistence. |
| Validation & i18n | **Jakarta Bean Validation + Spring `MessageSource`** | Inputs validated at the edge; all error/exception text resolved through i18n message resolvers. |
| Errors | **Centralized `@RestControllerAdvice` → `ProblemDetail`** | One exception handler; responses never carry raw error codes. |
| Migrations | **Flyway/Liquibase, run from infra** | Versioned, per-module scripts, **executed by a migration container — never by a Spring service**. |
| Scaffolding | **Spring Boot CLI `spring init` (via SDKMAN)** | Every module/service generated from the CLI; no hand-rolled skeletons. |
| Cache / Cart / Queue | **Redis** | Hot reads, cart state, rate limiting, and the initial job queue (graduates to Kafka on extraction). |
| Async / Events | **Outbox + Spring Modulith events** → `job-engine` | Reliable, in-process now; externalizable later without changing publishers. |
| Search | **OpenSearch / Postgres FTS (Phase 6)** | Start with Postgres FTS, graduate to OpenSearch when catalog/order volume demands. |
| Observability | **Micrometer + OpenTelemetry + structured JSON logs** | Per-module metrics and traces; module name is a first-class tag. |
| Tests | **JUnit 5, Testcontainers, ArchUnit, Spring Modulith test** | Module-slice tests + boundary tests + integration against real Postgres/Redis. |
| API docs | **OpenAPI 3.1 (springdoc) + generated typed client** | Contract is generated from the deployed app and **frozen before any frontend work**. |
| Frontend | **Vue 3 (`<script setup>`, TS) + Vite + Pinia + Vue Router + Vue I18n** | Buyer storefront + merchant admin; built **last**, against the frozen contract. Nuxt 3 recommended for the SSR/SEO storefront. |

### Why virtual threads matter here
With `spring.threads.virtual.enabled=true`, blocking JDBC/Redis calls no longer pin platform threads. We get high request concurrency with **plain, readable blocking code** — no reactive complexity unless a specific path needs it. Request-scoped context (tenant, principal, request id) is propagated with **scoped values**, not `ThreadLocal`, which is the correct primitive under virtual threads.

---

## 5. Repository Layout

A single Maven reactor. `platform-*` modules are the "pre-implemented starters" every domain module depends on; domain modules never depend on each other's `impl`.

```
shopify-clone/
├── pom.xml                         # reactor parent: <modules>, pluginManagement, enforcer
│
├── platform/                       # shared starters — the only thing domain modules may depend on
│   ├── platform-bom/               # internal BOM: pins every internal + third-party version
│   ├── platform-core/              # Money, Result<T>, typed Ids, Clock, base DomainEvent (sealed)
│   ├── platform-web/               # central exception handler → ProblemDetail, i18n MessageSource, Bean Validation, ModelMapper, API versioning, rate-limit, pagination
│   ├── platform-persistence/       # shard router, MyBatis-Spring config (SqlSessionFactory per shard), base mapper support, auditing — no migration runner
│   ├── platform-events/            # event publisher, transactional outbox, externalization SPI
│   ├── platform-security/          # auth filter, principal, TenantContext (scoped value), RBAC
│   ├── platform-observability/     # tracing, metrics, structured logging, module tagging
│   ├── platform-jobs/              # job engine API: @Job, JobScheduler, retry/backoff contracts
│   └── platform-test/              # Testcontainers fixtures, module test slices, fakes
│
├── modules/                        # bounded contexts (see §15 for the full catalog)
│   ├── identity/
│   │   ├── identity-api/            # interfaces, DTOs, published events — others depend on THIS
│   │   ├── identity-spi/            # ports this module needs others to implement (optional)
│   │   └── identity-impl/           # domain model, services, persistence — internal, sealed
│   ├── store/        { store-api,        store-impl }
│   ├── catalog/      { catalog-api,      catalog-impl }
│   ├── inventory/    { inventory-api,    inventory-impl }
│   ├── pricing/      { pricing-api,      pricing-impl }
│   ├── cart/         { cart-api,         cart-impl }
│   ├── checkout/     { checkout-api,     checkout-impl }
│   ├── orders/       { orders-api,       orders-impl }
│   ├── payments/     { payments-api,     payments-spi, payments-impl }
│   ├── customers/    { customers-api,    customers-impl }
│   ├── shipping/     { shipping-api,     shipping-impl }
│   ├── tax/          { tax-api,          tax-impl }
│   ├── notifications/{ notifications-api,notifications-impl }
│   └── search/       { search-api,       search-impl }
│
├── jobs/
│   └── job-engine/                 # worker runtime: drains outbox/queue, runs @Job handlers
│
└── app/
    └── shopify-application/         # the ONLY deployable: depends on every *-impl, owns config
```

**Dependency rule of thumb:**
`*-impl` → may depend on any `*-api`, any `platform-*`, and its own `*-spi`.
`*-impl` → may **never** depend on another module's `*-impl`.
`*-api` → depends only on `platform-core` (keep it light; it's everyone's contract).
`app` → depends on every `*-impl` to assemble the deployable. Nothing depends on `app`.

---

## 6. Module Anatomy

Every domain module follows the same internal shape. Example: `catalog`.

```
catalog-api/            (published — other modules compile against this)
└── com.shop.catalog.api
    ├── ProductCatalog.java         # the port: e.g. find(VariantId), publish/unpublish
    ├── dto/                        # records: ProductView, VariantView
    └── event/                      # sealed events: ProductPublished, VariantPriceChanged

catalog-impl/           (internal — sealed off; nothing outside compiles against it)
└── com.shop.catalog.internal
    ├── domain/                     # Product, Variant, Collection (aggregates), invariants
    ├── application/                # ProductCatalogService implements ...api.ProductCatalog
    ├── persistence/                # MyBatis mappers (@Mapper + XML/result maps), row records, shard-aware SqlSession
    └── web/                        # REST controllers for admin/storefront catalog endpoints
```

Rules:
- The module itself was generated with **`spring init`** — the skeleton is never hand-rolled.
- The `internal` package is the module's private world. **Spring Modulith forbids** other modules from referencing it.
- The implementation of an `api` port is a Spring bean. Other modules **inject the interface**, never the concrete class.
- DTOs crossing a boundary are `record`s in `*-api`. Domain entities and **MyBatis types never leave `impl`**.
- **ModelMapper** handles DTO ↔ domain conversion; controllers and persistence don't hand-map fields.
- Domain events are `sealed interface` hierarchies in `*-api` so consumers get exhaustive `switch`.

---

## 7. How Modules Communicate

There are exactly **two** legal ways for module A to talk to module B. Anything else is a boundary violation.

**1. Synchronous, read/command — via a published API bean.**
Inject `B`'s `*-api` interface. Use this when A needs an immediate answer (e.g. `checkout` asks `pricing` to compute totals). For multi-module fan-out on a latency-critical path (checkout → pricing + inventory + tax), use **structured concurrency** (`StructuredTaskScope`) so the calls run in parallel and fail fast together.

**2. Asynchronous, fire-and-forget — via domain events.**
Publish an event from A; B subscribes. Use this for side effects that must not block or fail the request (e.g. `OrderPlaced` → notifications, search indexing, analytics). Events go through the **transactional outbox**: they commit in the same DB transaction as the state change, then the `job-engine` delivers them. This gives at-least-once delivery; **all handlers must be idempotent**.

> **Decision rule:** if B's failure should roll back A's work → synchronous API call.
> If B's failure should *not* fail A → event. When in doubt, prefer the event and design for idempotency.

---

## 8. Data Architecture (Scale Data First)

The database is the scaling ceiling, so it gets designed first — even though local dev runs a single Postgres.

**Sharding.**
- Shard key is **`shop_id`** on every tenant-scoped table. A shop's entire dataset lives on one shard.
- `platform-persistence` resolves the active shard from `TenantContext` and routes the connection. Application code never picks a datasource.
- **No foreign keys or joins across shards.** Cross-shop/platform reads happen off the transactional path (replicas + async projections).
- Local/dev: one Postgres, `shard-count=1`. Staging/prod: N shards behind the same routing layer. The code path is identical.

**Read/write split.**
- Writes go to the shard primary. Read-heavy, non-read-your-writes paths (storefront catalog browse, reports) target **read replicas**.
- A request is read-only by default; only command handlers open write transactions.

**Caching.**
- Redis caches hot, slowly-changing reads (published products, shop settings, shipping zones) with explicit TTLs and event-driven invalidation (`ProductPublished` evicts the product cache key).
- **Cache is never the source of truth.** A cold cache must always be correct, just slower.

**Migrations.**
- Each module owns its migration scripts under `db/migration/<module>`; no module edits another's schema.
- Scripts are **applied from the infrastructure layer — a dedicated migration container/job — never by the Spring app at startup.** The app assumes the schema already exists.
- Every tenant table includes `shop_id` (NOT NULL) and is indexed on it. CI lints for this.

---

## 9. Async & Background Jobs

```
command handler                outbox (same TX)          job-engine workers
─────────────────              ─────────────────         ──────────────────
save aggregate ──┐
publish event  ──┴── commit ─▶ outbox row persisted ─▶ poll/stream ─▶ run @Job handler
                                                                       (idempotent, retried)
```

- **Outbox guarantees** the event is recorded atomically with the state change. No "saved the order but lost the confirmation email."
- **`job-engine`** drains the outbox and runs `@Job` handlers with retry + exponential backoff + a dead-letter table.
- **Idempotency is mandatory.** Every handler keys on a stable id (event id / order id) so re-delivery is a no-op.
- Start with a **Redis/DB-backed queue** (monolith-first, light infra). The publisher API does not change when this graduates to Kafka during extraction.

Typical async work: order confirmation email, webhook delivery to merchants, search indexing, inventory reservation cleanup, analytics projections, fulfillment side effects.

---

## 10. Multi-Tenancy

- Every request resolves a **shop (tenant)** from the host/domain or admin auth, and stores it in `TenantContext` (a **scoped value**, propagated cleanly across virtual threads and structured-concurrency forks).
- The shard router reads `TenantContext` to pick the datasource. Forgetting to set tenant context is a hard failure, not a silent fallback.
- Background jobs carry their `shop_id` in the job payload and re-establish `TenantContext` before running.
- Isolation is **logical** (row + shard scoping), which is the right tradeoff for a monolith-first SaaS. Physical isolation per tenant is an extraction-era concern, not a v1 concern.

---

## 11. Boundary Enforcement

Three layers, all in CI. A boundary violation **fails the build**.

1. **Maven module graph** — `*-impl` simply cannot see another `*-impl` on the classpath; it isn't a declared dependency. This is the strongest, earliest signal.
2. **Spring Modulith `ApplicationModules.verify()`** — a test asserts no module references another module's `internal` package and that the dependency graph has no illegal cycles.
3. **ArchUnit rules** — package-level rules (e.g. "controllers may not touch persistence directly", "no MyBatis/persistence types in `*-api`", "events are records/sealed").

---

## 12. Build & Run

**Prerequisites**
- **SDKMAN** → JDK 25 (Temurin/Liberica) + **Spring Boot CLI** (`spring init`, for scaffolding new modules)
- Maven 3.9+
- Docker (Testcontainers + official Postgres/Redis images)

**Common commands**
```bash
# Full reactor build + boundary checks + tests
./mvnw clean verify

# Run the app (local profile: single shard, Dockerized Postgres + Redis)
./mvnw -pl app/shopify-application spring-boot:run -Dspring-boot.run.profiles=local

# Build just one module and what it needs
./mvnw -pl modules/catalog/catalog-impl -am verify

# Generate module documentation (Spring Modulith C4 + PlantUML)
./mvnw -pl app/shopify-application test -Dtest=ModularityDocumentationTests
```

**Profiles**
| Profile | Shards | Datastores | Use |
|---|---|---|---|
| `local` | 1 | Dockerized PG + Redis | day-to-day dev |
| `test` | 1 | Testcontainers | CI |
| `staging` | N | managed PG + replicas | pre-prod |
| `prod` | N | sharded PG + replicas + Redis cluster | production |

---

## 13. Observability

- **Tracing:** OpenTelemetry; spans tagged with `module`, `shop_id`, `request_id`. Async handoffs propagate trace context through the outbox payload.
- **Metrics:** Micrometer; per-module timers and counters. Job engine exposes queue depth, retry rate, DLQ size.
- **Logging:** structured JSON; `module`, `shop_id`, `request_id`, `job_id` as fields. No `shop_id` in cache keys or logs without it.
- **Health:** liveness/readiness probes; readiness fails if the active shard set is unreachable.

---

## 14. Testing Strategy

| Layer | Tool | Scope |
|---|---|---|
| Unit | JUnit 5 | domain invariants, pure logic, no Spring |
| Module slice | Spring Modulith `@ApplicationModuleTest` | one module bootstrapped in isolation; collaborators stubbed via `*-api` |
| Integration | Testcontainers (PG + Redis) | persistence, sharding routing, outbox delivery |
| Boundary | Spring Modulith `verify()` + ArchUnit | no illegal cross-module access, no cycles |
| Contract | event/DTO schema tests | `*-api` changes are caught before consumers break |

**Rule:** a module's tests must pass with **only that module + platform** on the classpath. If a test needs another module's `impl`, the boundary is wrong.

---

## 15. Module Catalog

| Module | Bounded context | Owns (aggregate roots) | Key published events |
|---|---|---|---|
| `identity` | Auth & staff accounts | StaffUser, Session, ApiToken, Role | `StaffInvited`, `SessionRevoked` |
| `store` | Shop / tenant | Shop, Domain, Settings, Plan, Locale | `ShopCreated`, `PlanChanged` |
| `catalog` | Products | Product, Variant, Collection, Option | `ProductPublished`, `VariantUpdated` |
| `inventory` | Stock | Location, StockLevel, Reservation | `StockAdjusted`, `ReservationExpired` |
| `pricing` | Money & promotions | PriceList, Discount, Promotion | `DiscountApplied`, `PriceChanged` |
| `cart` | Buyer cart | Cart, LineItem (transient, Redis-backed) | `CartCheckedOut` |
| `checkout` | Checkout flow | CheckoutSession, totals calc | `CheckoutCompleted` |
| `orders` | Order lifecycle | Order, OrderLine, TransactionLedger, Return | `OrderPlaced`, `OrderCancelled`, `OrderRefunded` |
| `payments` | Payments | PaymentIntent, Refund, GatewayAdapter (SPI) | `PaymentCaptured`, `PaymentFailed` |
| `customers` | Buyers | Customer, Address, Segment, Consent | `CustomerRegistered`, `AddressChanged` |
| `shipping` | Delivery | ShippingZone, Rate, Carrier, Fulfillment | `FulfillmentCreated`, `Shipped` |
| `tax` | Tax | TaxRate, Exemption, calculation | `TaxCalculated` |
| `notifications` | Comms | Template, WebhookSubscription, DeliveryLog | `NotificationSent`, `WebhookDelivered` |
| `search` | Discovery | ProductIndex, OrderIndex (projections) | — (consumer of others' events) |

`payments` intentionally exposes an **SPI** (`payments-spi`) so gateway adapters (Stripe-like, local VN gateways) plug in without `payments-impl` depending on them.

---

## 16. The Extraction Path

A module becomes a separate service **only** when it hits a real trigger — never on architectural taste.

**Triggers (any one):**
- It needs to scale independently (e.g. `search` is CPU-bound and dwarfs everything else).
- It needs an independent release cadence or a dedicated team.
- Its data genuinely wants a different store/locality than the shard.

**Why extraction is cheap here:**
- Consumers already depend only on `*-api`, never on internals.
- Communication is already events or interface calls — swap the in-process publisher for an externalized one (Spring Modulith externalization → Kafka) and the in-process bean call for an HTTP/gRPC client behind the same `*-api` interface.
- The module already owns its schema and migrations.

**Extraction checklist** lives in `ROADMAP.md` Phase 12. Do not start it without the metrics that justify it.

---

## 17. Common Pitfalls → Hard Rules

The pitfalls below are turned into rules the build (or review) enforces.

| Pitfall | Our rule |
|---|---|
| Starting with microservices too early | One deployable until a §16 extraction trigger fires. PRs that add a second deployable are rejected. |
| Lack of domain boundaries | Every context is an `api`/`impl` module pair, verified by Modulith + ArchUnit. |
| Heavy synchronous processing | Side effects are events through the outbox. Sync cross-module calls need a "must roll back together" justification. |
| No caching strategy | Hot reads are cached with explicit TTL + event-driven invalidation. Cache is never source of truth. |
| Tight coupling between modules | Managed by the **reactor pom + internal BOM**: `*-impl` cannot declare another `*-impl` as a dependency. Shared code lives in `platform-*`, never copied between modules. |

---

## 18. Glossary

- **Bounded context / module** — a self-contained domain area owning its model, schema, and API.
- **Shard** — a Postgres instance holding the full dataset for a subset of shops.
- **Outbox** — a table written in the same transaction as a state change, later drained to deliver events reliably.
- **`*-api` / `*-spi` / `*-impl`** — published contract / ports-we-need-filled / private implementation.
- **`platform-*`** — shared starter modules; the only cross-cutting dependency a domain module may have.
- **TenantContext** — the active `shop_id`, carried as a scoped value and used by the shard router.

---

## 19. Frontend & Delivery Sequencing

The frontend is built **last**, and the order is deliberate and strict:

```
Backend (Phases 0–8)  →  Infra + DevOps + Deploy (Phase 9)  →  API Docs frozen (Phase 10)  →  Vue 3 frontend (Phase 11)
```

- **No frontend code is written until the backend is fully developed, deployed, and stable.** The UI builds against a *running* system, not a moving target.
- **The contract is frozen before the UI starts.** Phase 10 generates an **OpenAPI 3.1** spec from the deployed app (Storefront + Admin surfaces), documents auth / errors (`ProblemDetail`) / pagination / rate-limits / idempotency / webhooks, and produces a **generated typed client**. The frontend consumes *only* this frozen v1 contract — never undocumented endpoints, never backend internals.
- **Two Vue 3 apps:**
  - **Storefront (buyer)** — browse → cart → checkout. Read-heavy, replica/cache-backed, SEO-sensitive → **Nuxt 3** is the recommended host for this surface.
  - **Admin (merchant)** — catalog, inventory, orders, settings. RBAC-gated SPA.
- **Stack:** Vue 3 Composition API (`<script setup>`, TypeScript), Vite, Pinia, Vue Router, Vue I18n (shop locales), the generated typed client + a server-state cache (e.g. TanStack Query for Vue), and Playwright/Cypress for E2E.
- Frontend assets / SSR ship through the **same CDN** in the Phase 9 topology.

Full per-phase checklists for deployment, documentation, and the frontend are in [`ROADMAP.md`](./ROADMAP.md) Phases 9–11.

---

## 20. Engineering Conventions (non-negotiable)

These exist to keep the codebase boring and uniform — the opposite of "coding hell." They are enforced in review and, where possible, in CI.

- **Scaffolding — always `spring init`.** Every Spring module/service is generated with the **Spring Boot CLI** (installed via **SDKMAN**), so parent POM, BOM, and plugins are correct and identical everywhere. Hand-rolled module skeletons are rejected.
- **Data access — MyBatis, not JPA.** Queries are explicit MyBatis mappers (`@Mapper` + XML/result maps). This keeps SQL reviewable and shard-aware, with no ORM lazy-loading or hidden N+1s. JPA is intentionally absent.
- **Migrations — from infrastructure, not the app.** Schema is applied by a dedicated **migration container/job** (Flyway/Liquibase) in the infra layer, shard-aware. **No Spring service runs migrations**, at startup or otherwise; the app assumes the schema exists.
- **Mapping — ModelMapper.** DTO ↔ domain conversion goes through ModelMapper, not field copying scattered across controllers and persistence.
- **Validation + i18n + errors — centralized.** Inputs use **Jakarta Bean Validation**; every exception is handled by **one** `@RestControllerAdvice` returning `ProblemDetail`, with messages resolved through an i18n **`MessageSource`**. **Responses never expose raw error codes** — clients get a resolved, localized problem document.
- **Containers & charts — reuse, don't reinvent.** Use **official/community** Docker images, Compose files, and Helm charts (e.g. Bitnami) rather than hand-authoring them. The Spring app's `Dockerfile` uses **extracted layered jars** (`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`) so unchanged dependency layers stay cached between builds.

---

*This README describes the target shape. Implementation proceeds phase by phase per [`ROADMAP.md`](./ROADMAP.md). When reality and this document disagree, fix the code or update this document in the same PR — never let them drift.*

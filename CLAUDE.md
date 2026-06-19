# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

A multi-tenant Shopify-like commerce platform built as a **modular monolith** on Java 25 + Spring Boot 4.x. One deployable artifact, hard module boundaries, and a data layer sharded by `shop_id` from day one.

Before writing any code, run the pre-implementation gate in `ROADMAP.md §1`. It is mandatory for every change.

---

## Build & Run Commands

```bash
# Full reactor build + boundary checks + all tests
./mvnw clean verify

# Run the app locally (single shard, Dockerized Postgres + Redis)
./mvnw -pl app/shopify-application spring-boot:run -Dspring-boot.run.profiles=local

# Build one module and its transitive dependencies
./mvnw -pl modules/catalog/catalog-impl -am verify

# Run a single test class
./mvnw -pl modules/catalog/catalog-impl -am test -Dtest=ProductCatalogServiceTest

# Generate Spring Modulith C4/PlantUML module docs
./mvnw -pl app/shopify-application test -Dtest=ModularityDocumentationTests
```

**Prerequisites:** SDKMAN → JDK 25 (Temurin/Liberica) + Spring Boot CLI, Maven 3.9+, Docker (for Testcontainers + local Postgres/Redis).

---

## Module Layout

```
platform/       # shared starters — the ONLY cross-cutting dependencies domain modules may use
  platform-bom/         # pins all internal + third-party versions (internal BOM)
  platform-core/        # Money, Result<T>, typed Ids, Clock, sealed DomainEvent base
  platform-web/         # central @RestControllerAdvice → ProblemDetail, i18n, Bean Validation, ModelMapper, pagination, rate-limit
  platform-persistence/ # shard router, MyBatis-Spring config, base mappers, auditing
  platform-events/      # outbox, event publisher, externalization SPI
  platform-security/    # auth filter, Principal, TenantContext (scoped value), RBAC
  platform-observability/
  platform-jobs/        # @Job, JobScheduler, retry/backoff contracts
  platform-test/        # Testcontainers fixtures, module test slices, fakes

modules/        # bounded contexts; each has *-api / *-impl (and *-spi if needed)
  identity/ catalog/ inventory/ pricing/ cart/ checkout/
  orders/ payments/ shipping/ tax/ customers/ notifications/ search/ store/

jobs/
  job-engine/   # worker runtime: drains outbox, runs @Job handlers idempotently

app/
  shopify-application/  # the ONLY deployable; assembles every *-impl; owns config
```

Each domain module follows `{name}-api` / `{name}-impl` (and optionally `{name}-spi`). `payments` is the one module with a `-spi` for gateway adapters.

---

## Architecture Rules (enforced by Maven + Modulith + ArchUnit in CI)

**Dependency direction:**
- `*-api` → depends only on `platform-core`
- `*-impl` → may depend on any `*-api`, any `platform-*`, and its own `*-spi`
- `*-impl` → may **never** depend on another module's `*-impl`
- `app` → depends on every `*-impl` to assemble the deployable; nothing depends on `app`

**Cross-module communication — exactly two legal channels:**
1. **Synchronous** (immediate answer + must roll back together): inject the `*-api` interface directly. Use `StructuredTaskScope` for multi-module fan-out on latency-critical paths (e.g. checkout → pricing + inventory + tax in parallel).
2. **Asynchronous** (side effect that must not fail/block the request): publish a domain event through the **transactional outbox**. The `job-engine` delivers it; all handlers must be **idempotent**.

**Data model rules:**
- Every tenant table has `shop_id NOT NULL` + an index on it.
- No cross-shard joins or FKs on the transactional path.
- `platform-persistence` resolves the active shard from `TenantContext` automatically. App code never picks a datasource.
- Read-heavy paths target replicas; only command handlers open write transactions.

---

## Technology Choices (non-negotiable)

| Area | Choice | Note |
|---|---|---|
| Data access | **MyBatis** (`@Mapper` + XML) | JPA is intentionally absent — never introduce it |
| DTO↔domain mapping | **ModelMapper** | Not field-by-field copying in controllers or persistence |
| Validation | **Jakarta Bean Validation** | Inputs validated at the edge |
| Error handling | **One `@RestControllerAdvice` → `ProblemDetail`** | i18n `MessageSource` resolves all messages; no raw error codes in responses |
| Migrations | **Flyway/Liquibase in the infra/migration container** | The Spring app **never** runs migrations at startup; it assumes schema exists |
| Scaffolding | **`spring init` (Spring Boot CLI)** | Every new Spring module generated this way; hand-rolled skeletons are rejected |
| Docker/Helm | **Official/community images** | Don't hand-author Compose files or Helm chart templates when official ones exist |
| Spring image | **Extracted layered jars** | `Dockerfile` layers: `dependencies` / `spring-boot-loader` / `snapshot-dependencies` / `application` |

---

## Testing

| Layer | Tool | Key rule |
|---|---|---|
| Unit | JUnit 5 | Domain invariants, pure logic, no Spring context |
| Module slice | `@ApplicationModuleTest` (Spring Modulith) | One module in isolation; collaborators stubbed via `*-api` |
| Integration | Testcontainers (PG + Redis) | Persistence, shard routing, outbox delivery |
| Boundary | `ApplicationModules.verify()` + ArchUnit | Must stay green; a violation fails CI |

A module's tests must pass with **only that module + `platform-*`** on the classpath. If a test needs another module's `impl`, the boundary is wrong.

---

## Hard Stops — Ask Before Proceeding

Stop and get sign-off before:
- Adding a second deployable or splitting the monolith
- Introducing a cross-shard join/FK on a transactional path
- Putting a persistence type (MyBatis row / `@Mapper`) in a `*-api` module
- Making a module depend on another module's `*-impl`
- Adding a side effect inline in a request handler instead of via an event
- Adding a third-party dependency not pinned in `platform-bom`
- Hand-creating a module skeleton instead of using `spring init`
- Running a migration from the Spring app at startup
- Returning a raw error code instead of routing through the centralized exception handler

See `ROADMAP.md §3` for the full list.

---

## Delivery Sequencing

Backend (Phases 0–8) → Infra/DevOps/Deploy (Phase 9) → API docs frozen (Phase 10) → Vue 3 frontend (Phase 11).

**No frontend code is written until the backend is fully deployed and the OpenAPI v1 contract is frozen.** The frontend calls only endpoints in that frozen spec, using the generated typed client. See `ROADMAP.md §4` for per-phase checklists.

Frontend stack (Phase 11 only): Vue 3 Composition API (`<script setup>`, TypeScript), Vite, Pinia, Vue Router, Vue I18n, Nuxt 3 for the storefront (SSR/SEO), Playwright/Cypress for E2E.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **super-simple-shopify** (70 symbols, 69 relationships, 0 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/super-simple-shopify/context` | Codebase overview, check index freshness |
| `gitnexus://repo/super-simple-shopify/clusters` | All functional areas |
| `gitnexus://repo/super-simple-shopify/processes` | All execution flows |
| `gitnexus://repo/super-simple-shopify/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

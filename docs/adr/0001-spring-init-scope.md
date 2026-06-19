# ADR 0001 — `spring init` applies to bootable apps; library modules are plain reactor modules

- Status: Accepted
- Date: 2026-06-19

## Context

`ROADMAP.md §1a` and `README.md §20` mandate that **every Spring module/service is
generated with `spring init`** and that hand-rolled skeletons are rejected. Taken
literally this is impossible for the `*-api`, `*-spi`, and most `platform-*` modules:
`spring init` only emits a **bootable Spring Boot application** (one `@SpringBootApplication`,
a `spring-boot-maven-plugin` repackage goal, a `mvnw` wrapper). An `*-api` module whose
only dependency is `platform-core` is a plain library `jar`, not an application — there is
nothing for `spring init` to generate that wouldn't have to be torn back down.

## Decision

We read the rule by intent: **`spring init` is the source of truth for the toolchain
baseline** — the Spring Boot 4.x parent/BOM coordinates, the Java version, the Maven
wrapper, and the layered-jar plugin config. Concretely:

1. The **deployable** (`app/shopify-application`) and any **independently bootable runtime**
   are generated with `spring init`. Their POMs and wrappers are never hand-edited beyond
   wiring reactor modules.
2. The Maven **wrapper** (`mvnw`, `.mvn/`) used by the whole reactor is the one `spring init`
   produced — not a hand-written one.
3. `*-api` / `*-spi` / `*-impl` / `platform-*` are **library modules** (`<packaging>jar</packaging>`,
   no `spring-boot-maven-plugin`). They inherit the reactor parent, which in turn imports the
   Spring Boot dependencies BOM via `platform-bom`. They are authored as standard reactor
   modules — this is **not** "hand-rolling a Spring app skeleton", it is declaring a library.

The reactor parent `pom.xml` itself is a build-aggregation POM, not a Spring application, so
it is hand-authored.

## Consequences

- The hard-stop "hand-create a Spring module/service skeleton instead of `spring init`" is
  honoured for everything that actually boots.
- Version drift is impossible: third-party + Spring versions live only in `platform-bom`,
  seeded from the `spring init` baseline (Spring Boot 4.0.7).
- A reviewer checking the gate should verify bootable modules trace back to `spring init`
  output, and library modules carry no `spring-boot-maven-plugin`.

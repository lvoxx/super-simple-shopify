# syntax=docker/dockerfile:1
# Schema is owned by this infra migration container — NEVER by the Spring app at startup.
# It applies every module's forward-only Flyway migrations, shard-aware, as a gated pipeline
# step (wired into CD in Phase 9). Uses the official Flyway image.
FROM flyway/flyway:11-alpine
# Canonical migration sources, one folder per module.
COPY db/migration/ /flyway/sql/
COPY infra/migration/migrate-all-shards.sh /flyway/migrate-all-shards.sh
ENTRYPOINT ["/bin/sh", "/flyway/migrate-all-shards.sh"]

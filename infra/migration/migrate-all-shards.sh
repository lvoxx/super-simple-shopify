#!/bin/sh
# Apply all module migrations to every shard. Phase 0 has shard-count=1; the loop is written
# shard-aware so Phase 8/9 only change SHARD_JDBC_URLS (a comma-separated list, one per shard).
# Forward-only; fails the pipeline step on any error.
set -eu

: "${SHARD_JDBC_URLS:?set SHARD_JDBC_URLS (comma-separated jdbc urls, one per shard)}"
: "${FLYWAY_USER:?set FLYWAY_USER}"
: "${FLYWAY_PASSWORD:?set FLYWAY_PASSWORD}"

IFS=','
for url in $SHARD_JDBC_URLS; do
    echo "==> Migrating shard: ${url}"
    flyway \
        -url="${url}" \
        -user="${FLYWAY_USER}" \
        -password="${FLYWAY_PASSWORD}" \
        -locations="filesystem:/flyway/sql/platform,filesystem:/flyway/sql/hello" \
        -baselineOnMigrate=true \
        migrate
done
echo "All shards migrated."

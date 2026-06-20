#!/bin/sh
# Apply control-plane migrations to the single (non-sharded) control database. Run ONCE per deploy,
# separately from the per-shard tenant migrations (migrate-all-shards.sh): the control plane is global,
# so there is exactly one target. Forward-only; fails the pipeline step on any error.
set -eu

: "${CONTROL_JDBC_URL:?set CONTROL_JDBC_URL (jdbc url of the control database)}"
: "${FLYWAY_USER:?set FLYWAY_USER}"
: "${FLYWAY_PASSWORD:?set FLYWAY_PASSWORD}"

echo "==> Migrating control plane: ${CONTROL_JDBC_URL}"
flyway \
    -url="${CONTROL_JDBC_URL}" \
    -user="${FLYWAY_USER}" \
    -password="${FLYWAY_PASSWORD}" \
    -locations="filesystem:/flyway/sql/control" \
    -baselineOnMigrate=true \
    migrate
echo "Control plane migrated."

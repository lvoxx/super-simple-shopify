package com.shop.catalog;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a product is published (made visible to buyers). Written to the tenant outbox in the same
 * transaction as the status change, then drained idempotently by the job-engine. Downstream consumers
 * react to this — the published-product read cache invalidates, and (later phases) search reindexes —
 * never inline here.
 *
 * <p>Lives in the module's exposed base package (not {@code internal}) so other modules may consume it.
 */
public record ProductPublished(UUID eventId, Instant occurredAt, ShopId shopId, ProductId productId,
		String handle) implements DomainEvent {
}

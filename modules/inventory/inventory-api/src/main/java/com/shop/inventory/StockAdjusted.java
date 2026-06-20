package com.shop.inventory;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a variant's available stock at a location changes. Written to the tenant outbox in the same
 * transaction as the stock-level write, then drained idempotently by the job-engine. Downstream consumers
 * (search availability, low-stock notifications — later phases) react to this; it carries the resulting
 * {@code available} so consumers need not re-read.
 */
public record StockAdjusted(UUID eventId, Instant occurredAt, ShopId shopId, VariantRef variant,
		LocationId location, long delta, long available) implements DomainEvent {
}

package com.shop.catalog;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a variant's sellable attributes change (sku, title, or price). Written to the tenant outbox
 * in the same transaction as the write. Consumers that cached a variant — the published-product cache,
 * and carts that must re-resolve price at checkout — use this to invalidate; the cart never trusts a
 * stale persisted price.
 */
public record VariantUpdated(UUID eventId, Instant occurredAt, ShopId shopId, ProductId productId,
		VariantId variantId) implements DomainEvent {
}

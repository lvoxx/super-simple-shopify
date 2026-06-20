package com.shop.catalog;

import com.shop.platform.core.Identifier;

/**
 * Typed identifier for a product. Allocated by the tenant shard on insert (shard-local sequence), so it
 * is unique within a shard; queries always pair it with the active {@code ShopId} for tenant isolation.
 */
public record ProductId(long value) implements Identifier {

	public ProductId {
		if (value <= 0) {
			throw new IllegalArgumentException("productId must be positive, was " + value);
		}
	}

	public static ProductId of(long value) {
		return new ProductId(value);
	}

	@Override
	public String toString() {
		return "product-" + value;
	}
}

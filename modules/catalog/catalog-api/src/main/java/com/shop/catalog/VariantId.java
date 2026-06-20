package com.shop.catalog;

import com.shop.platform.core.Identifier;

/**
 * Typed identifier for a product variant — the individually-priced, purchasable unit. Allocated by the
 * tenant shard on insert; always scoped by the active {@code ShopId} on reads and writes.
 */
public record VariantId(long value) implements Identifier {

	public VariantId {
		if (value <= 0) {
			throw new IllegalArgumentException("variantId must be positive, was " + value);
		}
	}

	public static VariantId of(long value) {
		return new VariantId(value);
	}

	@Override
	public String toString() {
		return "variant-" + value;
	}
}

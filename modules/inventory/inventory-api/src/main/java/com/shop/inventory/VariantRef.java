package com.shop.inventory;

import com.shop.platform.core.Identifier;

/**
 * A reference to a catalog variant, as seen by inventory. Inventory cannot depend on {@code catalog-api}
 * (an {@code *-api} module depends only on {@code platform-core}), so it carries the variant id as its
 * own typed value rather than {@code catalog.VariantId}. Being typed keeps it from being confused with a
 * {@link LocationId} — both wrap a {@code long} — on the reserve/adjust paths.
 */
public record VariantRef(long value) implements Identifier {

	public VariantRef {
		if (value <= 0) {
			throw new IllegalArgumentException("variantRef must be positive, was " + value);
		}
	}

	public static VariantRef of(long value) {
		return new VariantRef(value);
	}

	@Override
	public String toString() {
		return "variant-" + value;
	}
}

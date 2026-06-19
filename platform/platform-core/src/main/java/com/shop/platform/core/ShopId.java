package com.shop.platform.core;

/**
 * The tenant key. Every tenant table is sharded and indexed by this value, and the active
 * {@code ShopId} is what {@code TenantContext} carries for the duration of a request or job.
 */
public record ShopId(long value) implements Identifier {

	public ShopId {
		if (value <= 0) {
			throw new IllegalArgumentException("shopId must be positive, was " + value);
		}
	}

	public static ShopId of(long value) {
		return new ShopId(value);
	}

	@Override
	public String toString() {
		return "shop-" + value;
	}
}

package com.shop.inventory;

import com.shop.platform.core.Identifier;

/** Typed identifier for a stock {@code location}. Allocated by the tenant shard on insert. */
public record LocationId(long value) implements Identifier {

	public LocationId {
		if (value <= 0) {
			throw new IllegalArgumentException("locationId must be positive, was " + value);
		}
	}

	public static LocationId of(long value) {
		return new LocationId(value);
	}

	@Override
	public String toString() {
		return "location-" + value;
	}
}

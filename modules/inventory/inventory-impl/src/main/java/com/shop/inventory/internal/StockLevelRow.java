package com.shop.inventory.internal;

import java.time.Instant;

/**
 * One row of the tenant-sharded {@code stock_level} table — the available/reserved quantity of a variant
 * at a location. Keyed by ({@code shopId}, {@code variantId}, {@code locationId}); no surrogate id. A
 * mutable MyBatis POJO, internal to the inventory module.
 */
public class StockLevelRow {

	private Long shopId;
	private Long variantId;
	private Long locationId;
	private Long available;
	private Long reserved;
	private Instant updatedAt;

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public Long getVariantId() {
		return variantId;
	}

	public void setVariantId(Long variantId) {
		this.variantId = variantId;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public Long getAvailable() {
		return available;
	}

	public void setAvailable(Long available) {
		this.available = available;
	}

	public Long getReserved() {
		return reserved;
	}

	public void setReserved(Long reserved) {
		this.reserved = reserved;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}

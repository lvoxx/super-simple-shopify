package com.shop.inventory.internal;

import java.time.Instant;

/**
 * One row of the tenant-sharded {@code stock_location} table — a mutable MyBatis POJO, internal to the
 * inventory module. {@code shopId} is the tenant key; {@code id} is allocated by the shard on insert.
 */
public class LocationRow {

	private Long id;
	private Long shopId;
	private String name;
	private Instant createdAt;
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}

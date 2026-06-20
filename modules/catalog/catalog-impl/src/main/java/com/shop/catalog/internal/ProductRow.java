package com.shop.catalog.internal;

import java.time.Instant;

/**
 * One row of the tenant-sharded {@code product} table — a mutable MyBatis POJO, internal to the catalog
 * module (never crosses a boundary; {@link com.shop.catalog.ProductView} does). {@code status} is stored
 * as its enum name; {@code shopId} is the tenant key, mandatory on every tenant row; {@code id} is
 * allocated by the shard on insert. {@code publishedAt} is null until first published.
 */
public class ProductRow {

	private Long id;
	private Long shopId;
	private String title;
	private String description;
	private String handle;
	private String status;
	private String vendor;
	private String productType;
	private Instant publishedAt;
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Instant publishedAt) {
		this.publishedAt = publishedAt;
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

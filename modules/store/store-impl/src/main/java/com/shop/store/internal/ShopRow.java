package com.shop.store.internal;

import java.time.Instant;

/**
 * One row of the control-plane {@code shop_registry} table — a mutable MyBatis POJO, internal to the
 * store module (never crosses a boundary; {@link com.shop.store.ShopView} does). {@code plan} and
 * {@code status} are stored as their enum names. The shop's id is the {@code ShopId} value.
 */
public class ShopRow {

	private Long id;
	private String name;
	private String plan;
	private String status;
	private String locale;
	private String primaryDomain;
	private Instant createdAt;
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getPrimaryDomain() {
		return primaryDomain;
	}

	public void setPrimaryDomain(String primaryDomain) {
		this.primaryDomain = primaryDomain;
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

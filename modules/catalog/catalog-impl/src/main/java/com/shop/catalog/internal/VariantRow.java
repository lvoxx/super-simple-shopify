package com.shop.catalog.internal;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the tenant-sharded {@code product_variant} table — a mutable MyBatis POJO, internal to the
 * catalog module. Price is stored decomposed into {@code priceAmount} + {@code priceCurrency} (ISO 4217)
 * rather than as a {@code Money}, since the row maps 1:1 to columns; the assembler reconstitutes
 * {@link com.shop.platform.core.Money} at the boundary. {@code shopId} is the tenant key.
 */
public class VariantRow {

	private Long id;
	private Long shopId;
	private Long productId;
	private String sku;
	private String title;
	private BigDecimal priceAmount;
	private String priceCurrency;
	private Integer position;
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

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public void setPriceAmount(BigDecimal priceAmount) {
		this.priceAmount = priceAmount;
	}

	public String getPriceCurrency() {
		return priceCurrency;
	}

	public void setPriceCurrency(String priceCurrency) {
		this.priceCurrency = priceCurrency;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
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

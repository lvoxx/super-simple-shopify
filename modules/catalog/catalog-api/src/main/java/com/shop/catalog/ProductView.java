package com.shop.catalog;

import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.List;

/**
 * Read model of a product with its variants — a record DTO, never the persistence row. {@code publishedAt}
 * is {@code null} until the product is first published. Variants are ordered by their position.
 */
public record ProductView(ProductId id, ShopId shopId, String title, String description, String handle,
		ProductStatus status, String vendor, String productType, Instant publishedAt, Instant createdAt,
		List<VariantView> variants) {

	public ProductView {
		variants = variants == null ? List.of() : List.copyOf(variants);
	}
}

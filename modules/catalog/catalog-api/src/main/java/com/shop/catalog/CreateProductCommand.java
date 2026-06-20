package com.shop.catalog;

import java.util.List;

/**
 * Command to create a product in the current shop together with its variants. The target shop is the
 * active tenant ({@code TenantContext}), never a field here. {@code handle} is the URL slug, unique per
 * shop. A product is created {@link ProductStatus#DRAFT} and made visible later via
 * {@link CatalogFacade#publish(ProductId)}. At least one variant is required — a product with nothing to
 * buy is not sellable.
 */
public record CreateProductCommand(String title, String description, String handle, String vendor,
		String productType, List<NewVariant> variants) {

	public CreateProductCommand {
		variants = variants == null ? List.of() : List.copyOf(variants);
	}

	/** A variant to create alongside the product. {@code price} is authoritative catalog price. */
	public record NewVariant(String sku, String title, com.shop.platform.core.Money price) {
	}
}

package com.shop.catalog;

import com.shop.platform.core.Money;

/**
 * Read model of a variant returned across the module boundary — a record DTO, never the persistence row.
 * {@code price} is the authoritative catalog price; stock is owned by the {@code inventory} module and
 * is intentionally absent here.
 */
public record VariantView(VariantId id, ProductId productId, String sku, String title, Money price,
		int position) {
}

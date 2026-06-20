package com.shop.catalog;

import com.shop.platform.core.Money;

/**
 * Command to edit a single variant of the current shop. Identifies the variant by {@link VariantId};
 * the owning product and shop are resolved from it under the active tenant. A successful update raises
 * {@link VariantUpdated} so dependent reads (cache, search, cart re-resolution) can react.
 */
public record UpdateVariantCommand(VariantId variantId, String sku, String title, Money price) {
}

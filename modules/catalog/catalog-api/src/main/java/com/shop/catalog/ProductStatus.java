package com.shop.catalog;

/**
 * Merchandising lifecycle of a product. {@link #DRAFT} is being worked on and is invisible to buyers;
 * {@link #ACTIVE} is published and visible on the storefront (publishing transitions DRAFT/ARCHIVED →
 * ACTIVE and raises {@link ProductPublished}); {@link #ARCHIVED} is retired — hidden from buyers but
 * retained for historical orders rather than deleted.
 */
public enum ProductStatus {
	DRAFT,
	ACTIVE,
	ARCHIVED
}

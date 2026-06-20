package com.shop.catalog;

import java.util.Optional;

/**
 * The catalog module's public, synchronous API. The admin surface and other modules call this to model
 * and read merchandise; nobody reaches into the module's persistence. All operations are
 * <strong>tenant-scoped</strong> — they act on the active {@code TenantContext} shop and fail hard if
 * none is bound. Catalog never writes stock; quantities are owned by the {@code inventory} module.
 */
public interface CatalogFacade {

	/** Create a product (and its variants) in the current shop, in {@link ProductStatus#DRAFT}. */
	ProductView create(CreateProductCommand command);

	/** Load a product with its variants in the current shop, or empty if no such product. */
	Optional<ProductView> findProduct(ProductId productId);

	/**
	 * Publish a product — transition it to {@link ProductStatus#ACTIVE} and emit {@link ProductPublished}.
	 * Idempotent: publishing an already-published product is a no-op that returns the current view.
	 */
	ProductView publish(ProductId productId);

	/**
	 * Unpublish a product — transition it back to {@link ProductStatus#DRAFT}. Hides it from buyers
	 * without archiving. Idempotent for an already-unpublished product.
	 */
	ProductView unpublish(ProductId productId);

	/** Edit a variant's sellable attributes and emit {@link VariantUpdated}. */
	VariantView updateVariant(UpdateVariantCommand command);
}

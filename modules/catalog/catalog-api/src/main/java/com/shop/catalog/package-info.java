/**
 * The {@code catalog} bounded context — the system of record for what a shop <strong>sells</strong>:
 * {@link com.shop.catalog.ProductView products} and their {@link com.shop.catalog.VariantView variants}
 * (the individually-priced, individually-purchasable units). A product is the merchandising entity a
 * buyer browses; a variant is what actually goes in a cart and is decremented from stock.
 *
 * <p>Catalog owns descriptive/merchandising data only — it never writes stock. Quantities live in the
 * {@code inventory} module; the two integrate through events, not shared tables (a later Phase 2
 * deliverable). Products move through a {@link com.shop.catalog.ProductStatus lifecycle}; publishing one
 * raises {@link com.shop.catalog.ProductPublished} (the signal storefront/search/cache consume), and an
 * edit to a sellable unit raises {@link com.shop.catalog.VariantUpdated}.
 *
 * <p>Products and variants are <strong>tenant</strong> data — sharded and indexed by {@code shop_id};
 * every read is tenant-scoped via {@code TenantContext}. This package is the module's public API
 * (records, events, and the {@link com.shop.catalog.CatalogFacade} interface); the implementation lives
 * in {@code com.shop.catalog.internal}.
 */
package com.shop.catalog;

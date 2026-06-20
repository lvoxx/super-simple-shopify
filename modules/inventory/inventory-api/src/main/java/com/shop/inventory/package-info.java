/**
 * The {@code inventory} bounded context — the system of record for <strong>how much</strong> of each
 * catalog variant is in stock and <strong>where</strong>. It owns {@link com.shop.inventory.LocationView
 * locations} (places stock is held), stock levels (available + reserved quantity of a variant at a
 * location), and {@link com.shop.inventory.ReservationView reservations} (temporary holds taken during
 * checkout and released on expiry).
 *
 * <p>Inventory references a catalog variant only as an opaque {@link com.shop.inventory.VariantRef} (a
 * variant id); it never reads catalog merchandising data on the stock path, and catalog never writes
 * stock. The two contexts integrate through events, not shared tables. Adjusting stock raises
 * {@link com.shop.inventory.StockAdjusted}; a lapsed hold raises
 * {@link com.shop.inventory.ReservationExpired} (drained by the job-engine to return the held quantity).
 *
 * <p>All inventory data is <strong>tenant</strong> data — sharded and indexed by {@code shop_id}; every
 * read is tenant-scoped via {@code TenantContext}. This package is the module's public API; the
 * implementation lives in {@code com.shop.inventory.internal}.
 */
package com.shop.inventory;

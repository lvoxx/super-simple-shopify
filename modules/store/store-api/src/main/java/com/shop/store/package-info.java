/**
 * The {@code store} bounded context — the system of record for shops (tenants): their identity, plan,
 * locale, domains, status, and shard assignment. This is the public API of the module (records, the
 * {@link com.shop.store.ShopCreated} event, and the {@link com.shop.store.StoreFacade} interface);
 * the implementation lives in {@code com.shop.store.internal} (a Spring Modulith internal package).
 *
 * <p>Shops are <strong>control-plane</strong> data: the registry is global (non-sharded) because a
 * shop must be resolvable to a shard before any tenant query can run. Other modules depend only on
 * this API and never on the store's persistence.
 */
package com.shop.store;

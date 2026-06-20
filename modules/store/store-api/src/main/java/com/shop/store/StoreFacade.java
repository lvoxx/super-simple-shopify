package com.shop.store;

import com.shop.platform.core.ShopId;
import java.util.Optional;

/**
 * The store module's public, synchronous API. Other modules and the app's control-plane surface call
 * this; nobody reaches into the store's persistence. Shop reads are control-plane lookups (global),
 * so unlike tenant queries they do not require a bound {@code TenantContext}.
 */
public interface StoreFacade {

	/** Provision a new shop: allocate its id, assign a shard, and emit {@link ShopCreated}. */
	ShopView createShop(CreateShopCommand command);

	/** Look up a shop by id, or empty if no such shop exists. */
	Optional<ShopView> findShop(ShopId shopId);

	/** Resolve a host/domain to its shop — the storefront tenant-resolution entry point. */
	Optional<ShopView> findByDomain(String host);
}

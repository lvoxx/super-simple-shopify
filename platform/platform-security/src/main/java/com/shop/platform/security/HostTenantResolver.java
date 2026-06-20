package com.shop.platform.security;

import com.shop.platform.core.ShopId;
import java.util.Optional;

/**
 * SPI for resolving a storefront request's host/domain to its shop. {@code platform-security} defines the
 * port; the app (composition root) supplies the adapter over the store module's {@code StoreFacade}, so
 * platform code never depends on a domain module. Admin requests instead carry {@code X-Shop-Id} and do
 * not use this resolver.
 */
@FunctionalInterface
public interface HostTenantResolver {

	/** The shop that owns {@code host}, or empty if the host maps to no shop. */
	Optional<ShopId> resolveByHost(String host);
}

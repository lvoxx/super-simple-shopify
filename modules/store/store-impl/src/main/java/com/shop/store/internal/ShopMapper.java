package com.shop.store.internal;

import com.shop.platform.persistence.ControlMapper;
import org.apache.ibatis.annotations.Param;

/**
 * Control-plane mapper for the shop registry. {@code @ControlMapper}, so it runs against the global
 * control datasource — the shop registry must be readable before any shard is known. The insert uses
 * generated keys to populate {@link ShopRow#getId()} with the new shop id.
 */
@ControlMapper
public interface ShopMapper {

	void insert(ShopRow row);

	ShopRow findById(@Param("id") long id);

	/** Resolve a host to its shop by joining the {@code shop_domain} map — storefront tenant resolution. */
	ShopRow findByHost(@Param("host") String host);
}

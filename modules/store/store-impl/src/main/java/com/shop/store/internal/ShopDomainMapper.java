package com.shop.store.internal;

import com.shop.platform.persistence.ControlMapper;
import org.apache.ibatis.annotations.Param;

/**
 * Control-plane mapper for the shop&rarr;domain map ({@code shop_domain}). Hosts are globally unique
 * so a storefront request can resolve a host to a shop before any tenant context exists.
 */
@ControlMapper
public interface ShopDomainMapper {

	void insert(@Param("shopId") long shopId, @Param("host") String host, @Param("primary") boolean primary);
}

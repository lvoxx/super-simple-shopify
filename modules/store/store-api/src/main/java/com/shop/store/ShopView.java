package com.shop.store;

import com.shop.platform.core.ShopId;
import java.time.Instant;

/**
 * Read model of a shop returned across the module boundary — a record DTO, never the persistence row.
 *
 * @param shardIndex the physical shard this shop's tenant data lives on (control-plane assignment)
 */
public record ShopView(ShopId id, String name, ShopPlan plan, ShopStatus status, String locale,
		String primaryDomain, int shardIndex, Instant createdAt) {
}

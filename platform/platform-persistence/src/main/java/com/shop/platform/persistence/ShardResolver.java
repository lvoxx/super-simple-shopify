package com.shop.platform.persistence;

import com.shop.platform.core.ShopId;

/**
 * Maps a {@link ShopId} to a shard index. The mapping is deliberately trivial and
 * deterministic ({@code shopId mod shardCount}) so the same shop always routes to the same
 * shard. With {@code shardCount == 1} (the Phase 0 default) every tenant lands on shard 0,
 * but the routing path is identical to the multi-shard case proven in Phase 8.
 */
public final class ShardResolver {

	private final int shardCount;

	public ShardResolver(int shardCount) {
		if (shardCount < 1) {
			throw new IllegalArgumentException("shardCount must be >= 1, was " + shardCount);
		}
		this.shardCount = shardCount;
	}

	public int shardCount() {
		return shardCount;
	}

	public int shardFor(ShopId shopId) {
		return (int) Math.floorMod(shopId.value(), (long) shardCount);
	}
}

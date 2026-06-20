package com.shop.platform.persistence;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TimeProvider;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The control-plane registry that assigns a shop to a shard and records it in the shop&rarr;shard map.
 * Assignment is deterministic ({@link ShardResolver}) so a shop's shard never moves; the persisted row
 * is the durable source of truth that Phase 8 routing reads (Phase 1 routing still computes it).
 *
 * <p>Callers invoke {@link #assign(ShopId)} inside the control-plane transaction that creates the shop,
 * so a shop and its shard assignment commit together.
 */
@Component
public class ShopShardRegistry {

	private final ShardResolver shardResolver;
	private final ShopShardAssignmentMapper assignments;
	private final TimeProvider time;

	public ShopShardRegistry(ShardResolver shardResolver, ShopShardAssignmentMapper assignments, TimeProvider time) {
		this.shardResolver = shardResolver;
		this.assignments = assignments;
		this.time = time;
	}

	/** Assign {@code shopId} to its deterministic shard and persist the mapping. Returns the shard index. */
	public int assign(ShopId shopId) {
		int shard = shardResolver.shardFor(shopId);
		assignments.insert(shopId.value(), shard, time.now());
		return shard;
	}

	public Optional<Integer> shardOf(ShopId shopId) {
		return Optional.ofNullable(assignments.findShard(shopId.value()));
	}
}

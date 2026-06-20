package com.shop.platform.persistence;

import java.time.Instant;
import org.apache.ibatis.annotations.Param;

/**
 * Control-plane mapper for the shop&rarr;shard map. This table is the source of truth for which
 * physical shard owns a shop; it lives on the control plane (global, non-sharded) because resolving a
 * shop to its shard cannot itself require knowing the shard. Written once when a shop is created.
 *
 * <p>Phase 1 records the assignment but routing still uses the deterministic {@link ShardResolver}
 * ({@code shopId mod shardCount}); Phase 8 switches routing to a cached lookup of this table.
 */
@ControlMapper
public interface ShopShardAssignmentMapper {

	void insert(@Param("shopId") long shopId, @Param("shardIndex") int shardIndex,
			@Param("assignedAt") Instant assignedAt);

	Integer findShard(@Param("shopId") long shopId);
}

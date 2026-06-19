package com.shop.platform.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code platform.persistence.shard-count} — how many physical shards exist. Defaults to 1
 * for local/dev; staging proves {@code > 1} in Phase 8.
 */
@ConfigurationProperties(prefix = "platform.persistence")
public class ShardingProperties {

	private int shardCount = 1;

	public int getShardCount() {
		return shardCount;
	}

	public void setShardCount(int shardCount) {
		this.shardCount = shardCount;
	}
}

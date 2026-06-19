package com.shop.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.platform.core.ShopId;
import org.junit.jupiter.api.Test;

class ShardResolverTest {

	@Test
	void singleShardRoutesEveryTenantToShardZero() {
		var resolver = new ShardResolver(1);
		assertThat(resolver.shardFor(ShopId.of(1))).isZero();
		assertThat(resolver.shardFor(ShopId.of(987654))).isZero();
	}

	@Test
	void isDeterministicAcrossShards() {
		var resolver = new ShardResolver(4);
		assertThat(resolver.shardFor(ShopId.of(10))).isEqualTo(resolver.shardFor(ShopId.of(10)));
		assertThat(resolver.shardFor(ShopId.of(7))).isEqualTo(3);
	}

	@Test
	void rejectsZeroShardCount() {
		assertThatThrownBy(() -> new ShardResolver(0)).isInstanceOf(IllegalArgumentException.class);
	}
}

package com.shop.platform.persistence;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires shard-aware persistence. Defining the {@link DataSource} bean makes Spring Boot back off
 * its single-datasource creation, so every connection flows through the
 * {@link TenantRoutingDataSource}. Connection settings come from a {@link JdbcConnectionDetails}
 * bean, so the same code path serves local {@code spring.datasource.*} properties and the
 * Testcontainers {@code @ServiceConnection} used in integration tests.
 *
 * <p>Phase 0 ({@code shard-count=1}) backs every shard index with one physical datasource; Phase 8
 * supplies a distinct datasource per shard from config.
 */
@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class PersistenceConfig {

	@Bean
	public ShardResolver shardResolver(ShardingProperties properties) {
		return new ShardResolver(properties.getShardCount());
	}

	@Bean
	@Primary
	public DataSource shardRoutingDataSource(ShardResolver shardResolver, JdbcConnectionDetails connection) {
		DataSource shardZero = DataSourceBuilder.create()
				.url(connection.getJdbcUrl())
				.username(connection.getUsername())
				.password(connection.getPassword())
				.driverClassName(connection.getDriverClassName())
				.build();
		Map<Object, Object> targets = new HashMap<>();
		for (int shard = 0; shard < shardResolver.shardCount(); shard++) {
			targets.put(shard, shardZero);
		}
		var routing = new TenantRoutingDataSource(shardResolver);
		routing.setTargetDataSources(targets);
		routing.setDefaultTargetDataSource(shardZero);
		routing.afterPropertiesSet();
		return routing;
	}
}

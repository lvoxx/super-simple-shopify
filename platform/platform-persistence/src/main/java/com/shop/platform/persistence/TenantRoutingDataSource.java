package com.shop.platform.persistence;

import com.shop.platform.core.TenantContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes every connection to the shard owning the current tenant. The lookup key is derived
 * from {@link TenantContext} — application code never picks a datasource. A missing tenant is
 * a hard failure (via {@link TenantContext#requireShop()}), never a silent default.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

	private final ShardResolver shardResolver;

	public TenantRoutingDataSource(ShardResolver shardResolver) {
		this.shardResolver = shardResolver;
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return shardResolver.shardFor(TenantContext.requireShop());
	}
}

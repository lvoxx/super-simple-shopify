package com.shop.store.internal;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TimeProvider;
import com.shop.platform.events.ControlPlaneEventPublisher;
import com.shop.platform.persistence.ControlPlaneConfig;
import com.shop.platform.persistence.ShopShardRegistry;
import com.shop.store.CreateShopCommand;
import com.shop.store.ShopCreated;
import com.shop.store.ShopView;
import com.shop.store.StoreFacade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The store module's implementation of {@link StoreFacade}. Shop creation is a single control-plane
 * transaction: insert the registry row (which allocates the shop id), assign the shard, register the
 * primary domain, and publish {@link ShopCreated} to the control outbox — all under
 * {@code controlTransactionManager}, so either everything commits or nothing does. No tenant shard is
 * touched, so there is no distributed transaction.
 *
 * <p>Reads ({@link #findShop}, {@link #findByDomain}) are control-plane lookups and need no tenant
 * context — they are how a tenant is found in the first place.
 */
@Service
public class StoreService implements StoreFacade {

	private final ShopMapper shops;
	private final ShopDomainMapper domains;
	private final ShopShardRegistry shardRegistry;
	private final ControlPlaneEventPublisher events;
	private final ShopAssembler assembler;
	private final TimeProvider time;

	public StoreService(ShopMapper shops, ShopDomainMapper domains, ShopShardRegistry shardRegistry,
			ControlPlaneEventPublisher events, ShopAssembler assembler, TimeProvider time) {
		this.shops = shops;
		this.domains = domains;
		this.shardRegistry = shardRegistry;
		this.events = events;
		this.assembler = assembler;
		this.time = time;
	}

	@Override
	@Transactional(ControlPlaneConfig.TRANSACTION_MANAGER)
	public ShopView createShop(CreateShopCommand command) {
		var row = assembler.toNewRow(command, time.now());
		shops.insert(row);
		ShopId shopId = ShopId.of(row.getId());
		int shard = shardRegistry.assign(shopId);
		domains.insert(shopId.value(), command.primaryDomain(), true);
		events.publish(new ShopCreated(UUID.randomUUID(), time.now(), shopId, row.getName(), shard));
		return assembler.toView(row, shard);
	}

	@Override
	public Optional<ShopView> findShop(ShopId shopId) {
		return Optional.ofNullable(shops.findById(shopId.value()))
				.map(row -> assembler.toView(row, shardRegistry.shardOf(shopId).orElse(-1)));
	}

	@Override
	public Optional<ShopView> findByDomain(String host) {
		return Optional.ofNullable(shops.findByHost(host))
				.map(row -> assembler.toView(row, shardRegistry.shardOf(ShopId.of(row.getId())).orElse(-1)));
	}
}

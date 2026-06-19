package com.shop.platform.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries the active {@link ShopId} for the dynamic scope of a request or job, using a
 * {@link ScopedValue} (final in JDK 25). The holder lives in platform-core so that both
 * platform-security (which binds it per request) and platform-persistence (which reads it to
 * route to a shard) depend only on core, never on each other.
 */
public final class TenantContext {

	private static final ScopedValue<ShopId> CURRENT = ScopedValue.newInstance();

	private TenantContext() {
	}

	/** Work that may throw, run inside a tenant scope. */
	@FunctionalInterface
	public interface TenantWork<R> {
		R run() throws Exception;
	}

	/** Bind {@code shop} for the duration of {@code work} (value-returning, may throw). */
	public static <R> R callWithShop(ShopId shop, TenantWork<R> work) throws Exception {
		Objects.requireNonNull(shop, "shop");
		return ScopedValue.where(CURRENT, shop).call(work::run);
	}

	/** Bind {@code shop} for the duration of {@code work}. */
	public static void runWithShop(ShopId shop, Runnable work) {
		Objects.requireNonNull(shop, "shop");
		ScopedValue.where(CURRENT, shop).run(work);
	}

	/** The active tenant, or a hard failure if none is bound. */
	public static ShopId requireShop() {
		if (!CURRENT.isBound()) {
			throw new TenantContextMissingException();
		}
		return CURRENT.get();
	}

	public static Optional<ShopId> currentShop() {
		return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
	}
}

package com.shop.platform.core;

import java.time.Instant;

/** Wall-clock {@link TimeProvider} backed by the system UTC clock. */
public final class SystemTimeProvider implements TimeProvider {

	@Override
	public Instant now() {
		return Instant.now();
	}
}

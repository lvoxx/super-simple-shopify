package com.shop.platform.core;

import java.time.Instant;

/**
 * Indirection over the system clock so time-dependent logic is testable. Production wires the
 * {@link SystemTimeProvider}; tests substitute a fixed instant.
 */
public interface TimeProvider {

	Instant now();
}

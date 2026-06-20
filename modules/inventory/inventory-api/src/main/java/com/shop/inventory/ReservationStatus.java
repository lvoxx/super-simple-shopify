package com.shop.inventory;

/**
 * Lifecycle of a stock reservation. {@link #HELD} is an active hold counting against available stock;
 * {@link #RELEASED} was returned to available before use (e.g. an abandoned checkout); {@link #COMMITTED}
 * was consumed by a placed order (held quantity becomes a permanent decrement); {@link #EXPIRED} lapsed
 * past its deadline and was swept back to available by the job-engine (raising
 * {@link ReservationExpired}).
 */
public enum ReservationStatus {
	HELD,
	RELEASED,
	COMMITTED,
	EXPIRED
}

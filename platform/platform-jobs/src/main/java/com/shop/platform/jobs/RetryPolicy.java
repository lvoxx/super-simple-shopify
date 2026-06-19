package com.shop.platform.jobs;

import java.time.Duration;

/**
 * Exponential backoff with a cap. After {@code maxAttempts} the message is dead-lettered.
 */
public record RetryPolicy(int maxAttempts, Duration baseBackoff, Duration maxBackoff) {

	public static RetryPolicy defaults() {
		return new RetryPolicy(5, Duration.ofSeconds(2), Duration.ofMinutes(5));
	}

	public Duration backoffFor(int attempt) {
		long millis = (long) (baseBackoff.toMillis() * Math.pow(2, Math.max(0, attempt - 1)));
		return Duration.ofMillis(Math.min(millis, maxBackoff.toMillis()));
	}

	public boolean exhausted(int attempt) {
		return attempt >= maxAttempts;
	}
}

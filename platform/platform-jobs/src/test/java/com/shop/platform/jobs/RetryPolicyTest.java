package com.shop.platform.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

	@Test
	void backoffGrowsExponentiallyThenCaps() {
		var policy = new RetryPolicy(5, Duration.ofSeconds(2), Duration.ofSeconds(10));
		assertThat(policy.backoffFor(1)).isEqualTo(Duration.ofSeconds(2));
		assertThat(policy.backoffFor(2)).isEqualTo(Duration.ofSeconds(4));
		assertThat(policy.backoffFor(10)).isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void exhaustsAtMaxAttempts() {
		var policy = RetryPolicy.defaults();
		assertThat(policy.exhausted(5)).isTrue();
		assertThat(policy.exhausted(4)).isFalse();
	}
}

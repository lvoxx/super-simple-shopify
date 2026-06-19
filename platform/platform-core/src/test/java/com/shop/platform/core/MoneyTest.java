package com.shop.platform.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

	@Test
	void normalisesScaleToCurrencyFractionDigits() {
		assertThat(Money.of("10.1", "USD").amount().scale()).isEqualTo(2);
	}

	@Test
	void addsSameCurrency() {
		assertThat(Money.of("10.00", "USD").add(Money.of("2.50", "USD")))
				.isEqualTo(Money.of("12.50", "USD"));
	}

	@Test
	void rejectsCurrencyMismatch() {
		assertThatThrownBy(() -> Money.of("1", "USD").add(Money.of("1", "EUR")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}

package com.shop.platform.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Currency-safe money. Amounts are normalised to the currency's fraction digits with
 * banker's rounding; arithmetic across mismatched currencies is rejected outright.
 */
public record Money(BigDecimal amount, Currency currency) {

	public Money {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(currency, "currency");
		amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
	}

	public static Money of(String amount, String currencyCode) {
		return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
	}

	public static Money zero(Currency currency) {
		return new Money(BigDecimal.ZERO, currency);
	}

	public Money add(Money other) {
		requireSameCurrency(other);
		return new Money(amount.add(other.amount), currency);
	}

	public Money subtract(Money other) {
		requireSameCurrency(other);
		return new Money(amount.subtract(other.amount), currency);
	}

	public Money multiply(long quantity) {
		return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
	}

	public boolean isNegative() {
		return amount.signum() < 0;
	}

	private void requireSameCurrency(Money other) {
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException(
					"currency mismatch: " + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
		}
	}
}

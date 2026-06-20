package com.shop.inventory;

import com.shop.platform.core.Identifier;

/** Typed identifier for a stock {@code reservation} (a hold taken during checkout). */
public record ReservationId(long value) implements Identifier {

	public ReservationId {
		if (value <= 0) {
			throw new IllegalArgumentException("reservationId must be positive, was " + value);
		}
	}

	public static ReservationId of(long value) {
		return new ReservationId(value);
	}

	@Override
	public String toString() {
		return "reservation-" + value;
	}
}

package com.shop.inventory;

import java.time.Instant;

/** Read model of a stock reservation — a record DTO, never the persistence row. */
public record ReservationView(ReservationId id, VariantRef variant, LocationId location, long quantity,
		ReservationStatus status, Instant expiresAt) {
}

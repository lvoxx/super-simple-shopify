package com.shop.inventory;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a {@link ReservationStatus#HELD} reservation lapses past its deadline and its quantity is
 * swept back to available. Emitted by the expiry sweep within the same transaction that returns the
 * stock. Checkout (a later phase) consumes this to fail an abandoned session cleanly.
 */
public record ReservationExpired(UUID eventId, Instant occurredAt, ShopId shopId, ReservationId reservation,
		VariantRef variant, LocationId location, long quantity) implements DomainEvent {
}

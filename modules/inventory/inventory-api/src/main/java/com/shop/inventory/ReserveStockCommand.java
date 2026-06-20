package com.shop.inventory;

import java.time.Duration;

/**
 * Command to place a hold on {@code quantity} units of a variant at a location for {@code ttl}. Moves the
 * quantity from available to reserved and creates a {@link ReservationStatus#HELD} reservation that
 * lapses after the ttl. Fails if available stock is insufficient. Used by checkout (a later phase);
 * exposed now so the reservation lifecycle is complete and testable.
 */
public record ReserveStockCommand(VariantRef variant, LocationId location, long quantity, Duration ttl) {
}

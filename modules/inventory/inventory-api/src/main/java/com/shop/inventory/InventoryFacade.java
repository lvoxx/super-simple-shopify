package com.shop.inventory;

import java.util.List;
import java.util.Optional;

/**
 * The inventory module's public, synchronous API. The admin surface and other modules (checkout, in a
 * later phase) call this to manage and consult stock; nobody reaches into the module's persistence. All
 * operations are <strong>tenant-scoped</strong> — they act on the active {@code TenantContext} shop and
 * fail hard if none is bound.
 */
public interface InventoryFacade {

	/** Create a stock location in the current shop. */
	LocationView createLocation(CreateLocationCommand command);

	/** Read a variant's stock at a location, or empty if no level has been recorded yet. */
	Optional<StockLevelView> findStock(VariantRef variant, LocationId location);

	/**
	 * Change available stock by a delta and emit {@link StockAdjusted}. Creates the stock level on first
	 * adjustment; rejects an adjustment that would drive available below zero.
	 */
	StockLevelView adjust(AdjustStockCommand command);

	/**
	 * Place a hold on stock, returning the reservation. Moves quantity from available to reserved; fails
	 * if available is insufficient.
	 */
	ReservationView reserve(ReserveStockCommand command);

	/** Release a held reservation, returning its quantity to available. Idempotent once released. */
	ReservationView release(ReservationId reservationId);

	/**
	 * Sweep reservations that have lapsed: return each held quantity to available, mark it
	 * {@link ReservationStatus#EXPIRED}, and emit {@link ReservationExpired}. Returns the expired
	 * reservations. Idempotent — already-resolved reservations are skipped. The job-engine drives this.
	 */
	List<ReservationView> expireDueReservations();
}

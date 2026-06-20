package com.shop.inventory.internal;

import com.shop.inventory.AdjustStockCommand;
import com.shop.inventory.CreateLocationCommand;
import com.shop.inventory.InventoryFacade;
import com.shop.inventory.LocationId;
import com.shop.inventory.LocationView;
import com.shop.inventory.ReservationExpired;
import com.shop.inventory.ReservationId;
import com.shop.inventory.ReservationStatus;
import com.shop.inventory.ReservationView;
import com.shop.inventory.ReserveStockCommand;
import com.shop.inventory.StockAdjusted;
import com.shop.inventory.StockLevelView;
import com.shop.inventory.VariantRef;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.core.TimeProvider;
import com.shop.platform.events.DomainEventPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inventory module's implementation of {@link InventoryFacade}. Every operation is tenant-scoped: the
 * shop comes from {@link TenantContext#requireShop()} (a hard failure if unbound). Each mutation is one
 * tenant transaction so the stock change and its outbox event commit together: an adjustment writes the
 * level and {@link StockAdjusted} atomically; the expiry sweep returns each held quantity and emits
 * {@link ReservationExpired} atomically. Available stock is never allowed below zero.
 */
@Service
public class InventoryService implements InventoryFacade {

	private final InventoryMapper inventory;
	private final InventoryAssembler assembler;
	private final DomainEventPublisher events;
	private final TimeProvider time;

	public InventoryService(InventoryMapper inventory, InventoryAssembler assembler,
			DomainEventPublisher events, TimeProvider time) {
		this.inventory = inventory;
		this.assembler = assembler;
		this.events = events;
		this.time = time;
	}

	@Override
	@Transactional
	public LocationView createLocation(CreateLocationCommand command) {
		ShopId shopId = TenantContext.requireShop();
		var row = assembler.toNewLocationRow(shopId, command.name(), time.now());
		inventory.insertLocation(row);
		return assembler.toLocationView(row);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StockLevelView> findStock(VariantRef variant, LocationId location) {
		ShopId shopId = TenantContext.requireShop();
		StockLevelRow row = inventory.findStock(shopId.value(), variant.value(), location.value());
		return Optional.ofNullable(row).map(assembler::toStockView);
	}

	@Override
	@Transactional
	public StockLevelView adjust(AdjustStockCommand command) {
		ShopId shopId = TenantContext.requireShop();
		requireLocation(shopId, command.location().value());
		var now = time.now();
		StockLevelRow row = inventory.findStock(shopId.value(), command.variant().value(),
				command.location().value());
		long newAvailable;
		if (row == null) {
			newAvailable = command.delta();
			if (newAvailable < 0) {
				throw insufficientStock();
			}
			row = new StockLevelRow();
			row.setShopId(shopId.value());
			row.setVariantId(command.variant().value());
			row.setLocationId(command.location().value());
			row.setAvailable(newAvailable);
			row.setReserved(0L);
			row.setUpdatedAt(now);
			inventory.insertStock(row);
		} else {
			newAvailable = row.getAvailable() + command.delta();
			if (newAvailable < 0) {
				throw insufficientStock();
			}
			row.setAvailable(newAvailable);
			row.setUpdatedAt(now);
			inventory.updateStockQuantities(shopId.value(), row.getVariantId(), row.getLocationId(),
					row.getAvailable(), row.getReserved(), now);
		}
		events.publish(new StockAdjusted(UUID.randomUUID(), now, shopId, command.variant(),
				command.location(), command.delta(), newAvailable));
		return assembler.toStockView(row);
	}

	@Override
	@Transactional
	public ReservationView reserve(ReserveStockCommand command) {
		ShopId shopId = TenantContext.requireShop();
		if (command.quantity() <= 0) {
			throw new DomainException("inventory.reservation.bad_quantity", ProblemCategory.VALIDATION,
					"Reservation quantity must be positive");
		}
		var now = time.now();
		StockLevelRow stock = inventory.findStock(shopId.value(), command.variant().value(),
				command.location().value());
		if (stock == null || stock.getAvailable() < command.quantity()) {
			throw insufficientStock();
		}
		stock.setAvailable(stock.getAvailable() - command.quantity());
		stock.setReserved(stock.getReserved() + command.quantity());
		stock.setUpdatedAt(now);
		inventory.updateStockQuantities(shopId.value(), stock.getVariantId(), stock.getLocationId(),
				stock.getAvailable(), stock.getReserved(), now);

		var reservation = new ReservationRow();
		reservation.setShopId(shopId.value());
		reservation.setVariantId(command.variant().value());
		reservation.setLocationId(command.location().value());
		reservation.setQuantity(command.quantity());
		reservation.setStatus(ReservationStatus.HELD.name());
		reservation.setExpiresAt(now.plus(command.ttl()));
		reservation.setCreatedAt(now);
		reservation.setUpdatedAt(now);
		inventory.insertReservation(reservation);
		return assembler.toReservationView(reservation);
	}

	@Override
	@Transactional
	public ReservationView release(ReservationId reservationId) {
		ShopId shopId = TenantContext.requireShop();
		ReservationRow reservation = inventory.findReservation(shopId.value(), reservationId.value());
		if (reservation == null) {
			throw new DomainException("inventory.reservation.not_found", ProblemCategory.NOT_FOUND,
					"No reservation " + reservationId.value() + " in shop " + shopId.value());
		}
		if (!ReservationStatus.HELD.name().equals(reservation.getStatus())) {
			// Idempotent: already released/expired/committed — nothing to return.
			return assembler.toReservationView(reservation);
		}
		returnHeldQuantity(shopId, reservation, ReservationStatus.RELEASED, time.now());
		return assembler.toReservationView(reservation);
	}

	@Override
	@Transactional
	public List<ReservationView> expireDueReservations() {
		ShopId shopId = TenantContext.requireShop();
		var now = time.now();
		List<ReservationRow> due = inventory.findDueReservations(shopId.value(), now);
		var expired = new ArrayList<ReservationView>();
		for (ReservationRow reservation : due) {
			returnHeldQuantity(shopId, reservation, ReservationStatus.EXPIRED, now);
			events.publish(new ReservationExpired(UUID.randomUUID(), now, shopId,
					ReservationId.of(reservation.getId()), VariantRef.of(reservation.getVariantId()),
					LocationId.of(reservation.getLocationId()), reservation.getQuantity()));
			expired.add(assembler.toReservationView(reservation));
		}
		return expired;
	}

	/** Move a held reservation's quantity from reserved back to available and set its terminal status. */
	private void returnHeldQuantity(ShopId shopId, ReservationRow reservation, ReservationStatus terminal,
			Instant now) {
		StockLevelRow stock = inventory.findStock(shopId.value(), reservation.getVariantId(),
				reservation.getLocationId());
		if (stock != null) {
			stock.setReserved(Math.max(0, stock.getReserved() - reservation.getQuantity()));
			stock.setAvailable(stock.getAvailable() + reservation.getQuantity());
			stock.setUpdatedAt(now);
			inventory.updateStockQuantities(shopId.value(), stock.getVariantId(), stock.getLocationId(),
					stock.getAvailable(), stock.getReserved(), now);
		}
		reservation.setStatus(terminal.name());
		reservation.setUpdatedAt(now);
		inventory.updateReservationStatus(shopId.value(), reservation.getId(), terminal.name(), now);
	}

	private void requireLocation(ShopId shopId, long locationId) {
		if (inventory.findLocation(shopId.value(), locationId) == null) {
			throw new DomainException("inventory.location.not_found", ProblemCategory.NOT_FOUND,
					"No location " + locationId + " in shop " + shopId.value());
		}
	}

	private DomainException insufficientStock() {
		return new DomainException("inventory.stock.insufficient", ProblemCategory.CONFLICT,
				"Insufficient available stock for the requested operation");
	}
}

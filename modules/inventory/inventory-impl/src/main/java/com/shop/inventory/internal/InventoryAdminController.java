package com.shop.inventory.internal;

import com.shop.inventory.AdjustStockCommand;
import com.shop.inventory.CreateLocationCommand;
import com.shop.inventory.InventoryFacade;
import com.shop.inventory.LocationId;
import com.shop.inventory.LocationView;
import com.shop.inventory.ReservationId;
import com.shop.inventory.ReservationView;
import com.shop.inventory.ReserveStockCommand;
import com.shop.inventory.StockLevelView;
import com.shop.inventory.VariantRef;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.web.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin surface for managing a shop's inventory. These endpoints are <strong>tenant-scoped</strong>: they
 * live under {@link ApiVersion#V1_PREFIX}, so the tenant-binding filter resolves {@code X-Shop-Id} and
 * binds {@code TenantContext} before the service runs. Inbound DTOs are the only place edge validation is
 * declared; the controller never reaches into persistence.
 */
@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/inventory")
public class InventoryAdminController {

	private final InventoryFacade inventory;

	public InventoryAdminController(InventoryFacade inventory) {
		this.inventory = inventory;
	}

	@PostMapping("/locations")
	@ResponseStatus(HttpStatus.CREATED)
	public LocationView createLocation(@Valid @RequestBody CreateLocationRequest request) {
		return inventory.createLocation(new CreateLocationCommand(request.name()));
	}

	@GetMapping("/stock/{variantId}/{locationId}")
	public StockLevelView stock(@PathVariable long variantId, @PathVariable long locationId) {
		return inventory.findStock(VariantRef.of(variantId), LocationId.of(locationId))
				.orElseThrow(() -> new DomainException("inventory.stock.not_found", ProblemCategory.NOT_FOUND,
						"No stock recorded for variant " + variantId + " at location " + locationId));
	}

	@PostMapping("/stock/adjust")
	public StockLevelView adjust(@Valid @RequestBody AdjustStockRequest request) {
		return inventory.adjust(new AdjustStockCommand(VariantRef.of(request.variantId()),
				LocationId.of(request.locationId()), request.delta(), request.reason()));
	}

	@PostMapping("/reservations")
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationView reserve(@Valid @RequestBody ReserveStockRequest request) {
		return inventory.reserve(new ReserveStockCommand(VariantRef.of(request.variantId()),
				LocationId.of(request.locationId()), request.quantity(),
				Duration.ofSeconds(request.ttlSeconds())));
	}

	@PostMapping("/reservations/{reservationId}/release")
	public ReservationView release(@PathVariable long reservationId) {
		return inventory.release(ReservationId.of(reservationId));
	}

	/** Sweep lapsed reservations for the current shop. Invoked operationally / by the job-engine. */
	@PostMapping("/reservations/expire")
	public List<ReservationView> expire() {
		return inventory.expireDueReservations();
	}

	/** Inbound request DTO for creating a location. */
	public record CreateLocationRequest(@NotBlank String name) {
	}

	/** Inbound request DTO for a stock adjustment; {@code delta} may be negative. */
	public record AdjustStockRequest(@Positive long variantId, @Positive long locationId, long delta,
			String reason) {
	}

	/** Inbound request DTO for placing a reservation. */
	public record ReserveStockRequest(@Positive long variantId, @Positive long locationId,
			@Positive long quantity, @Positive long ttlSeconds) {
	}
}

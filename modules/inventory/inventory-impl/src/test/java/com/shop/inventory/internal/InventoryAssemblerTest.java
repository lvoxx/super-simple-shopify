package com.shop.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.inventory.LocationView;
import com.shop.inventory.ReservationStatus;
import com.shop.inventory.ReservationView;
import com.shop.inventory.StockLevelView;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for the inventory row-to-DTO mapping — passes with only inventory-api + platform-core on
 * the classpath (no Spring, no other module impl), satisfying the module-slice isolation rule.
 */
class InventoryAssemblerTest {

	private final InventoryAssembler assembler = new InventoryAssembler();

	@Test
	void newLocationRowCarriesTenantAndTimestamps() {
		var now = Instant.parse("2026-06-20T10:15:30Z");

		LocationRow row = assembler.toNewLocationRow(ShopId.of(7), "Main Warehouse", now);

		assertThat(row.getShopId()).isEqualTo(7L);
		assertThat(row.getName()).isEqualTo("Main Warehouse");
		assertThat(row.getCreatedAt()).isEqualTo(now);
		assertThat(row.getUpdatedAt()).isEqualTo(now);
	}

	@Test
	void locationViewRoundTrips() {
		var row = assembler.toNewLocationRow(ShopId.of(7), "Main", Instant.parse("2026-06-20T10:15:30Z"));
		row.setId(99L);

		LocationView view = assembler.toLocationView(row);

		assertThat(view.id().value()).isEqualTo(99L);
		assertThat(view.shopId().value()).isEqualTo(7L);
		assertThat(view.name()).isEqualTo("Main");
	}

	@Test
	void stockViewExposesAvailableAndReserved() {
		var row = new StockLevelRow();
		row.setShopId(7L);
		row.setVariantId(200L);
		row.setLocationId(99L);
		row.setAvailable(42L);
		row.setReserved(8L);

		StockLevelView view = assembler.toStockView(row);

		assertThat(view.variant().value()).isEqualTo(200L);
		assertThat(view.location().value()).isEqualTo(99L);
		assertThat(view.available()).isEqualTo(42L);
		assertThat(view.reserved()).isEqualTo(8L);
	}

	@Test
	void reservationViewRoundTripsStatusEnum() {
		var row = new ReservationRow();
		row.setId(500L);
		row.setShopId(7L);
		row.setVariantId(200L);
		row.setLocationId(99L);
		row.setQuantity(3L);
		row.setStatus(ReservationStatus.HELD.name());
		row.setExpiresAt(Instant.parse("2026-06-20T11:00:00Z"));

		ReservationView view = assembler.toReservationView(row);

		assertThat(view.id().value()).isEqualTo(500L);
		assertThat(view.quantity()).isEqualTo(3L);
		assertThat(view.status()).isEqualTo(ReservationStatus.HELD);
		assertThat(view.expiresAt()).isEqualTo(Instant.parse("2026-06-20T11:00:00Z"));
	}
}

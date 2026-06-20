package com.shop.inventory.internal;

import com.shop.inventory.LocationId;
import com.shop.inventory.LocationView;
import com.shop.inventory.ReservationId;
import com.shop.inventory.ReservationStatus;
import com.shop.inventory.ReservationView;
import com.shop.inventory.StockLevelView;
import com.shop.inventory.VariantRef;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Centralizes the inventory module's row-to-DTO mapping. As in the sibling slices, ModelMapper is the
 * project default but the edges here cross typed-id, enum, and tenant-key boundaries, where explicit
 * construction is clearer. Mapping stays here, never inlined into the controller or persistence.
 */
@Component
public class InventoryAssembler {

	public LocationRow toNewLocationRow(ShopId shopId, String name, Instant now) {
		var row = new LocationRow();
		row.setShopId(shopId.value());
		row.setName(name);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		return row;
	}

	public LocationView toLocationView(LocationRow row) {
		return new LocationView(LocationId.of(row.getId()), ShopId.of(row.getShopId()), row.getName(),
				row.getCreatedAt());
	}

	public StockLevelView toStockView(StockLevelRow row) {
		return new StockLevelView(VariantRef.of(row.getVariantId()), LocationId.of(row.getLocationId()),
				row.getAvailable(), row.getReserved());
	}

	public ReservationView toReservationView(ReservationRow row) {
		return new ReservationView(ReservationId.of(row.getId()), VariantRef.of(row.getVariantId()),
				LocationId.of(row.getLocationId()), row.getQuantity(),
				ReservationStatus.valueOf(row.getStatus()), row.getExpiresAt());
	}
}

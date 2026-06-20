package com.shop.inventory.internal;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Tenant mapper for locations, stock levels, and reservations. A plain {@code @Mapper}, so it runs
 * against the shard-routed tenant datasource (resolved from {@code TenantContext}); every statement still
 * filters by {@code shop_id} since a shard physically holds many shops.
 */
@Mapper
public interface InventoryMapper {

	void insertLocation(LocationRow row);

	LocationRow findLocation(@Param("shopId") long shopId, @Param("id") long id);

	StockLevelRow findStock(@Param("shopId") long shopId, @Param("variantId") long variantId,
			@Param("locationId") long locationId);

	void insertStock(StockLevelRow row);

	void updateStockQuantities(@Param("shopId") long shopId, @Param("variantId") long variantId,
			@Param("locationId") long locationId, @Param("available") long available,
			@Param("reserved") long reserved, @Param("updatedAt") Instant updatedAt);

	void insertReservation(ReservationRow row);

	ReservationRow findReservation(@Param("shopId") long shopId, @Param("id") long id);

	void updateReservationStatus(@Param("shopId") long shopId, @Param("id") long id,
			@Param("status") String status, @Param("updatedAt") Instant updatedAt);

	/** Held reservations whose deadline has passed — candidates for the expiry sweep. */
	List<ReservationRow> findDueReservations(@Param("shopId") long shopId, @Param("now") Instant now);
}

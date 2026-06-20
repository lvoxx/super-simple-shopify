package com.shop.inventory;

import com.shop.platform.core.ShopId;
import java.time.Instant;

/** Read model of a stock location — a record DTO, never the persistence row. */
public record LocationView(LocationId id, ShopId shopId, String name, Instant createdAt) {
}

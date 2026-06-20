package com.shop.inventory;

/**
 * Command to change the available quantity of a variant at a location by {@code delta} (positive to
 * receive stock, negative to remove). The stock level is created on first adjustment. Available stock may
 * not go negative — an over-large negative delta is rejected. {@code reason} is a free-text audit note.
 */
public record AdjustStockCommand(VariantRef variant, LocationId location, long delta, String reason) {
}

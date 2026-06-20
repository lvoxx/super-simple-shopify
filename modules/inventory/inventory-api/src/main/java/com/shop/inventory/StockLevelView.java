package com.shop.inventory;

/**
 * Read model of a variant's stock at one location. {@code available} is what may still be reserved or
 * sold; {@code reserved} is currently held by open reservations. On-hand is {@code available + reserved}.
 */
public record StockLevelView(VariantRef variant, LocationId location, long available, long reserved) {
}

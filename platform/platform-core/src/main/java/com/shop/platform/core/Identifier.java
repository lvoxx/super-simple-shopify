package com.shop.platform.core;

/**
 * Marker for typed identifier value objects. Keeps primitive ids (long/UUID) from leaking
 * across boundaries untyped — a {@code ShopId} can never be passed where an {@code OrderId}
 * is expected.
 */
public interface Identifier {
}

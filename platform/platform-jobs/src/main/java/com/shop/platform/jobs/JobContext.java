package com.shop.platform.jobs;

import com.shop.platform.core.ShopId;
import java.util.UUID;

/**
 * What a handler receives per delivery. {@code eventId} is the idempotency key; {@code attempt}
 * starts at 1 and increments on each retry; {@code shopId} re-establishes tenant context inside
 * the worker.
 */
public record JobContext(UUID eventId, ShopId shopId, String eventType, String payload, int attempt) {
}

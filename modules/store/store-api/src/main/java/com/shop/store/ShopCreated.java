package com.shop.store;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a new shop is provisioned. Published to the control-plane outbox in the same transaction
 * as the shop + shard-assignment writes, then drained by the job-engine. Downstream modules react to
 * this to bootstrap their per-shop state (default catalog, settings, etc.) in later phases.
 *
 * <p>Lives in the module's exposed base package (not {@code internal}) so other modules may consume it.
 */
public record ShopCreated(UUID eventId, Instant occurredAt, ShopId shopId, String name, int shardIndex)
		implements DomainEvent {
}

package com.shop.hello;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.UUID;

/** Raised whenever the hello endpoint is hit; drained from the outbox by the job-engine. */
public record HelloRequestedEvent(UUID eventId, Instant occurredAt, ShopId shopId, String message)
		implements DomainEvent {
}

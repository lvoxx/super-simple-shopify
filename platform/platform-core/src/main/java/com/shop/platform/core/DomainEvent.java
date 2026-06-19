package com.shop.platform.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract every domain event implements. It cannot be a {@code sealed} type in the
 * literal sense — concrete events are records declared inside each domain module's {@code *-api},
 * and a sealed permits-list cannot span modules. The interface IS the seal: nothing crosses a
 * module boundary as an event unless it is a {@code DomainEvent}, every event is tenant-scoped
 * ({@link #shopId()}), and the {@link #eventId()} is the idempotency key job handlers de-dupe on.
 */
public interface DomainEvent {

	UUID eventId();

	Instant occurredAt();

	ShopId shopId();
}

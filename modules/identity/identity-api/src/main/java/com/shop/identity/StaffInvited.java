package com.shop.identity;

import com.shop.platform.core.DomainEvent;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Raised when a staff user is added to a shop. Published to the tenant outbox in the same transaction
 * as the {@code staff_user} write, then drained idempotently by the job-engine. Downstream modules
 * (e.g. notifications, in a later phase) react to this to send the invite email — never inline here.
 *
 * <p>Lives in the module's exposed base package (not {@code internal}) so other modules may consume it.
 */
public record StaffInvited(UUID eventId, Instant occurredAt, ShopId shopId, String subject,
		Set<StaffRole> roles) implements DomainEvent {

	public StaffInvited {
		roles = Set.copyOf(roles);
	}
}

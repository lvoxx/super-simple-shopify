package com.shop.identity;

import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.Set;

/**
 * Read model of a staff user returned across the module boundary — a record DTO, never the persistence
 * row. {@code subject} is the caller's Keycloak {@code sub} claim; {@code roles} drives RBAC decisions.
 */
public record StaffUserView(ShopId shopId, String subject, String displayName, String email,
		StaffStatus status, Set<StaffRole> roles, Instant createdAt) {

	public StaffUserView {
		roles = Set.copyOf(roles);
	}
}

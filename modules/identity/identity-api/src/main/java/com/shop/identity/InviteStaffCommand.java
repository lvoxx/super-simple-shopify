package com.shop.identity;

import java.util.Set;

/**
 * Command to add a staff user to the current shop, mapping a Keycloak {@code subject} to a per-shop
 * identity with one or more {@link StaffRole roles}. The target shop is the active tenant
 * ({@code TenantContext}), never a field on the command — staff are tenant-scoped.
 */
public record InviteStaffCommand(String subject, String displayName, String email, Set<StaffRole> roles) {

	public InviteStaffCommand {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}
}

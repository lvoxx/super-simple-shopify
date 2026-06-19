package com.shop.platform.security;

import com.shop.platform.core.ShopId;
import java.util.Set;

/**
 * The authenticated caller, always tenant-scoped. {@code subject} is the staff user / token id;
 * {@code roles} drives RBAC checks.
 */
public record Principal(ShopId shopId, String subject, Set<Role> roles) {

	public Principal {
		roles = Set.copyOf(roles);
	}

	public boolean hasRole(Role role) {
		return roles.contains(role);
	}
}

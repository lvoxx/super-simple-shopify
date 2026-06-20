package com.shop.identity;

/**
 * The RBAC roles a staff user can hold within a shop. Intentionally mirrors the coarse role set in
 * {@code platform-security} ({@code Role}); an {@code *-api} module may depend only on
 * {@code platform-core}, so the two enums are reconciled in the admin-auth wiring slice (mapping
 * {@code StaffRole} onto the security {@code Principal}'s roles), not by a cross-module dependency here.
 */
public enum StaffRole {
	OWNER,
	ADMIN,
	STAFF,
	READ_ONLY
}

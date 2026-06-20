package com.shop.identity;

/**
 * Lifecycle of a staff user within a shop. A newly added user starts {@link #INVITED} (a Keycloak
 * subject has been granted access but has not yet signed in); {@link #ACTIVE} once they have; and
 * {@link #SUSPENDED} when access is revoked without deleting the record.
 */
public enum StaffStatus {
	INVITED,
	ACTIVE,
	SUSPENDED
}

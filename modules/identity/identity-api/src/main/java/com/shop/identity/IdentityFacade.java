package com.shop.identity;

import java.util.Optional;

/**
 * The identity module's public, synchronous API. The admin surface and other modules call this to add
 * staff and to resolve a Keycloak subject to its per-shop roles for authorization; nobody reaches into
 * the module's persistence. All operations are <strong>tenant-scoped</strong> — they act on the active
 * {@code TenantContext} shop and fail hard if none is bound.
 */
public interface IdentityFacade {

	/** Add a staff user to the current shop and emit {@link StaffInvited}. */
	StaffUserView invite(InviteStaffCommand command);

	/** Resolve a Keycloak subject to its staff user in the current shop, or empty if none. */
	Optional<StaffUserView> findBySubject(String subject);
}

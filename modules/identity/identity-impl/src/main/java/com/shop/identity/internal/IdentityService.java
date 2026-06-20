package com.shop.identity.internal;

import com.shop.identity.IdentityFacade;
import com.shop.identity.InviteStaffCommand;
import com.shop.identity.StaffInvited;
import com.shop.identity.StaffUserView;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.core.TimeProvider;
import com.shop.platform.events.DomainEventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The identity module's implementation of {@link IdentityFacade}. Every operation is tenant-scoped: the
 * shop comes from {@link TenantContext#requireShop()} (a hard failure if unbound), never from the
 * caller. Inviting a staff user is one tenant transaction — insert the user, insert its role rows, and
 * publish {@link StaffInvited} to the tenant outbox — so the event is durable iff the write commits.
 *
 * <p>Authentication is delegated to Keycloak at the gateway; this service manages the authorization
 * model (who exists and what roles they hold), not credentials or tokens.
 */
@Service
public class IdentityService implements IdentityFacade {

	private final StaffUserMapper staff;
	private final StaffAssembler assembler;
	private final DomainEventPublisher events;
	private final TimeProvider time;

	public IdentityService(StaffUserMapper staff, StaffAssembler assembler, DomainEventPublisher events,
			TimeProvider time) {
		this.staff = staff;
		this.assembler = assembler;
		this.events = events;
		this.time = time;
	}

	@Override
	@Transactional
	public StaffUserView invite(InviteStaffCommand command) {
		ShopId shopId = TenantContext.requireShop();
		if (staff.findBySubject(shopId.value(), command.subject()) != null) {
			throw new DomainException("identity.staff.already_exists", ProblemCategory.CONFLICT,
					"Staff subject already exists in shop: " + command.subject(), command.subject());
		}
		var now = time.now();
		var row = assembler.toNewRow(shopId, command, now);
		staff.insert(row);
		for (String role : row.getRoles()) {
			staff.insertRole(shopId.value(), row.getSubject(), role);
		}
		events.publish(new StaffInvited(UUID.randomUUID(), now, shopId, row.getSubject(), command.roles()));
		return assembler.toView(row);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StaffUserView> findBySubject(String subject) {
		ShopId shopId = TenantContext.requireShop();
		return Optional.ofNullable(staff.findBySubject(shopId.value(), subject)).map(assembler::toView);
	}
}

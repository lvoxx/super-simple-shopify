package com.shop.identity.internal;

import com.shop.identity.InviteStaffCommand;
import com.shop.identity.StaffRole;
import com.shop.identity.StaffStatus;
import com.shop.identity.StaffUserView;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Centralizes the identity module's row&harr;DTO mapping. As in the store slice, ModelMapper is the
 * project default but the edges here cross record + enum boundaries (the API uses records with
 * {@code StaffRole}/{@code StaffStatus} enums while rows store their names), where explicit construction
 * is clearer. Mapping stays here, never inlined into the controller or persistence.
 */
@Component
public class StaffAssembler {

	/** Build a new staff row from an invite command, defaulting status to {@link StaffStatus#INVITED}. */
	public StaffUserRow toNewRow(ShopId shopId, InviteStaffCommand command, Instant now) {
		var row = new StaffUserRow();
		row.setShopId(shopId.value());
		row.setSubject(command.subject());
		row.setDisplayName(command.displayName());
		row.setEmail(command.email());
		row.setStatus(StaffStatus.INVITED.name());
		row.setRoles(command.roles().stream().map(StaffRole::name).sorted().toList());
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		return row;
	}

	public StaffUserView toView(StaffUserRow row) {
		return new StaffUserView(
				ShopId.of(row.getShopId()),
				row.getSubject(),
				row.getDisplayName(),
				row.getEmail(),
				StaffStatus.valueOf(row.getStatus()),
				row.getRoles().stream().map(StaffRole::valueOf).collect(Collectors.toUnmodifiableSet()),
				row.getCreatedAt());
	}
}

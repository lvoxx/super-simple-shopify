package com.shop.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.identity.InviteStaffCommand;
import com.shop.identity.StaffRole;
import com.shop.identity.StaffStatus;
import com.shop.identity.StaffUserView;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for the identity row&harr;DTO mapping — passes with only identity-api + platform-core
 * on the classpath (no Spring, no other module impl), satisfying the module-slice isolation rule.
 */
class StaffAssemblerTest {

	private final StaffAssembler assembler = new StaffAssembler();

	@Test
	void newRowDefaultsStatusToInvitedAndStoresRoleNames() {
		var now = Instant.parse("2026-06-20T10:15:30Z");

		StaffUserRow row = assembler.toNewRow(ShopId.of(7),
				new InviteStaffCommand("kc-sub-1", "Ada", "ada@acme.example",
						Set.of(StaffRole.OWNER, StaffRole.ADMIN)),
				now);

		assertThat(row.getShopId()).isEqualTo(7L);
		assertThat(row.getSubject()).isEqualTo("kc-sub-1");
		assertThat(row.getStatus()).isEqualTo(StaffStatus.INVITED.name());
		assertThat(row.getRoles()).containsExactly(StaffRole.ADMIN.name(), StaffRole.OWNER.name());
		assertThat(row.getCreatedAt()).isEqualTo(now);
		assertThat(row.getUpdatedAt()).isEqualTo(now);
	}

	@Test
	void viewRoundTripsEnumsAndRoles() {
		var row = new StaffUserRow();
		row.setShopId(42L);
		row.setSubject("kc-sub-2");
		row.setDisplayName("Grace");
		row.setEmail("grace@acme.example");
		row.setStatus(StaffStatus.ACTIVE.name());
		row.setRoles(java.util.List.of(StaffRole.STAFF.name(), StaffRole.READ_ONLY.name()));
		row.setCreatedAt(Instant.parse("2026-06-20T10:15:30Z"));

		StaffUserView view = assembler.toView(row);

		assertThat(view.shopId().value()).isEqualTo(42L);
		assertThat(view.subject()).isEqualTo("kc-sub-2");
		assertThat(view.status()).isEqualTo(StaffStatus.ACTIVE);
		assertThat(view.roles()).containsExactlyInAnyOrder(StaffRole.STAFF, StaffRole.READ_ONLY);
	}
}

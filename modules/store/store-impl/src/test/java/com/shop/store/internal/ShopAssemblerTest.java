package com.shop.store.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.store.CreateShopCommand;
import com.shop.store.ShopPlan;
import com.shop.store.ShopStatus;
import com.shop.store.ShopView;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for the store row&harr;DTO mapping — passes with only store-api + platform-core on
 * the classpath (no Spring, no other module impl), satisfying the module-slice isolation rule.
 */
class ShopAssemblerTest {

	private final ShopAssembler assembler = new ShopAssembler();

	@Test
	void newRowDefaultsPlanToFreeAndStatusToActive() {
		var now = Instant.parse("2026-06-20T10:15:30Z");

		ShopRow row = assembler.toNewRow(new CreateShopCommand("Acme", null, "en-US", "acme.example"), now);

		assertThat(row.getPlan()).isEqualTo(ShopPlan.FREE.name());
		assertThat(row.getStatus()).isEqualTo(ShopStatus.ACTIVE.name());
		assertThat(row.getName()).isEqualTo("Acme");
		assertThat(row.getPrimaryDomain()).isEqualTo("acme.example");
		assertThat(row.getCreatedAt()).isEqualTo(now);
		assertThat(row.getUpdatedAt()).isEqualTo(now);
	}

	@Test
	void viewRoundTripsEnumsAndShard() {
		var row = new ShopRow();
		row.setId(42L);
		row.setName("Acme");
		row.setPlan(ShopPlan.PRO.name());
		row.setStatus(ShopStatus.ACTIVE.name());
		row.setLocale("en-US");
		row.setPrimaryDomain("acme.example");
		row.setCreatedAt(Instant.parse("2026-06-20T10:15:30Z"));

		ShopView view = assembler.toView(row, 3);

		assertThat(view.id().value()).isEqualTo(42L);
		assertThat(view.plan()).isEqualTo(ShopPlan.PRO);
		assertThat(view.status()).isEqualTo(ShopStatus.ACTIVE);
		assertThat(view.shardIndex()).isEqualTo(3);
	}
}

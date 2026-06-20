package com.shop.store.internal;

import com.shop.platform.core.ShopId;
import com.shop.store.CreateShopCommand;
import com.shop.store.ShopPlan;
import com.shop.store.ShopStatus;
import com.shop.store.ShopView;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Centralizes the store module's row&harr;DTO mapping. ModelMapper is the project default for
 * DTO&harr;domain mapping, but the store edges cross record + enum boundaries (the API uses records
 * with {@code ShopPlan}/{@code ShopStatus} enums while the row stores their names), where explicit,
 * STRICT-safe construction is clearer than coaxing ModelMapper — matching the response-DTO precedent
 * in the Phase 0 hello slice. Mapping stays here, never inlined into the controller or persistence.
 */
@Component
public class ShopAssembler {

	/** Build a new registry row from a create command, defaulting the plan to {@link ShopPlan#FREE}. */
	public ShopRow toNewRow(CreateShopCommand command, Instant now) {
		var row = new ShopRow();
		row.setName(command.name());
		row.setPlan((command.plan() != null ? command.plan() : ShopPlan.FREE).name());
		row.setStatus(ShopStatus.ACTIVE.name());
		row.setLocale(command.locale());
		row.setPrimaryDomain(command.primaryDomain());
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		return row;
	}

	public ShopView toView(ShopRow row, int shardIndex) {
		return new ShopView(
				ShopId.of(row.getId()),
				row.getName(),
				ShopPlan.valueOf(row.getPlan()),
				ShopStatus.valueOf(row.getStatus()),
				row.getLocale(),
				row.getPrimaryDomain(),
				shardIndex,
				row.getCreatedAt());
	}
}

package com.shop.platform.observability;

import com.shop.platform.core.ShopId;
import org.slf4j.MDC;

/**
 * Pushes the tenant + module onto SLF4J's MDC so every log line emitted during the work carries
 * {@code shop_id} and {@code module}. Always paired with {@link #clear()} in a finally block.
 */
public final class TenantLoggingContext {

	private TenantLoggingContext() {
	}

	public static void bind(String module, ShopId shopId) {
		MDC.put(ObservabilityTags.MODULE, module);
		MDC.put(ObservabilityTags.SHOP_ID, shopId.toString());
	}

	public static void clear() {
		MDC.remove(ObservabilityTags.MODULE);
		MDC.remove(ObservabilityTags.SHOP_ID);
	}
}

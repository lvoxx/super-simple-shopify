package com.shop.platform.observability;

/**
 * Canonical tag/MDC keys every metric, trace, and log line is enriched with. Phase 9 wires the
 * OTel exporters + JSON log encoder that consume these; the keys are fixed here so dashboards
 * and queries never have to guess the spelling.
 */
public final class ObservabilityTags {

	/** The owning bounded context, e.g. {@code catalog}, {@code orders}. */
	public static final String MODULE = "module";

	/** The tenant the work is scoped to. */
	public static final String SHOP_ID = "shop_id";

	private ObservabilityTags() {
	}
}

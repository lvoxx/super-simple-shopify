package com.shop.platform.core;

/**
 * Thrown when tenant-scoped work runs without a bound {@link ShopId}. There is no silent
 * fallback to a "default" tenant — a missing tenant is always a hard failure.
 */
public final class TenantContextMissingException extends DomainException {

	public TenantContextMissingException() {
		super("tenancy.context.missing", ProblemCategory.TENANCY,
				"No ShopId bound to the current scope; tenant context is mandatory");
	}
}

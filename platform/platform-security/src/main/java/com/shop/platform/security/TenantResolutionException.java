package com.shop.platform.security;

import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;

/** Raised when a request carries no resolvable tenant. Never falls back to a default shop. */
public final class TenantResolutionException extends DomainException {

	public TenantResolutionException(String developerMessage) {
		super("tenancy.unresolved", ProblemCategory.TENANCY, developerMessage);
	}
}

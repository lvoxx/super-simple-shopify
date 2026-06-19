package com.shop.platform.web;

/**
 * API versioning scheme. v1 is frozen in Phase 10 before any frontend work; breaking changes
 * after that require a new version, not an edit to v1.
 */
public final class ApiVersion {

	public static final String V1 = "v1";

	/** Path prefix convention, e.g. {@code /api/v1/...}. */
	public static final String V1_PREFIX = "/api/" + V1;

	private ApiVersion() {
	}
}

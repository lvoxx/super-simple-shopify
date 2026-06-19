package com.shop.platform.core;

/**
 * Transport-agnostic classification of a failure. platform-core stays free of any HTTP type;
 * platform-web maps each category onto an RFC 9457 {@code ProblemDetail} status. The numeric
 * hint is the canonical HTTP status, kept here only so the mapping has one source of truth.
 */
public enum ProblemCategory {

	VALIDATION(400),
	UNAUTHENTICATED(401),
	FORBIDDEN(403),
	NOT_FOUND(404),
	CONFLICT(409),
	TENANCY(400),
	RATE_LIMITED(429),
	INTERNAL(500);

	private final int canonicalStatus;

	ProblemCategory(int canonicalStatus) {
		this.canonicalStatus = canonicalStatus;
	}

	public int canonicalStatus() {
		return canonicalStatus;
	}
}

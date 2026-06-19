package com.shop.platform.security;

/** Coarse RBAC roles for staff principals. Fine-grained scopes are layered on in Phase 1. */
public enum Role {
	OWNER,
	ADMIN,
	STAFF,
	READ_ONLY
}

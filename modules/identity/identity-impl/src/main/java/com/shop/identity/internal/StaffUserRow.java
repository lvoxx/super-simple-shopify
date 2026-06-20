package com.shop.identity.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One staff user of the tenant-sharded {@code staff_user} table, with its {@code staff_user_role} rows
 * collected into {@link #roles} — a mutable MyBatis POJO, internal to the identity module (never crosses
 * a boundary; {@link com.shop.identity.StaffUserView} does). {@code status} and each role are stored as
 * their enum names. {@code shopId} is the tenant key, mandatory on every tenant row.
 */
public class StaffUserRow {

	private Long shopId;
	private String subject;
	private String displayName;
	private String email;
	private String status;
	private List<String> roles = new ArrayList<>();
	private Instant createdAt;
	private Instant updatedAt;

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}

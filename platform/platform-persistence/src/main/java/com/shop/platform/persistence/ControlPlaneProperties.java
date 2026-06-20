package com.shop.platform.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code platform.persistence.control.*} — connection settings for the global control plane
 * (shop registry, shop&rarr;shard map, control outbox). The control plane is a single,
 * non-sharded datastore.
 *
 * <p>When {@code url} is blank (local/dev and the Phase 0/Testcontainers single-node setup) the
 * control plane shares the same physical Postgres as shard 0 — the {@link ControlPlaneConfig}
 * falls back to the ambient {@code JdbcConnectionDetails}. Production points this at a dedicated
 * control database.
 */
@ConfigurationProperties(prefix = "platform.persistence.control")
public class ControlPlaneProperties {

	private String url;
	private String username;
	private String password;
	private String driverClassName;

	public boolean hasExplicitConnection() {
		return url != null && !url.isBlank();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}
}

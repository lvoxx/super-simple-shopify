package com.shop.platform.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Throwaway Postgres + Redis backed by official images via Testcontainers. {@code @ServiceConnection}
 * wires the datasource/Redis connection details automatically — tests never hard-code ports. Import
 * this into an integration test to get a real shard datastore. Requires Docker (present in CI).
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

	@Bean
	@ServiceConnection
	public PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	public GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
	}
}

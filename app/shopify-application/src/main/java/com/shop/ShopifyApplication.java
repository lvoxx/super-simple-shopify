package com.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The one and only deployable. Its package ({@code com.shop}) is the Spring Modulith base
 * package and the component/mapper scan root, so it assembles every platform module, the
 * job-engine runtime, and (from Phase 1) the domain modules under {@code com.shop.<module>}.
 *
 * <p>The app NEVER runs database migrations — Flyway is a test-only dependency here and schema
 * is applied by the infra migration container.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopifyApplication.class, args);
	}
}

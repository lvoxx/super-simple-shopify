package com.shop;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith boundary verification — runs on every build and fails a PR that breaks a
 * module boundary. The {@code platform.*} and {@code jobs.*} packages are technical shared
 * infrastructure, not bounded contexts, so they are excluded from module analysis; the domain
 * modules added in Phase 1+ (under {@code com.shop.<module>}) are what this guards.
 */
class ModularityTests {

	static final ApplicationModules modules = ApplicationModules.of(ShopifyApplication.class,
			DescribedPredicate.describe("platform + jobs technical infrastructure",
					(JavaClass type) -> type.getPackageName().startsWith("com.shop.platform")
							|| type.getPackageName().startsWith("com.shop.jobs")));

	@Test
	void verifiesModuleBoundaries() {
		modules.verify();
	}

	@Test
	void rendersModuleDocumentation() {
		new Documenter(modules).writeDocumentation();
	}
}

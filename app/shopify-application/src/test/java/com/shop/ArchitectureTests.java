package com.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Static architecture rules that back up the prose in CLAUDE.md / ROADMAP. More rules are added
 * as domain modules land; the Phase 0 invariant is that platform-core is a pure-Java contract
 * layer with no framework coupling.
 */
class ArchitectureTests {

	static final JavaClasses classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.shop");

	@Test
	void platformCoreStaysPureJava() {
		ArchRule rule = noClasses()
				.that().resideInAPackage("com.shop.platform.core..")
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.springframework..", "org.apache.ibatis..", "jakarta.servlet..");
		rule.check(classes);
	}

	@Test
	void storeApiStaysPersistenceFree() {
		ArchRule rule = noClasses()
				.that().resideInAPackage("com.shop.store")
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.apache.ibatis..", "com.shop.platform.persistence..",
						"com.shop.store.internal..");
		rule.check(classes);
	}

	@Test
	void identityApiStaysPersistenceFree() {
		ArchRule rule = noClasses()
				.that().resideInAPackage("com.shop.identity")
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.apache.ibatis..", "com.shop.platform.persistence..",
						"com.shop.identity.internal..");
		rule.check(classes);
	}

	@Test
	void catalogApiStaysPersistenceFree() {
		ArchRule rule = noClasses()
				.that().resideInAPackage("com.shop.catalog")
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.apache.ibatis..", "com.shop.platform.persistence..",
						"com.shop.catalog.internal..");
		rule.check(classes);
	}

	@Test
	void inventoryApiStaysPersistenceFree() {
		ArchRule rule = noClasses()
				.that().resideInAPackage("com.shop.inventory")
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.apache.ibatis..", "com.shop.platform.persistence..",
						"com.shop.inventory.internal..");
		rule.check(classes);
	}
}

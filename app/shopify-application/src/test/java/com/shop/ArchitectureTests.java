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
}

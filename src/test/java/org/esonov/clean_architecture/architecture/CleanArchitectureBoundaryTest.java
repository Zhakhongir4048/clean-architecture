package org.esonov.clean_architecture.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces the Dependency Rule for every feature package (Screaming Architecture):
 * <pre>
 *   org.esonov.clean_architecture
 *   ├── config                 shared infrastructure (Main)
 *   ├── shared/{domain, adapter}   shared kernel
 *   └── &lt;feature&gt;/{domain, application, adapter, config}
 *
 *   inside a feature:  config (Main) ──► adapter ──► application ──► domain
 * </pre>
 * Rules are written with package wildcards, so a new feature is covered without editing this class.
 * See src/ARCHITECTURE.md for the rationale.
 */
@AnalyzeClasses(packages = "org.esonov.clean_architecture", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureBoundaryTest {

    private static final String ROOT = "org.esonov.clean_architecture";
    private static final String SHARED = ROOT + ".shared..";
    private static final String CONFIG = ROOT + ".config..";

    private static final String DOMAIN = ROOT + ".*.domain..";
    private static final String APPLICATION = ROOT + ".*.application..";
    private static final String PORT_IN = ROOT + ".*.application.port.in..";
    private static final String PORT_OUT = ROOT + ".*.application.port.out..";
    private static final String USECASE = ROOT + ".*.application.usecase..";
    private static final String ADAPTER = ROOT + ".*.adapter..";
    private static final String WEB_ADAPTER = ROOT + ".*.adapter.in.web..";
    private static final String PERSISTENCE_ADAPTER = ROOT + ".*.adapter.out.persistence..";
    private static final String FEATURE_CONFIG = ROOT + ".*.config..";

    /** Feature entities and value objects; the shared kernel (e.g. DomainValidationException) is not an entity. */
    private static final DescribedPredicate<JavaClass> ENTITIES =
            resideInAPackage(DOMAIN).and(not(resideInAPackage(SHARED))).as("entities");

    // ---- Dependency Rule ---------------------------------------------------------------------

    @ArchTest
    static final ArchRule dependency_rule = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Adapters").definedBy(ADAPTER)
            .layer("Main").definedBy(ROOT, CONFIG, FEATURE_CONFIG)
            .whereLayer("Main").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Main")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Main")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters");

    @ArchTest
    static final ArchRule inner_layers_are_framework_free = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
                    "org.hibernate..", "tools.jackson..", "com.fasterxml.jackson..", "lombok..")
            .because("business rules must not depend on frameworks (Chapter 32: Frameworks Are Details)");

    // ---- Boundary crossings ------------------------------------------------------------------

    @ArchTest
    static final ArchRule web_adapter_talks_only_to_input_ports = noClasses()
            .that().resideInAPackage(WEB_ADAPTER)
            .should().dependOnClassesThat().resideInAnyPackage(USECASE, PORT_OUT, PERSISTENCE_ADAPTER)
            .because("controllers cross the boundary only through input ports");

    @ArchTest
    static final ArchRule web_adapter_does_not_touch_entities = noClasses()
            .that().resideInAPackage(WEB_ADAPTER)
            .should().dependOnClassesThat(ENTITIES)
            .because("entities must not leak out through the input boundary; use response models");

    @ArchTest
    static final ArchRule input_ports_do_not_expose_entities = noClasses()
            .that().resideInAPackage(PORT_IN)
            .should().dependOnClassesThat(ENTITIES)
            .because("CRP: callers of input ports must not depend on entities, even transitively");

    @ArchTest
    static final ArchRule persistence_adapter_does_not_use_input_side = noClasses()
            .that().resideInAPackage(PERSISTENCE_ADAPTER)
            .should().dependOnClassesThat().resideInAnyPackage(PORT_IN, USECASE, WEB_ADAPTER);

    @ArchTest
    static final ArchRule ports_are_interfaces_or_data = classes()
            .that().resideInAnyPackage(PORT_IN, PORT_OUT)
            .should().beInterfaces().orShould().beRecords()
            .because("boundary crossings are interfaces plus plain data structures");

    @ArchTest
    static final ArchRule interactors_implement_input_ports = classes()
            .that().resideInAPackage(USECASE)
            .and().haveSimpleNameEndingWith("Interactor")
            .should().bePublic()
            .andShould().implement(resideInAPackage(PORT_IN).as("an input port"));

    @ArchTest
    static final ArchRule adapters_are_hidden = classes()
            .that().resideInAPackage(ADAPTER)
            .and().areNotRecords()
            .should().notBePublic()
            .because("adapters are details; only Spring's wiring should reach them");

    // ---- Features (Screaming Architecture) -------------------------------------------------

    @ArchTest
    static final ArchRule features_are_independent = slices()
            .matching(ROOT + ".(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAnyPackage(SHARED, CONFIG))
            .ignoreDependency(resideInAPackage(ROOT), alwaysTrue())
            // The one sanctioned crossing: an adapter of feature A calls an input port of feature B.
            .ignoreDependency(resideInAPackage(ADAPTER), resideInAPackage(PORT_IN))
            .because("features talk to each other only through another feature's input ports, called from an adapter");

    @ArchTest
    static final ArchRule shared_kernel_does_not_know_features = noClasses()
            .that().resideInAPackage(SHARED)
            .should().dependOnClassesThat(resideInAPackage(ROOT + "..").and(not(resideInAPackage(SHARED))))
            .because("the shared kernel sits below every feature");

    @ArchTest
    static final ArchRule no_package_cycles = slices()
            .matching(ROOT + ".(**)")
            .should().beFreeOfCycles();
}

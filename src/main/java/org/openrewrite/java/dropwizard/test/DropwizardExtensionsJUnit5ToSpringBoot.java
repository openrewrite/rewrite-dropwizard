/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.dropwizard.test;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

import static java.util.Comparator.comparing;
import static org.openrewrite.java.dropwizard.test.AnnotationUtils.getSimpleName;
import static org.openrewrite.java.dropwizard.test.AnnotationUtils.getSimpleNameWithParams;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

/**
 * Converts JUnit 5 Dropwizard test extensions to Spring Boot test annotations.
 * Handles {@code DropwizardAppExtension}, {@code DropwizardExtensionsSupport},
 * {@code ResourceExtension}, and {@code DropwizardClientExtension}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class DropwizardExtensionsJUnit5ToSpringBoot extends Recipe {

    private static final String DW_APP_EXTENSION = "io.dropwizard.testing.junit5.DropwizardAppExtension";
    private static final String DW_EXTENSIONS_SUPPORT = "io.dropwizard.testing.junit5.DropwizardExtensionsSupport";
    private static final String DW_RESOURCE_EXTENSION = "io.dropwizard.testing.junit5.ResourceExtension";
    private static final String DW_CLIENT_EXTENSION = "io.dropwizard.testing.junit5.DropwizardClientExtension";
    private static final String DW_DAO_EXTENSION = "io.dropwizard.testing.junit5.DAOTestExtension";

    private static final String SPRING_BOOT_TEST =
            "org.springframework.boot.test.context.SpringBootTest";
    private static final String SPRING_BOOT_TEST_WITH_PORT =
            "org.springframework.boot.test.context.SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)";
    private static final String DATA_JPA_TEST =
            "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest";
    private static final String EXTEND_WITH = "org.junit.jupiter.api.extension.ExtendWith";

    String displayName = "Replace Dropwizard JUnit 5 extensions with Spring Boot test configuration";

    String description = "Removes Dropwizard JUnit 5 test extensions (DropwizardAppExtension, " +
            "DropwizardExtensionsSupport, etc.) and adds Spring Boot test annotations.";

    private static Set<String> determineAnnotations(Set<String> foundExtensions) {
        Set<String> set = new HashSet<>();
        if (foundExtensions.contains(DW_DAO_EXTENSION)) {
            set.add(DATA_JPA_TEST);
        } else {
            set.add(SPRING_BOOT_TEST_WITH_PORT);
        }
        return set;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        Preconditions.or(
                                new UsesType<>(DW_APP_EXTENSION, true),
                                new UsesType<>(DW_EXTENSIONS_SUPPORT, true),
                                new UsesType<>(DW_RESOURCE_EXTENSION, true),
                                new UsesType<>(DW_CLIENT_EXTENSION, true),
                                new UsesType<>(DW_DAO_EXTENSION, true)),
                        Preconditions.and(
                                Preconditions.not(new UsesType<>(SPRING_BOOT_TEST, true)),
                                Preconditions.not(new UsesType<>(DATA_JPA_TEST, true)))),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.@Nullable VariableDeclarations visitVariableDeclarations(
                            J.VariableDeclarations vd, ExecutionContext ctx) {
                        vd = super.visitVariableDeclarations(vd, ctx);

                        Set<String> foundExtensions = findDropwizardExtensions(vd);
                        if (foundExtensions.isEmpty()) {
                            return vd;
                        }

                        maybeRemoveImport(DW_APP_EXTENSION);
                        maybeRemoveImport(DW_EXTENSIONS_SUPPORT);
                        maybeRemoveImport(DW_RESOURCE_EXTENSION);
                        maybeRemoveImport(DW_CLIENT_EXTENSION);
                        maybeRemoveImport(DW_DAO_EXTENSION);
                        maybeRemoveImport("io.dropwizard.testing.ConfigOverride");
                        maybeRemoveImport("io.dropwizard.testing.ResourceHelpers");

                        doAfterVisit(new AddSpringBootTestAndRemoveExtendWith(foundExtensions));

                        return null;
                    }

                    private Set<String> findDropwizardExtensions(J.VariableDeclarations vd) {
                        if (vd.getTypeAsFullyQualified() == null) {
                            return emptySet();
                        }

                        Set<String> found = new HashSet<>();
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DW_APP_EXTENSION)) {
                            found.add(DW_APP_EXTENSION);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DW_RESOURCE_EXTENSION)) {
                            found.add(DW_RESOURCE_EXTENSION);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DW_CLIENT_EXTENSION)) {
                            found.add(DW_CLIENT_EXTENSION);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DW_DAO_EXTENSION)) {
                            found.add(DW_DAO_EXTENSION);
                        }
                        return found;
                    }
                });
    }

    /**
     * Adds Spring Boot test annotations and removes @ExtendWith(DropwizardExtensionsSupport.class).
     */
    public static class AddSpringBootTestAndRemoveExtendWith extends JavaIsoVisitor<ExecutionContext> {

        private final Set<String> foundExtensions;

        public AddSpringBootTestAndRemoveExtendWith(Set<String> foundExtensions) {
            this.foundExtensions = foundExtensions;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

            // Skip if already has Spring Boot test annotations
            boolean hasSpringAnnotation =
                    hasAnnotation(cd, SPRING_BOOT_TEST) || hasAnnotation(cd, DATA_JPA_TEST);
            if (hasSpringAnnotation) {
                return cd;
            }

            // Remove @ExtendWith(DropwizardExtensionsSupport.class)
            cd = removeExtendWithAnnotation(cd);

            // Add Spring Boot test annotations
            Set<String> annotationSet = determineAnnotations(foundExtensions);
            for (String fqn : annotationSet) {
                if (!hasAnnotation(cd, getSimpleName(fqn))) {
                    maybeAddImport(SPRING_BOOT_TEST);
                    JavaTemplate template =
                            JavaTemplate.builder("@" + getSimpleNameWithParams(fqn))
                                    .javaParser(JavaParser.fromJavaVersion()
                                            .classpathFromResources(ctx, "spring-boot-test-2.*",
                                                    "spring-boot-test-autoconfigure-2.*"))
                                    .imports(SPRING_BOOT_TEST, DATA_JPA_TEST)
                                    .build();
                    cd = template.apply(
                            updateCursor(cd),
                            cd.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                }
            }

            return cd;
        }

        private J.ClassDeclaration removeExtendWithAnnotation(J.ClassDeclaration cd) {
            return cd.withLeadingAnnotations(
                    cd.getLeadingAnnotations().stream()
                            .filter(ann -> {
                                if (!"ExtendWith".equals(ann.getSimpleName())) {
                                    return true;
                                }
                                // Check if it contains DropwizardExtensionsSupport
                                String printed = ann.printTrimmed(getCursor());
                                if (printed.contains("DropwizardExtensionsSupport")) {
                                    maybeRemoveImport(EXTEND_WITH);
                                    maybeRemoveImport(DW_EXTENSIONS_SUPPORT);
                                    return false;
                                }
                                return true;
                            })
                            .collect(Collectors.toList()));
        }

        private boolean hasAnnotation(J.ClassDeclaration cd, String name) {
            return cd.getLeadingAnnotations().stream()
                    .anyMatch(a -> a.getSimpleName().equals(getSimpleName(name)));
        }
    }
}

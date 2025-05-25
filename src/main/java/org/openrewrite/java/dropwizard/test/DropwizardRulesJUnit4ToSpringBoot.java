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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Comparator.comparing;
import static org.openrewrite.java.dropwizard.test.AnnotationUtils.getSimpleName;
import static org.openrewrite.java.dropwizard.test.AnnotationUtils.getSimpleNameWithParams;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

// Inspired by https://docs.openrewrite.org/recipes/java/spring/test/springrulestojunitextension
@Value
@EqualsAndHashCode(callSuper = false)
public class DropwizardRulesJUnit4ToSpringBoot extends Recipe {

    private static final String DROPWIZARD_APP_RULE = "io.dropwizard.testing.junit.DropwizardAppRule";
    private static final String RESOURCE_TEST_RULE = "io.dropwizard.testing.junit.ResourceTestRule";
    private static final String DAO_TEST_RULE = "io.dropwizard.testing.junit.DAOTestRule";
    private static final String DROPWIZARD_CLIENT_RULE =
            "io.dropwizard.testing.junit.DropwizardClientRule";
    private static final String MOCKITO_TEST_RULE = "org.mockito.junit.MockitoJUnitRule";
    private static final String JUNIT_RULE = "org.junit.Rule";
    private static final String JUNIT_CLASS_RULE = "org.junit.ClassRule";
    private static final String SPRING_BOOT_TEST =
            "org.springframework.boot.test.context.SpringBootTest";
    private static final String SPRING_BOOT_TEST_WITH_PORT =
            "org.springframework.boot.test.context.SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)";
    private static final String DATA_JPA_TEST =
            "org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest";
    private static final String WEB_MVC_TEST =
            "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest";
    private static final String AUTO_CONFIGURE_MOCK_MVC =
            "org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc";

    /**
     * Decide which annotations to apply based on the found rules. Adjust this logic as needed.
     * We can't add MockMVC annotations as Spring Jersey does not support these.
     */
    private static Set<String> determineAnnotations(Set<String> foundRules) {
        Set<String> set = new HashSet<>();

        if (foundRules.contains(DAO_TEST_RULE)) {
            // Data-only tests
            set.add(DATA_JPA_TEST);
        } else {
            set.add(SPRING_BOOT_TEST_WITH_PORT);
        }

        return set;
    }

    @Override
    public String getDisplayName() {
        return "Replace Dropwizard rules with Spring Boot test configuration";
    }

    @Override
    public String getDescription() {
        return "Remove Dropwizard JUnit4 rules and add Spring Boot test annotations and extensions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        Preconditions.or(
                                new UsesType<>(DROPWIZARD_APP_RULE, true),
                                new UsesType<>(RESOURCE_TEST_RULE, true),
                                new UsesType<>(DAO_TEST_RULE, true),
                                new UsesType<>(DROPWIZARD_CLIENT_RULE, true),
                                new UsesType<>(MOCKITO_TEST_RULE, true)),
                        Preconditions.and(
                                Preconditions.not(new UsesType<>(SPRING_BOOT_TEST, true)),
                                Preconditions.not(new UsesType<>(DATA_JPA_TEST, true)),
                                Preconditions.not(new UsesType<>(AUTO_CONFIGURE_MOCK_MVC, true)),
                                Preconditions.not(new UsesType<>(WEB_MVC_TEST, true)))),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.@Nullable VariableDeclarations visitVariableDeclarations(
                            J.VariableDeclarations vd, ExecutionContext ctx) {
                        vd = super.visitVariableDeclarations(vd, ctx);

                        Set<String> foundRules = findDropwizardRules(vd);

                        if (foundRules.isEmpty()) {
                            return vd;
                        }

                        maybeRemoveImport(DROPWIZARD_APP_RULE);
                        maybeRemoveImport(RESOURCE_TEST_RULE);
                        maybeRemoveImport(DAO_TEST_RULE);
                        maybeRemoveImport(DROPWIZARD_CLIENT_RULE);
                        maybeRemoveImport(MOCKITO_TEST_RULE);
                        maybeRemoveImport(JUNIT_RULE);
                        maybeRemoveImport(JUNIT_CLASS_RULE);

                        doAfterVisit(new AddSpringBootTestAnnotationVisitor(foundRules));

                        return null;
                    }

                    private Set<String> findDropwizardRules(J.VariableDeclarations vd) {
                        if (vd.getTypeAsFullyQualified() == null) {
                            return Collections.emptySet();
                        }

                        Set<String> foundRules = new HashSet<>();

                        if (isOfClassType(vd.getTypeAsFullyQualified(), DROPWIZARD_APP_RULE)) {
                            foundRules.add(DROPWIZARD_APP_RULE);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), RESOURCE_TEST_RULE)) {
                            foundRules.add(RESOURCE_TEST_RULE);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DAO_TEST_RULE)) {
                            foundRules.add(DAO_TEST_RULE);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), DROPWIZARD_CLIENT_RULE)) {
                            foundRules.add(DROPWIZARD_CLIENT_RULE);
                        }
                        if (isOfClassType(vd.getTypeAsFullyQualified(), MOCKITO_TEST_RULE)) {
                            foundRules.add(MOCKITO_TEST_RULE);
                        }

                        return foundRules;
                    }
                });
    }

    public static class AddSpringBootTestAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Set<String> foundRules;

        public AddSpringBootTestAnnotationVisitor(Set<String> foundRules) {
            this.foundRules = foundRules;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

            // If we already have a Spring test annotation that sets the extension, no need to re-add
            boolean hasSpringExtension =
                    hasAnnotation(cd, SPRING_BOOT_TEST)
                            || hasAnnotation(cd, DATA_JPA_TEST)
                            || hasAnnotation(cd, WEB_MVC_TEST);

            if (hasSpringExtension) {
                return cd;
            }

            maybeAddImport(SPRING_BOOT_TEST);
            maybeAddImport(AUTO_CONFIGURE_MOCK_MVC);
            maybeAddImport(DATA_JPA_TEST);
            maybeAddImport(WEB_MVC_TEST);

            // Determine which annotations we need based on the rules found
            Set<String> annotationSet = determineAnnotations(foundRules);

            // Apply each annotation
            for (String fqn : annotationSet) {
                if (!hasAnnotation(cd, getSimpleName(fqn))) {
                    JavaTemplate template =
                            JavaTemplate.builder("@" + getSimpleNameWithParams(fqn))
                                    .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                                    .imports(SPRING_BOOT_TEST, AUTO_CONFIGURE_MOCK_MVC, DATA_JPA_TEST, WEB_MVC_TEST)
                                    .build();
                    cd =
                            template.apply(
                                    updateCursor(cd),
                                    cd.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                }
            }

            return cd;
        }

        private boolean hasAnnotation(J.ClassDeclaration cd, String fqn) {
            return cd.getLeadingAnnotations().stream()
                    .anyMatch(a -> a.getSimpleName().equals(getSimpleName(fqn)));
        }
    }
}

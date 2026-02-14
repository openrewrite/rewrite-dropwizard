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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class MockitoVariableToMockBean extends Recipe {


    @Override
    public String getDisplayName() {
        return "Convert Mockito mock() to @MockBean";
    }

    @Override
    public String getDescription() {
        return "Converts static final Mockito mock fields to Spring Boot @MockBean fields.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MockitoToMockBeanVisitor();
    }

    public static class MockitoToMockBeanVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final String ORG_MOCKITO_MOCKITO = "org.mockito.Mockito";
        private static final String MOCKITO_MOCK_BEAN =
                "org.springframework.boot.test.mock.mockito.MockBean";

        private final MethodMatcher mockMatcher = new MethodMatcher("org.mockito.Mockito mock(..)");

        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                J.VariableDeclarations varDecls, ExecutionContext ctx) {
            J.VariableDeclarations original = super.visitVariableDeclarations(varDecls, ctx);

            if (isStaticFinalMockitoMock(original)) {
                maybeRemoveImport(ORG_MOCKITO_MOCKITO);
                maybeAddImport(MOCKITO_MOCK_BEAN);

                // Remove static and final modifiers
                List<J.Modifier> newModifiers =
                        original.getModifiers().stream()
                                .filter(
                                        mod ->
                                                mod.getType() != J.Modifier.Type.Static &&
                                                        mod.getType() != J.Modifier.Type.Final)
                                .collect(Collectors.toList());

                // Remove the initializer
                List<J.VariableDeclarations.NamedVariable> variables =
                        original.getVariables().stream()
                                .map(v -> v.withInitializer(null))
                                .collect(Collectors.toList());

                // Build the new declaration
                J.VariableDeclarations modified = original
                        .withLeadingAnnotations(Collections.emptyList())
                        .withModifiers(newModifiers)
                        .withVariables(variables);

                // Add @MockBean using JavaTemplate
                JavaTemplate template = JavaTemplate.builder("@MockBean")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-boot-test-2.*"))
                        .imports(MOCKITO_MOCK_BEAN)
                        .build();
                modified = template.apply(
                        updateCursor(modified),
                        modified.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));

                return autoFormat(modified, ctx);
            }

            return original;
        }

        private boolean isStaticFinalMockitoMock(J.VariableDeclarations varDecls) {
            if (varDecls.getVariables().size() != 1) {
                return false;
            }

            boolean hasStaticAndFinal =
                    varDecls.getModifiers().stream()
                            .map(J.Modifier::getType)
                            .collect(Collectors.toSet())
                            .containsAll(Arrays.asList(J.Modifier.Type.Static, J.Modifier.Type.Final));

            J.VariableDeclarations.NamedVariable var = varDecls.getVariables().get(0);
            if (!(var.getInitializer() instanceof J.MethodInvocation)) {
                return false;
            }

            J.MethodInvocation init = (J.MethodInvocation) var.getInitializer();
            return hasStaticAndFinal && mockMatcher.matches(init);
        }
    }
}

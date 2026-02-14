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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class TransformDropwizardRuleInvocations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert dropwizard appRule to restTemplate";
    }

    @Override
    public String getDescription() {
        return "Transforms Dropwizard AppRule testing calls to their equivalent RestTemplate calls.";
    }

    private DropwizardToSpringCallBuilder getCallBuilder() {
        return new RestTemplateCallBuilder();
    }

    private List<MethodMatcher> getRestCallMatchers() {
        return Arrays.asList(
                new MethodMatcher("io.dropwizard.testing.junit.ResourceTestRule target(..)"),
                new MethodMatcher("io.dropwizard.testing.junit.DropwizardAppRule client(..)")
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            private boolean methodProcessed;

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodProcessed = false;
                J.VariableDeclarations varDecls =
                        (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);

                if (!isMethodScopedVariable() || !isValidInitializer(varDecls)) {
                    return varDecls;
                }

                // Check if the initializer is a matching Dropwizard call
                J.MethodInvocation mi = (J.MethodInvocation) varDecls.getVariables().get(0).getInitializer();
                if (!isRuleRESTCall(mi)) {
                    return varDecls;
                }

                methodProcessed = true;

                addImports();
                removeImports();

                return transformVariableDeclaration(ctx, varDecls, mi);
            }


            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                // If we already processed a relevant call or we are inside a variable decl, skip
                if (methodProcessed || getCursor().firstEnclosing(J.VariableDeclarations.class) != null) {
                    return method;
                }

                if (isRuleRESTCall(method)) {
                    methodProcessed = true;
                    removeImports();
                    addImports();

                    return transformMethodInvocation(ctx, method);
                }

                return method;
            }


            private void removeImports() {
                DropwizardToSpringCallBuilder builder = getCallBuilder();

                for (String requiredImport : builder.getImportsToRemove()) {
                    maybeRemoveImport(requiredImport);
                }
            }

            private void addImports() {
                DropwizardToSpringCallBuilder builder = getCallBuilder();

                for (String requiredImport : builder.getImports()) {
                    maybeAddImport(requiredImport);
                }
                for (String requiredStaticImport : builder.getStaticImports()) {
                    maybeAddImport(requiredStaticImport);
                }
            }

            private boolean isMethodScopedVariable() {
                return getCursor().firstEnclosing(J.MethodDeclaration.class) != null;
            }

            private boolean isValidInitializer(J.VariableDeclarations varDecls) {
                return !varDecls.getVariables().isEmpty() &&
                        varDecls.getVariables().get(0).getInitializer() instanceof J.MethodInvocation;
            }

            private boolean isRuleRESTCall(J.MethodInvocation method) {
                Expression curr = method;
                while (curr instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) curr;
                    for (MethodMatcher mm : getRestCallMatchers()) {
                        if (mm.matches(mi)) {
                            return true;
                        }
                    }
                    curr = mi.getSelect();
                }
                return false;
            }

            private J.MethodInvocation transformMethodInvocation(ExecutionContext ctx, J.MethodInvocation original) {
                JavaType returnType = (original.getMethodType() != null) ?
                        original.getMethodType().getReturnType() :
                        null;

                String snippet = getCallBuilder()
                        .buildMethod(DropwizardCallParser.parse(original, getCursor()), returnType);

                return JavaTemplate.builder(snippet)
                        .contextSensitive()
                        .imports(getCallBuilder().getImports())
                        .staticImports(getCallBuilder().getStaticImports())
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-5.*", "spring-core-5.*"))
                        .build()
                        .apply(updateCursor(original), original.getCoordinates().replace());
            }

            private J transformVariableDeclaration(ExecutionContext ctx, J.VariableDeclarations varDecls, J.MethodInvocation original) {
                JavaType varType = varDecls.getType();

                DropwizardCallParser.ParsedCall info = DropwizardCallParser.parse(original, getCursor());

                String variableInitializer = getCallBuilder().buildMethod(info, varType);
                String variableDeclaration = getCallBuilder().buildVariable(varDecls, varType);

                return JavaTemplate.builder(String.format("%s = %s", variableDeclaration, variableInitializer))
                        .contextSensitive()
                        .imports(getCallBuilder().getImports())
                        .staticImports(getCallBuilder().getStaticImports())
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-5.*", "spring-core-5.*"))
                        .build()
                        .apply(updateCursor(varDecls), varDecls.getCoordinates().replace());
            }
        };
    }

}

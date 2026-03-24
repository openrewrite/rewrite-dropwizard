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
package org.openrewrite.java.dropwizard.general;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.concurrent.atomic.AtomicBoolean;


@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveVariablesByPackage extends Recipe {

    @Option(displayName = "Package filter",
            description = "The package name to filter methods by. Methods with return types or parameter types in this package (or subpackages) will be removed.",
            example = "com.example.unwanted")
    String packageFilter;

    @Option(displayName = "Remove only class scope",
            description = "Ignores variables that are method scope",
            example = "true")
    Boolean removeOnlyClassScope;

    String displayName = "Remove class variables matching package filter";

    String description = "Removes class-level variables from classes in the specified package.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            final TypeMatcher targetTypeMatcher = new TypeMatcher(packageFilter + "..*", true);

            @Override
            public J.@Nullable VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);

                // Skip method parameters.
                if (isMethodParameter()) {
                    return vd;
                }

                // Optionally remove only class-scope variables.
                if (removeOnlyClassScope && !isClassScope()) {
                    return vd;
                }


                // check if variable or initializer matches
                if (!targetTypeMatcher.matches(vd.getType())) {
                    // check of initializer of field is of target type
                    Expression initializer = vd.getVariables().get(0).getInitializer();
                    if (!(initializer instanceof J.MethodInvocation) || ((J.MethodInvocation) initializer).getSelect() == null) {
                        return vd;
                    }

                    if (!targetTypeMatcher.matches(((J.MethodInvocation) initializer).getSelect().getType())) {
                        return vd;
                    } else {
                        // maybe remove import of removed initializer class
                        maybeRemoveImport(TypeUtils.asFullyQualified(((J.MethodInvocation) initializer).getSelect().getType()));
                    }
                }

                // Before removing, check if any declared variable has usages in scope
                for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                    if (hasUsagesInScope(var.getName())) {
                        return vd;
                    }
                }

                maybeRemoveImport(vd.getTypeAsFullyQualified());

                return null;
            }

            private boolean hasUsagesInScope(J.Identifier varIdentifier) {
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (cu == null) {
                    return false;
                }

                return new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                        if (!found.get() &&
                                SemanticallyEqual.areEqual(identifier, varIdentifier) &&
                                !(getCursor().getParentTreeCursor().getValue() instanceof J.VariableDeclarations.NamedVariable)) {
                            found.set(true);
                        }
                        return identifier;
                    }
                }.reduce(cu, new AtomicBoolean()).get();
            }

            private boolean isClassScope() {
                return getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.ClassDeclaration;
            }

            private boolean isMethodParameter() {
                return getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.MethodDeclaration;
            }
        };
    }
}

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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;


@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveVariablesByPackage extends Recipe {

    @Option(displayName = "Package filter",
            description = "The package name to filter methods by. Methods with return types or parameter types in this package (or subpackages) will be removed.",
            example = "com.example.unwanted")
    String packageFilter;

    @Option(displayName = "Remove only class scope",
            description = "Ignores variables that are method scope",
            example = "com.example.unwanted")
    Boolean removeOnlyClassScope;

    String displayName = "Remove class variables matching package filter";

    String description = "Removes class-level variables from classes in the specified package.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

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

                // Check if the variable matches the package filter
                boolean matches = false;

                // Check initializers for method calls from filtered package
                for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                    if (var.getInitializer() instanceof J.MethodInvocation) {
                        JavaType.Method methodType =
                                ((J.MethodInvocation) var.getInitializer()).getMethodType();
                        if (methodType != null &&
                                methodType.getDeclaringType().getFullyQualifiedName().contains(packageFilter)) {
                            matches = true;
                            break;
                        }
                    }
                }

                // Original type checks
                if (!matches) {
                    JavaType.FullyQualified declaredFqn = resolveFullyQualified(vd.getType());
                    if (declaredFqn != null && declaredFqn.getFullyQualifiedName().contains(packageFilter)) {
                        matches = true;
                    }
                }

                if (!matches) {
                    for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                        if (var.getInitializer() != null) {
                            JavaType.FullyQualified initFqn = resolveFullyQualified(var.getInitializer().getType());
                            if (initFqn != null && initFqn.getFullyQualifiedName().contains(packageFilter)) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }

                if (!matches) {
                    return vd;
                }

                // Before removing, check if any declared variable has usages in scope
                for (J.VariableDeclarations.NamedVariable var : vd.getVariables()) {
                    if (hasUsagesInScope(var.getSimpleName())) {
                        return vd;
                    }
                }

                doAfterVisit(new RemoveImportsVisitor());
                return null;
            }

            private boolean hasUsagesInScope(String varName) {
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (cu == null) {
                    return false;
                }

                // Count occurrences of the variable name as a standalone identifier in the source
                String source = cu.printAll();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                        "\\b" + java.util.regex.Pattern.quote(varName) + "\\b");
                java.util.regex.Matcher m = p.matcher(source);
                int count = 0;
                while (m.find()) {
                    count++;
                }
                // At least 1 occurrence is the declaration itself; more means usages exist
                return count > 1;
            }

            private JavaType.FullyQualified resolveFullyQualified(JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    return TypeUtils.asFullyQualified( ((JavaType.Parameterized) type).getType() );
                }
                if (type instanceof JavaType.Class) {
                    return TypeUtils.asFullyQualified( type );
                }
                return null;
            }

            private boolean isClassScope() {
                return getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.ClassDeclaration;
            }

            private boolean isMethodParameter() {
                return getCursor().dropParentUntil(J.class::isInstance).getValue() instanceof J.MethodDeclaration;
            }
        };
    }

    // For some reason, just calling maybeRemoveImport(..) does not work properly.
    static class RemoveImportsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
            J.Import imp = super.visitImport(_import, ctx);
            maybeRemoveImport(imp.getQualid().toString());
            return imp;
        }
    }
}

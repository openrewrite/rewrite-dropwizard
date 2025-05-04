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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveMethodsByPackage extends Recipe {

    @Option(displayName = "Package filter",
            description = "The package name to filter methods by. Methods with return types or parameter types in this package (or subpackages) will be removed.",
            example = "com.example.unwanted")
    String packageFilter;

    @Override
    public String getDisplayName() {
        return "Remove methods referencing specified package";
    }

    @Override
    public String getDescription() {
        return "Removes any method that has a return type or parameter type from the specified package.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.@Nullable MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                // Check the return type
                if (method.getReturnTypeExpression() != null) {
                    JavaType.FullyQualified fq =
                            TypeUtils.asFullyQualified(method.getReturnTypeExpression().getType());

                    if (fq != null && fq.getFullyQualifiedName().startsWith(packageFilter)) {
                        maybeRemoveImport(fq);
                        return null;
                    }

                    String returnTypeStr = method.getReturnTypeExpression().toString();
                    if (returnTypeStr.startsWith(packageFilter)) {
                        maybeRemoveImport(returnTypeStr);
                        return null;
                    }
                }

                // Check each parameter type
                for (Statement param : method.getParameters()) {
                    if (param instanceof J.VariableDeclarations) {
                        for (J.VariableDeclarations.NamedVariable namedVar :
                                ((J.VariableDeclarations) param).getVariables()) {
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(namedVar.getType());
                            if (fq != null) {
                                if (fq.getFullyQualifiedName().startsWith(packageFilter)) {
                                    maybeRemoveImport(fq);
                                    return null;
                                }
                            }
                        }
                    }
                }
                return m;
            }
        };
    }
}

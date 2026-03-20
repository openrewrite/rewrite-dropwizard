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
package org.openrewrite.java.dropwizard.method;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveSuperTypeByType extends Recipe {

    @Option(displayName = "Fully qualified name of the superclass to remove",
            description = "Supertypes that match this name are to be removed",
            example = "io.dropwizard.Configuration")
    String typeToRemove;

    String displayName = "Remove supertype by fully qualified name matches";

    String description = "Removes a specified type from class extends or implements clauses.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(typeToRemove, false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                        // Handle extends clause
                        if (cd.getExtends() != null && isOfClassType(cd.getExtends().getType(), typeToRemove)) {
                            cd = cd.withExtends(null);
                            JavaType.ShallowClass type = (JavaType.ShallowClass) JavaType.buildType("java.lang.Object");
                            doAfterVisit(new UpdateMethodTypesVisitor(type));
                            doAfterVisit(new RemoveUnnecessarySuperCalls.RemoveUnnecessarySuperCallsVisitor());
                        }

                        // Handle implements clause
                        if (cd.getImplements() != null) {
                            java.util.List<TypeTree> remaining = new java.util.ArrayList<>();
                            boolean found = false;
                            for (TypeTree impl : cd.getImplements()) {
                                if (isOfClassType(impl.getType(), typeToRemove)) {
                                    found = true;
                                    maybeRemoveImport(typeToRemove);
                                } else {
                                    remaining.add(impl);
                                }
                            }
                            if (found) {
                                cd = cd.withImplements(remaining.isEmpty() ? null : remaining);
                                // Update the class type metadata to remove the interface, so
                                // TypeUtils.isOverride() no longer sees it as an override target
                                if (cd.getType() instanceof JavaType.Class) {
                                    JavaType.Class classType = (JavaType.Class) cd.getType();
                                    java.util.List<JavaType.FullyQualified> updatedInterfaces = new java.util.ArrayList<>();
                                    for (JavaType.FullyQualified iface : classType.getInterfaces()) {
                                        if (!isOfClassType(iface, typeToRemove)) {
                                            updatedInterfaces.add(iface);
                                        }
                                    }
                                    JavaType.Class updatedClassType = classType.withInterfaces(updatedInterfaces);
                                    cd = cd.withType(updatedClassType);
                                    // Also update method types so their declaring type reflects the removed interface
                                    doAfterVisit(new UpdateMethodTypesVisitor(updatedClassType));
                                }
                                doAfterVisit(new RemoveUnnecessaryOverride(false).getVisitor());
                            }
                        }

                        return cd;
                    }

                }
        );
    }
}

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

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public abstract class RemoveSuperTypeVisitor extends JavaIsoVisitor<ExecutionContext> {

    protected abstract boolean shouldRemoveType(JavaType type);

    @Override
    public J.ClassDeclaration visitClassDeclaration(
            J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

        JavaType.FullyQualified fqn =
                TypeUtils.asFullyQualified(JavaType.buildType("java.lang.Object"));
        boolean modified = false;

        // Handle extends clause
        if (nonNull(cd.getExtends())
                && nonNull(cd.getExtends().getType())
                && shouldRemoveType(cd.getExtends().getType())) {
            modified = true;
            cd = cd.withExtends(null);
        }

        // Handle implements clause
        if (nonNull(cd.getImplements())) {
            List<TypeTree> filteredImplements =
                    cd.getImplements().stream()
                            .filter(impl -> !shouldRemoveType(impl.getType()))
                            .collect(Collectors.toList());

            if (filteredImplements.size() < cd.getImplements().size()) {
                modified = true;
                cd = cd.withImplements(filteredImplements);
            }
        }

        if (modified) {
            runCleanup(fqn);
        }

        return cd;
    }

    private void runCleanup(JavaType.FullyQualified fqn) {
        doAfterVisit(new UpdateMethodTypesVisitor(fqn));
        doAfterVisit(new RemoveUnnecessarySuperCalls.RemoveUnnecessarySuperCallsVisitor());
    }
}

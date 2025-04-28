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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public abstract class RemoveSuperTypeVisitor extends JavaIsoVisitor<ExecutionContext> {

    protected abstract boolean shouldRemoveType(@Nullable JavaType type);

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

        if (cd.getExtends() != null && shouldRemoveType(cd.getExtends().getType())) {
            cd = cd.withExtends(null);
        }
        if (cd.getImplements() != null) {
            cd = cd.withImplements(ListUtils.filter(cd.getImplements(), impl -> !shouldRemoveType(impl.getType())));

        }
        if (cd != classDecl) {
            JavaType.ShallowClass type = (JavaType.ShallowClass) JavaType.buildType("java.lang.Object");
            doAfterVisit(new UpdateMethodTypesVisitor(type));
            doAfterVisit(new RemoveUnnecessarySuperCalls.RemoveUnnecessarySuperCallsVisitor());
        }

        return cd;
    }

}

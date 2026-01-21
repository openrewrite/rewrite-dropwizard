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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class RemoveUnnecessarySuperCalls extends Recipe {

    @Getter
    final String displayName = "Remove `super` calls when the class does not extend another class";

    @Getter
    final String description = "Removes calls to `super(...)` or `super.someMethod(...)` if the class does not have a real superclass besides `java.lang.Object`.";

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new RemoveUnnecessarySuperCallsVisitor();
    }

    public static class RemoveUnnecessarySuperCallsVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

            if (isSuperCall(mi)) {
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (classDecl != null) {
                    if (!hasRealSuperclass(classDecl)) {
                        return null;
                    }
                    if (!TypeUtils.isOverride(mi.getMethodType())) {
                        return null;
                    }
                }
            }
            return mi;
        }

        /**
         * Checks if this is a call on `super`, like `super(...)` or `super.someMethod(...)`.
         */
        private boolean isSuperCall(J.MethodInvocation mi) {
            // e.g. super.method()
            if (mi.getSelect() instanceof J.Identifier) {
                return "super".equals(((J.Identifier) mi.getSelect()).getSimpleName());
            }
            // e.g. constructor call "super(...)"
            return mi.getSelect() == null && "super".equals(mi.getSimpleName());
        }

        /**
         * Checks if the class extends some real superclass other than `java.lang.Object`.
         */
        private boolean hasRealSuperclass(J.ClassDeclaration classDecl) {
            if (classDecl.getExtends() == null) {
                return false;
            }

            // If we can't resolve types, we might choose to be safe and not remove
            JavaType.FullyQualified fqClassType = TypeUtils.asFullyQualified(classDecl.getType());
            if (fqClassType == null) {
                return true;
            }

            JavaType.FullyQualified fqSuperType = TypeUtils.asFullyQualified(fqClassType.getSupertype());
            if (fqSuperType == null) {
                return false;
            }

            // If the only "superclass" is Object, treat as no real parent
            return !"java.lang.Object".equals(fqSuperType.getFullyQualifiedName());
        }
    }
}

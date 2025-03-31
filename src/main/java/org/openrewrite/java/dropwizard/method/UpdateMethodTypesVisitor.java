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

public class UpdateMethodTypesVisitor extends JavaIsoVisitor<ExecutionContext> {
    private final JavaType.FullyQualified newSuperclass;

    public UpdateMethodTypesVisitor(JavaType.FullyQualified newSuperclass) {
        this.newSuperclass = newSuperclass;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(
            J.MethodDeclaration method, ExecutionContext ctx) {
        JavaType.Method methodType = method.getMethodType();

        if (methodType != null) {

            boolean hasOverrideAnnotation =
                    method.getLeadingAnnotations().stream()
                            .anyMatch(annotation -> "Override".equals(annotation.getSimpleName()));

            if (hasOverrideAnnotation) {
                JavaType.FullyQualified declaringType = methodType.getDeclaringType();
                if (declaringType != null) {
                    JavaType.Method originalMethodType = method.getMethodType();
                    if (originalMethodType != null) {
                        JavaType.Method newMethodType = originalMethodType.withDeclaringType(newSuperclass);
                        method = method.withMethodType(newMethodType);
                        method = method.withName(method.getName().withType(newMethodType));
                    }
                    return method;
                }
            }
        }
        return method;
    }
}

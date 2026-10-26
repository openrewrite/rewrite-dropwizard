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

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@RequiredArgsConstructor
public class UpdateMethodTypesVisitor extends JavaIsoVisitor<ExecutionContext> {
    private final JavaType.FullyQualified declaringType;
    private final Map<JavaType.FullyQualified, JavaType.FullyQualified> updatedTypes = new IdentityHashMap<>();

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
        return cd.getType() == null ? cd : cd.withType(updateHierarchy(cd.getType()));
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(
            J.MethodDeclaration method, ExecutionContext ctx) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        JavaType.Method methodType = m.getMethodType();
        if (methodType != null) {
            JavaType.Method updated = methodType.withDeclaringType(updateHierarchy(methodType.getDeclaringType()));
            return m.withMethodType(updated).withName(m.getName().withType(updated));
        }
        return m;
    }

    private JavaType.FullyQualified updateHierarchy(JavaType.FullyQualified type) {
        JavaType.FullyQualified cached = updatedTypes.get(type);
        if (cached != null) {
            return cached;
        }
        updatedTypes.put(type, type);
        JavaType.FullyQualified updated = type;
        if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            updated = parameterized.withType(updateHierarchy(parameterized.getType()));
        } else if (isOfClassType(type, declaringType.getFullyQualifiedName())) {
            updated = declaringType;
        } else if (type instanceof JavaType.Class && type.getSupertype() != null) {
            updated = ((JavaType.Class) type).withSupertype(updateHierarchy(type.getSupertype()));
        }
        updatedTypes.put(type, updated);
        return updated;
    }
}

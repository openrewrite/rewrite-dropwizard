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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveUnnecessaryOverride extends Recipe {

    private static final AnnotationMatcher OVERRIDE_ANNOTATION = new AnnotationMatcher("@java.lang.Override");
    private static final RemoveAnnotationVisitor removeAnnotationVisitor = new RemoveAnnotationVisitor(OVERRIDE_ANNOTATION);

    @Option(
            displayName = "Ignore methods in anonymous classes",
            description = "When enabled, ignore @Override annotations on methods in anonymous classes.",
            required = false)
    @Nullable
    Boolean ignoreAnonymousClassMethods;

    @Override
    public String getDisplayName() {
        return "Remove unnecessary `@Override` annotations";
    }

    @Override
    public String getDescription() {
        return "Removes `@Override` annotations from methods that don't actually override or implement any method. " +
                "This helps maintain clean code by removing incorrect annotations that could be misleading.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("java.lang.Override", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    private Cursor getCursorToParentScope(Cursor cursor) {
                        return cursor.dropParentUntil(is -> is instanceof J.NewClass || is instanceof J.ClassDeclaration);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        if (!m.isConstructor() &&
                                service(AnnotationService.class).matches(getCursor(), OVERRIDE_ANNOTATION) &&
                                !TypeUtils.isOverride(m.getMethodType()) &&
                                !(Boolean.TRUE.equals(ignoreAnonymousClassMethods) &&
                                        getCursorToParentScope(getCursor()).getValue() instanceof J.NewClass)) {
                            return removeAnnotationVisitor.visitMethodDeclaration(m, ctx);
                        }
                        return m;
                    }
                });
    }
}

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

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
package org.openrewrite.java.dropwizard.lifecycle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateManagedLifecycle extends Recipe {

    String displayName = "Migrate Dropwizard Managed lifecycle to Spring Boot";

    String description = "Converts classes implementing `io.dropwizard.lifecycle.Managed` to Spring Boot " +
            "lifecycle beans using `@Component`, `@PostConstruct`, and `@PreDestroy`.";

    private static final String MANAGED_TYPE = "io.dropwizard.lifecycle.Managed";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(MANAGED_TYPE, false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                        if (cd.getImplements() == null) {
                            return cd;
                        }

                        boolean implementsManaged = cd.getImplements().stream()
                                .anyMatch(impl -> isOfClassType(impl.getType(), MANAGED_TYPE));

                        if (!implementsManaged) {
                            return cd;
                        }

                        // Remove Managed from implements clause
                        List<TypeTree> remaining = new ArrayList<>();
                        for (TypeTree impl : cd.getImplements()) {
                            if (!isOfClassType(impl.getType(), MANAGED_TYPE)) {
                                remaining.add(impl);
                            }
                        }
                        cd = cd.withImplements(remaining.isEmpty() ? null : remaining);

                        // Update type metadata
                        if (cd.getType() instanceof JavaType.Class) {
                            JavaType.Class classType = (JavaType.Class) cd.getType();
                            List<JavaType.FullyQualified> updatedInterfaces = new ArrayList<>();
                            for (JavaType.FullyQualified iface : classType.getInterfaces()) {
                                if (!isOfClassType(iface, MANAGED_TYPE)) {
                                    updatedInterfaces.add(iface);
                                }
                            }
                            cd = cd.withType(classType.withInterfaces(updatedInterfaces));
                        }

                        // Add @Component annotation
                        maybeAddImport("org.springframework.stereotype.Component");
                        doAfterVisit(new org.openrewrite.java.AddImport<>("org.springframework.stereotype.Component", null, false));
                        cd = JavaTemplate.builder("@Component")
                                .imports("org.springframework.stereotype.Component")
                                .build()
                                .apply(
                                        updateCursor(cd),
                                        cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));

                        // Remove Managed import
                        maybeRemoveImport(MANAGED_TYPE);

                        return cd;
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                        // Only process methods in classes that implement Managed
                        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (enclosingClass == null || enclosingClass.getImplements() == null) {
                            return md;
                        }

                        boolean inManagedClass = enclosingClass.getImplements().stream()
                                .anyMatch(impl -> isOfClassType(impl.getType(), MANAGED_TYPE));

                        if (!inManagedClass) {
                            return md;
                        }

                        String methodName = md.getSimpleName();
                        String annotationFqn = null;

                        if ("start".equals(methodName)) {
                            annotationFqn = "jakarta.annotation.PostConstruct";
                        } else if ("stop".equals(methodName)) {
                            annotationFqn = "jakarta.annotation.PreDestroy";
                        }

                        if (annotationFqn == null) {
                            return md;
                        }

                        // Remove @Override if present
                        List<J.Annotation> annotations = new ArrayList<>();
                        for (J.Annotation ann : md.getLeadingAnnotations()) {
                            if (!"Override".equals(ann.getSimpleName())) {
                                annotations.add(ann);
                            }
                        }
                        md = md.withLeadingAnnotations(annotations);

                        // Add the lifecycle annotation
                        String simpleName = annotationFqn.substring(annotationFqn.lastIndexOf('.') + 1);
                        doAfterVisit(new org.openrewrite.java.AddImport<>(annotationFqn, null, false));
                        md = JavaTemplate.builder("@" + simpleName)
                                .imports(annotationFqn)
                                .build()
                                .apply(
                                        updateCursor(md),
                                        md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));

                        return md;
                    }
                }
        );
    }
}

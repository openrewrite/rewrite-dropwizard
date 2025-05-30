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
package org.openrewrite.java.dropwizard.annotation;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;

import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static org.openrewrite.java.JavaParser.fromJavaVersion;
import static org.openrewrite.java.JavaParser.runtimeClasspath;

@RequiredArgsConstructor
public abstract class AddClassAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final String annotationText;
    private final Boolean annotateSubclasses;

    protected abstract boolean shouldAddAnnotation(ClassDeclaration cd);

    @Override
    public ClassDeclaration visitClassDeclaration(ClassDeclaration classDeclaration, ExecutionContext ctx) {
        ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, ctx);

        // if annotation parameters are given as well
        String annotationType = annotationText.split("[(<]")[0].trim();

        if (service(AnnotationService.class).matches(getCursor(), new AnnotationMatcher(annotationType))) {
            return cd;
        }

        boolean shouldAdd = shouldAddAnnotation(cd);

        if (!shouldAdd && TRUE.equals(annotateSubclasses)) {
            shouldAdd = shouldAddAnnotationToAnyParentClass();
        }

        if (!shouldAdd) {
            return cd;
        }

        maybeAddImport(annotationType);

        ClassDeclaration updated = JavaTemplate.builder("@#{}")
                .javaParser(fromJavaVersion().classpath(runtimeClasspath()))
                .imports(annotationType)
                .build()
                .apply(
                        updateCursor(cd),
                        cd.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)),
                        annotationText);
        doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(updated));
        return maybeAutoFormat(cd, updated, ctx);
    }

    private boolean shouldAddAnnotationToAnyParentClass() {
        Cursor cursor = getCursor();
        while (true) {
            cursor = cursor.getParent();
            if (cursor == null) {
                return false;
            }
            if (cursor.getValue() instanceof ClassDeclaration &&
                    shouldAddAnnotation(cursor.getValue())) {
                return true;
            }
        }
    }
}

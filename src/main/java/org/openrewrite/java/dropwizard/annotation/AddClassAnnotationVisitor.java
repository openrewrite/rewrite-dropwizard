package org.openrewrite.java.dropwizard.annotation;

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

public abstract class AddClassAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final String annotationText;
    private final boolean annotateSubclasses;

    public AddClassAnnotationVisitor(String annotationText, boolean annotateSubclasses) {
        this.annotationText = annotationText;
        this.annotateSubclasses = annotateSubclasses;
    }

    protected abstract boolean shouldAddAnnotation(ClassDeclaration cd);

    @Override
    public ClassDeclaration visitClassDeclaration(ClassDeclaration classDeclaration, ExecutionContext ctx) {
        ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, ctx);

        // if annotation parameters are given as well
        String annotationType = annotationText.split("[(<]")[0].trim();

        if (service(AnnotationService.class)
                .matches(getCursor(), new AnnotationMatcher(annotationType))) {
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

        ClassDeclaration updated =
                JavaTemplate.builder("@#{}")
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
        try {
            getCursor().dropParentUntil(
                    x -> {
                        if (x instanceof J.ClassDeclaration) {
                            return shouldAddAnnotation((J.ClassDeclaration) x);
                        }
                        return false;
                    });

            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}

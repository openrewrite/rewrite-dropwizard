package org.openrewrite.dropwizard.annotation;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static org.openrewrite.java.JavaParser.fromJavaVersion;
import static org.openrewrite.java.JavaParser.runtimeClasspath;

public abstract class AddClassAnnotationRecipe extends Recipe {
    private final String annotationText;
    private final Boolean annotateSubclasses;

    public AddClassAnnotationRecipe(String annotationText, Boolean annotateSubclasses) {
        this.annotationText = annotationText;
        this.annotateSubclasses = annotateSubclasses;
    }

    protected abstract boolean shouldAddAnnotation(ClassDeclaration cd);

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public ClassDeclaration visitClassDeclaration(
                    ClassDeclaration classDeclaration, ExecutionContext ctx) {
                ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, ctx);

                // if annotation parameters are given as well
                String annotationType = annotationText.split("[(<]")[0].trim();

                JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(JavaType.buildType(annotationType));

                if (fqn == null) {
                    return cd;
                }

                for (Annotation annotation : cd.getLeadingAnnotations()) {
                    if (annotation.getSimpleName().equals(fqn.getClassName())) {
                        return cd;
                    }
                }

                boolean shouldAdd = shouldAddAnnotation(cd);

                if (!shouldAdd && TRUE.equals(annotateSubclasses)) {
                    shouldAdd = shouldAddAnnotationToAnyParentClass();
                }

                if (!shouldAdd) {
                    return cd;
                }
                maybeAddImport(fqn);

                JavaTemplate template =
                        JavaTemplate.builder("@#{}\n")
                                .contextSensitive()
                                .javaParser(fromJavaVersion().classpath(runtimeClasspath()))
                                .imports(fqn.getFullyQualifiedName())
                                .build();

                ClassDeclaration updated =
                        template.apply(
                                updateCursor(cd),
                                cd.getCoordinates()
                                        .addAnnotation(comparing(J.Annotation::getSimpleName)),
                                annotationText);

                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(updated));
                return maybeAutoFormat(cd, updated, ctx);
            }

            private boolean shouldAddAnnotationToAnyParentClass() {
                getCursor().dropParentUntil(
                        x -> {
                            if (x instanceof J.ClassDeclaration) {
                                return shouldAddAnnotation((J.ClassDeclaration) x);
                            }
                            return false;
                        });

                return true;

            }
        };
    }
}

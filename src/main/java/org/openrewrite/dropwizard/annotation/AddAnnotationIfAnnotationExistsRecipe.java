package org.openrewrite.dropwizard.annotation;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class AddAnnotationIfAnnotationExistsRecipe extends AddClassAnnotationRecipe {

    private final String targetAnnotationClassNames;

    public AddAnnotationIfAnnotationExistsRecipe(
            String annotationToAdd, String targetAnnotationClassNames, Boolean annotateInnerClasses) {
        super(annotationToAdd, annotateInnerClasses);
        this.targetAnnotationClassNames = (targetAnnotationClassNames);
    }

    @Override
    public String getDisplayName() {
        return "Add annotation if target annotations exist";
    }

    @Override
    public String getDescription() {
        return "Adds annotation if class has any of the specified target annotations.";
    }

    @Override
    protected boolean shouldAddAnnotation(J.ClassDeclaration cd) {
        return cd.getLeadingAnnotations().stream()
                .anyMatch(
                        annotation -> {
                            JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                            return type != null
                                    && targetAnnotationClassNames.equals(type.getFullyQualifiedName());
                        });
    }
}

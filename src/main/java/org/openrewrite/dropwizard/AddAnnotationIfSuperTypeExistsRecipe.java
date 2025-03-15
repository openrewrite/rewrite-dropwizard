package org.openrewrite.dropwizard;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Objects;

public class AddAnnotationIfSuperTypeExistsRecipe extends AddClassAnnotationRecipe {

    private final String targetSupertypeNames;

    public AddAnnotationIfSuperTypeExistsRecipe(
            String annotationToAdd, String targetSupertypeNames, boolean annotateInnerClasses) {
        super(annotationToAdd, annotateInnerClasses);
        this.targetSupertypeNames = targetSupertypeNames;
    }

    @Override
    public String getDisplayName() {
        return "Add annotation if target supertypes exist";
    }

    @Override
    public String getDescription() {
        return "Adds annotation if class extends or implements any of the specified target types.";
    }

    @Override
    protected boolean shouldAddAnnotation(J.ClassDeclaration cd) {
        if (cd.getExtends() != null) {
            JavaType.FullyQualified extendsType = TypeUtils.asFullyQualified(cd.getExtends().getType());
            if (extendsType != null && targetSupertypeNames.equals(extendsType.getFullyQualifiedName())) {
                return true;
            }
        }

        if (cd.getImplements() != null) {
            return cd.getImplements().stream()
                    .map(impl -> TypeUtils.asFullyQualified(impl.getType()))
                    .filter(Objects::nonNull)
                    .anyMatch(type -> targetSupertypeNames.equals(type.getFullyQualifiedName()));
        }

        return false;
    }
}

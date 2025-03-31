package org.openrewrite.java.dropwizard.method;

import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

public class RemoveSuperTypeRecipe extends RemoveSuperType {
    private final String typeToRemove;

    public RemoveSuperTypeRecipe(String typeToRemove) {
        this.typeToRemove = typeToRemove;
    }

    @Override
    public String getDisplayName() {
        return "Remove supertype by fully qualified name matches";
    }

    @Override
    public String getDescription() {
        return "Removes a specified type from class extends or implements clauses.";
    }

    @Override
    protected boolean shouldRemoveType(JavaType type) {
        return isOfClassType(type, typeToRemove);
    }
}

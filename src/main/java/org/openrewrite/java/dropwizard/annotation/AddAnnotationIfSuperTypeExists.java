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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Objects;

public class AddAnnotationIfSuperTypeExists extends AddClassAnnotation {

    private final String targetSupertypeNames;

    public AddAnnotationIfSuperTypeExists(
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

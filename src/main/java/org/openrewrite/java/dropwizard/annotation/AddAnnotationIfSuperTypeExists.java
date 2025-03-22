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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddAnnotationIfSuperTypeExists extends Recipe {

    @Option
    String targetAnnotationClassNames;

    @Option
    String annotationToAdd;

    @Option(required = false)
    Boolean annotateInnerClasses;

    @Override
    public String getDisplayName() {
        return "Add annotation if target supertypes exist";
    }

    @Override
    public String getDescription() {
        return "Adds annotation if class extends or implements any of the specified target types.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddClassAnnotationIfSuperTypeExistsVisitor(targetAnnotationClassNames,
                annotationToAdd, annotateInnerClasses);
    }

    private static class AddClassAnnotationIfSuperTypeExistsVisitor extends AddClassAnnotationVisitor {

        private final String targetSupertypeName;

        public AddClassAnnotationIfSuperTypeExistsVisitor(String targetSupertypeName,
                                                          String annotationText, boolean annotateSubclasses) {
            super(annotationText, annotateSubclasses);
            this.targetSupertypeName = targetSupertypeName;
        }

        @Override
        protected boolean shouldAddAnnotation(J.ClassDeclaration cd) {
            if (cd.getExtends() != null) {
                JavaType.FullyQualified extendsType = TypeUtils.asFullyQualified(cd.getExtends().getType());
                if (extendsType != null && targetSupertypeName.equals(extendsType.getFullyQualifiedName())) {
                    return true;
                }
            }

            if (cd.getImplements() != null) {
                return cd.getImplements().stream()
                        .map(impl -> TypeUtils.asFullyQualified(impl.getType()))
                        .filter(Objects::nonNull)
                        .anyMatch(type -> targetSupertypeName.equals(type.getFullyQualifiedName()));
            }

            return false;
        }
    }
}

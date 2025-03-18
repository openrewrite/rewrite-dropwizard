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

public class AddMethodAnnotationIfAnnotationExists extends AddMethodAnnotation {
    private final String targetAnnotationClassName;

    public AddMethodAnnotationIfAnnotationExists(
            String annotationToAdd, String targetAnnotationClassName) {
        super(annotationToAdd);
        this.targetAnnotationClassName = targetAnnotationClassName;
    }

    @Override
    public String getDisplayName() {
        return "Add annotation if target method annotation exists";
    }

    @Override
    public String getDescription() {
        return "Adds an annotation to methods that have a specific target annotation.";
    }

    @Override
    protected boolean shouldAddAnnotation(J.MethodDeclaration method) {
        return method.getLeadingAnnotations().stream()
                .anyMatch(
                        annotation -> {
                            JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                            return type != null && targetAnnotationClassName.equals(type.getFullyQualifiedName());
                        });
    }
}

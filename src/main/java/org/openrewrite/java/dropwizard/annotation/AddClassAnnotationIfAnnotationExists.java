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
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddClassAnnotationIfAnnotationExists extends Recipe {

    @Option(displayName = "Annotation to add",
            description = "The annotation that should be added.",
            example = "org.springframework.stereotype.Component")
    String annotationToAdd;

    @Option(displayName = "Target annotation class name",
            description = "The annotation that should looked for.",
            example = "javax.ws.rs.Path")
    String targetAnnotationClassName;

    @Option(displayName = "Annotate inner classes",
            description = "Boolean whether to annotate inner classes of the matched annotation",
            required = false)
    Boolean annotateInnerClasses;

    @Override
    public String getDisplayName() {
        return "Add annotation if target annotations exist";
    }

    @Override
    public String getDescription() {
        return "Adds annotation if class has any of the specified target annotations.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddClassAnnotationVisitor(annotationToAdd, annotateInnerClasses) {
            @Override
            protected boolean shouldAddAnnotation(J.ClassDeclaration cd) {
                return service(AnnotationService.class)
                        .getAllAnnotations(getCursor())
                        .stream()
                        .anyMatch(annotation -> {
                            JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                            return type != null && targetAnnotationClassName.equals(type.getFullyQualifiedName());
                        });
            }
        };
    }
}

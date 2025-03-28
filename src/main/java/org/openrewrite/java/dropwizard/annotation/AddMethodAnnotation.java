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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;

public abstract class AddMethodAnnotation extends Recipe {
  private final String annotationText;

  public AddMethodAnnotation(String annotationText) {
    this.annotationText = annotationText;
  }

  protected abstract boolean shouldAddAnnotation(J.MethodDeclaration method);

  @Override
  public String getDisplayName() {
    return "Add method annotation to methods with specific annotation";
  }

  @Override
  public String getDescription() {
    return "Adds a specified annotation to methods that already have another specific annotation.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      @Override
      public J.MethodDeclaration visitMethodDeclaration(
          J.MethodDeclaration method, ExecutionContext ctx) {
        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

        String annotationType = annotationText.split("[(<]")[0].trim();

        JavaType.FullyQualified fqn =
                TypeUtils.asFullyQualified(JavaType.buildType(annotationType));

//        if (service(AnnotationService.class)
//                .matches(getCursor(), new AnnotationMatcher(annotationType))) {
//          return md;
//        }
//          // This does not catch it

        for (J.Annotation annotation : method.getLeadingAnnotations()) {
          if (annotation.getSimpleName().equals(fqn.getClassName())) {
            return method;
          }
        }

        if (!shouldAddAnnotation(md)) {
          return md;
        }

        maybeAddImport(annotationType);

        JavaTemplate template =
            JavaTemplate.builder("@#{}\n")
                .contextSensitive()
                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                .imports(annotationType)
                .build();

        J.MethodDeclaration updated =
            template.apply(
                updateCursor(md),
                md.getCoordinates()
                    .addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                annotationText);

        // Format imports and code
        doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(updated));
        return maybeAutoFormat(md, updated, ctx);
      }
    };
  }
}

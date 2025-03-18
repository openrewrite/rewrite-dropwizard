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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class AnnotationTransformer extends Recipe {
  private final String sourceAnnotation;
  private final String targetAnnotation;
  private final Map<String, String> attributeMapping;
  private final Map<String, SerializableFunction<Object, Object>> valueTransformers;

  protected AnnotationTransformer() {
    this.sourceAnnotation = getSourceAnnotation();
    this.targetAnnotation = getTargetAnnotation();
    this.attributeMapping = getAttributeMappings();
    this.valueTransformers = getValueTransformers();
  }

  /**
   * @return The fully qualified class name of the source annotation to transform.
   */
  protected abstract String getSourceAnnotation();

  /**
   * @return The fully qualified class name of the target annotation to transform into.
   */
  protected abstract String getTargetAnnotation();

  /**
   * @return A map of source attribute names to target attribute names.
   */
  protected abstract Map<String, String> getAttributeMappings();

  /**
   * @return A map of source attribute names to functions that transform their values.
   */
  protected abstract Map<String, SerializableFunction<Object, Object>> getValueTransformers();

  /**
   * Optionally provide fallback attributes if the source annotation has no attributes. By default,
   * returns an empty map. Subclasses can override this to provide a default.
   */
  protected Map<String, Object> getFallbackAttributes() {
    return new HashMap<>();
  }

  private Object transformValue(String attributeName, Object value) {
    SerializableFunction<Object, Object> transformer = valueTransformers.get(attributeName);
    return transformer != null ? transformer.apply(value) : value;
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      @Override
      public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
        J.Annotation a = super.visitAnnotation(annotation, ctx);

        if (!isSourceAnnotation(a)) {
          return a;
        }

        // Remove the import for the source annotation
        maybeRemoveImport(sourceAnnotation);

        Map<String, Object> attributes = new HashMap<>();

        if (a.getArguments() != null && !a.getArguments().isEmpty()) {
          // Extract attributes from the source annotation
          for (Expression arg : a.getArguments()) {
            if (arg instanceof J.Assignment) {
              handleAssignedAttribute((J.Assignment) arg, attributes);
            } else {
              // Handle unnamed attribute, commonly the default attribute
              handleUnnamedAttribute(arg, attributes);
            }
          }
        }

        // If no attributes were found, try fallback attributes
        if (attributes.isEmpty()) {
          attributes.putAll(getFallbackAttributes());
        }

        maybeAddImport(targetAnnotation);

        JavaTemplate template =
            JavaTemplate.builder(buildTemplate(attributes))
                .contextSensitive()
                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                .imports(targetAnnotation)
                .build();

        J.Annotation added =
            template.apply(getCursor(), a.getCoordinates().replace(), targetAnnotation);
        doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(added));
        return maybeAutoFormat(a, added, ctx);
      }

      private boolean isSourceAnnotation(J.Annotation annotation) {
        return annotation.getType() != null
            && TypeUtils.isOfClassType(annotation.getType(), sourceAnnotation);
      }

      private void handleAssignedAttribute(J.Assignment assign, Map<String, Object> attributes) {
        if (!(assign.getVariable() instanceof J.Identifier)) {
          return;
        }

        String sourceName = ((J.Identifier) assign.getVariable()).getSimpleName();
        if (!attributeMapping.containsKey(sourceName)) {
          return; // Skip attributes that aren't mapped
        }

        Object rawValue = extractValue(assign.getAssignment());
        Object transformedValue = transformValue(sourceName, rawValue);
        String targetName = attributeMapping.get(sourceName);
        attributes.put(targetName, transformedValue);
      }

      private void handleUnnamedAttribute(Expression arg, Map<String, Object> attributes) {
        // If the annotation uses a single default attribute, map it to "value" if present.
        if (attributeMapping.containsKey("value")) {
          Object rawValue = extractValue(arg);
          Object transformedValue = transformValue("value", rawValue);
          attributes.put(attributeMapping.get("value"), transformedValue);
        }
      }

      private Object extractValue(Expression expr) {
        if (expr instanceof J.Literal) {
          // If it's a literal, return the literal value.
          return ((J.Literal) expr).getValue();
        } else if (expr instanceof J.NewArray) {
          // If it's an array, extract its elements.
          J.NewArray newArray = (J.NewArray) expr;
          if (newArray.getInitializer() != null && !newArray.getInitializer().isEmpty()) {
            // Attempt to convert each element into a string if they are literals.
            // For simplicity, assume they are string literals. If not, fallback.
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Expression elem : newArray.getInitializer()) {
              Object elemValue = extractValue(elem);
              if (!first) {
                sb.append(", ");
              }
              // If the element value is a String, quote it.
              // Otherwise, just append its string representation.
              if (elemValue instanceof String) {
                sb.append("\"").append(elemValue).append("\"");
              } else {
                sb.append(elemValue);
              }
              first = false;
            }
            return sb.toString();
          } else {
            // Empty array
            return "";
          }
        }

        // Fallback for other expressions: return their string representation.
        // Ideally, you handle other expression types more gracefully.
        return expr.toString();
      }

      private String buildTemplate(Map<String, Object> attributes) {
        if (attributes.isEmpty()) {
          return "@#{}\n";
        }

        StringBuilder template = new StringBuilder("@#{}(");
        boolean first = true;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
          if (!first) {
            template.append(", ");
          }
          template.append(entry.getKey()).append(" = ").append(formatValue(entry.getValue()));
          first = false;
        }
        template.append(")\n");
        return template.toString();
      }

      private String formatValue(Object value) {
        if (value == null) {
          return "null";
        }
        if (value instanceof String) {
          return "\"" + value + "\"";
        }
        return String.valueOf(value);
      }
    };
  }
}

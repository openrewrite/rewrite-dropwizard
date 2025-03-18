package org.openrewrite.java.dropwizard.annotation.security;

import org.openrewrite.java.dropwizard.annotation.AnnotationTransformer;
import org.openrewrite.java.dropwizard.annotation.SerializableFunction;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RolesToPreAuthorize extends AnnotationTransformer {

  @Override
  protected String getSourceAnnotation() {
    return "javax.annotation.security.RolesAllowed";
  }

  @Override
  protected String getTargetAnnotation() {
    return "org.springframework.security.access.prepost.PreAuthorize";
  }

  @Override
  protected Map<String, String> getAttributeMappings() {
    Map<String, String> mappings = new HashMap<>();
    mappings.put("value", "value");
    return mappings;
  }

  @Override
  protected Map<String, SerializableFunction<Object, Object>> getValueTransformers() {
    Map<String, SerializableFunction<Object, Object>> transformers = new HashMap<>();
    transformers.put(
        "value",
        obj -> {
          if (obj instanceof String[]) {
            String[] roles = (String[]) obj;
            // Strip any existing quotes first
            roles =
                Arrays.stream(roles)
                    .map(role -> role.replace("\"", "").trim())
                    .toArray(String[]::new);
            return "hasAnyRole('" + String.join("', '", roles) + "')";
          } else if (obj instanceof String) {
            String role = ((String) obj).replace("\"", "").trim();
            return "hasAnyRole('" + role + "')";
          }
          return obj;
        });
    return transformers;
  }

  @Override
  public String getDisplayName() {
    return "Replace @RolesAllowed with @PreAuthorize";
  }

  @Override
  public String getDescription() {
    return "Replaces @RolesAllowed annotation with Spring Security's @PreAuthorize, converting role expressions appropriately.";
  }
}

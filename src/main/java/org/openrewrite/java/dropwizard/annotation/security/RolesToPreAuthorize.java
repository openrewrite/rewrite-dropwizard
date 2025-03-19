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

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PermitAllToPreAuthorizeTransformer extends AnnotationTransformer {
  @Override
  protected String getSourceAnnotation() {
    return "javax.annotation.security.PermitAll";
  }

  @Override
  protected String getTargetAnnotation() {
    return "org.springframework.security.access.prepost.PreAuthorize";
  }

  @Override
  protected Map<String, String> getAttributeMappings() {
    // If in future you have an attribute on PermitAll that you want to map, you can add it here.
    // For now, since PermitAll has none, just map the "default" value attribute to "value".
    Map<String, String> map = new HashMap<>();
    map.put("value", "value");
    return map;
  }

  @Override
  protected Map<String, SerializableFunction<Object, Object>> getValueTransformers() {
    return Collections.emptyMap();
  }

  @Override
  protected Map<String, Object> getFallbackAttributes() {
    // Since PermitAll has no attributes, we provide the default attribute for PreAuthorize here:
    // We want @PreAuthorize("permitAll"), so value = "permitAll".
    Map<String, Object> fallback = new HashMap<>();
    fallback.put("value", "permitAll()");
    return fallback;
  }

  @Override
  public String getDisplayName() {
    return "Replace @PermitAll with @PreAuthorize(\"permitAll()\")";
  }

  @Override
  public String getDescription() {
    return "Replaces @PermitAll annotation with Spring Security's @PreAuthorize(\"permitAll()\"), converting role expressions appropriately.";
  }
}

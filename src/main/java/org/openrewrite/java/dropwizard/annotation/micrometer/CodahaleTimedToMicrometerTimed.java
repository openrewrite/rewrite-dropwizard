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
package org.openrewrite.java.dropwizard.annotation.micrometer;

import org.openrewrite.java.dropwizard.annotation.AnnotationTransformer;
import org.openrewrite.java.dropwizard.annotation.OneToOneTransformer;
import org.openrewrite.java.dropwizard.annotation.SerializableFunction;

import java.util.HashMap;
import java.util.Map;

public class CodahaleTimedToMicrometerTimed extends AnnotationTransformer {

  @Override
  protected String getSourceAnnotation() {
    return "com.codahale.metrics.annotation.Timed";
  }

  @Override
  protected String getTargetAnnotation() {
    return "io.micrometer.core.annotation.Timed";
  }

  @Override
  protected Map<String, String> getAttributeMappings() {
    Map<String, String> mappings = new HashMap<>();
    mappings.put("name", "value");
    mappings.put("absolute", "absolute");
    mappings.put("description", "description");
    return mappings;
  }

  @Override
  protected Map<String, SerializableFunction<Object, Object>> getValueTransformers() {
    Map<String, SerializableFunction<Object, Object>> transformers = new HashMap<>();
    // Direct 1:1 mapping for all attributes
    transformers.put("name", new OneToOneTransformer());
    transformers.put("absolute", new OneToOneTransformer());
    transformers.put("description", new OneToOneTransformer());
    return transformers;
  }

  @Override
  public String getDisplayName() {
    return "Replace @Timed (Dropwizard) with @Timed (Micrometer)";
  }

  @Override
  public String getDescription() {
    return "Replaces Dropwizard's @Timed annotation with Micrometer's @Timed annotation, preserving name (mapped to value), absolute, and description attributes.";
  }
}

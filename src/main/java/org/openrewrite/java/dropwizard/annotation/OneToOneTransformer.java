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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonDeserialize(as = OneToOneTransformer.class)
public class OneToOneTransformer implements SerializableFunction<Object, Object> {
  private static final long serialVersionUID = 1L;

  @Override
  public Object apply(Object value) {
    return value;
  }
}

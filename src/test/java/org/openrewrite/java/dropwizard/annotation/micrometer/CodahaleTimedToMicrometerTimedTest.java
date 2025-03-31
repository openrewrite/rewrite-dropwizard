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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CodahaleTimedToMicrometerTimedTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.dropwizard.annotation.micrometer.CodahaleTimedToMicrometerTimed")
          .parser(
            JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)
              .classpath("metrics-annotation", "micrometer-core"));
    }

    @DocumentExample
    @Test
    void transformsSimpleTimed() {
        rewriteRun(
          //language=java
          java(
            """
              package com.example;

              import com.codahale.metrics.annotation.Timed;

              class TestClass {
                  @Timed
                  public void timedMethod() {
                  }
              }
              """,
            """
              package com.example;

              import io.micrometer.core.annotation.Timed;

              class TestClass {
                  @Timed
                  public void timedMethod() {
                  }
              }
              """
          )
        );
    }

    @Test
    void transformsTimedWithName() {
        rewriteRun(
          //language=java
          java(
            """
              package com.example;

              import com.codahale.metrics.annotation.Timed;

              class TestClass {
                  @Timed(name = "customMetricName")
                  public void timedMethod() {
                  }
              }
              """,
            """
              package com.example;

              import io.micrometer.core.annotation.Timed;

              class TestClass {
                  @Timed(value = "customMetricName")
                  public void timedMethod() {
                  }
              }
              """
          )
        );
    }

    @Test
    void transformsTimedWithMultipleAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              package com.example;

              import com.codahale.metrics.annotation.Timed;

              class TestClass {
                  @Timed(name = "customMetricName", absolute = true, description = "Method execution time")
                  public void timedMethod() {
                  }
              }
              """,
            """
              package com.example;

              import io.micrometer.core.annotation.Timed;

              class TestClass {
                  @Timed(value = "customMetricName", absolute = true, description = "Method execution time")
                  public void timedMethod() {
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoresUnmappedAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              package com.example;

              import com.codahale.metrics.annotation.Timed;
              import java.util.concurrent.TimeUnit;

              class TestClass {
                  @Timed(
                      name = "customMetricName",
                      rateUnit = TimeUnit.SECONDS,
                      durationUnit = TimeUnit.MILLISECONDS
                  )
                  public void timedMethod() {
                  }
              }
              """,
            """
              package com.example;

              import io.micrometer.core.annotation.Timed;

              import java.util.concurrent.TimeUnit;

              class TestClass {
                  @Timed(
                      value = "customMetricName")
                  public void timedMethod() {
                  }
              }
              """
          )
        );
    }
}

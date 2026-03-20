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
package org.openrewrite.java.dropwizard.health;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateHealthCheckMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateHealthCheckMethod())
          .parser(JavaParser.fromJavaVersion().classpath(
            "metrics-healthchecks", "spring-boot-actuator"))
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void migratesCheckMethodSignature() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;

              public class MyHealthCheck extends HealthCheck {
                  @Override
                  protected HealthCheck.Result check() {
                      return Result.healthy();
                  }
              }
              """,
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;
              import org.springframework.boot.actuate.health.Health;

              public class MyHealthCheck extends HealthCheck {
                  @Override
                  public Health health() {
                      return  Health.up().build();
                  }
              }
              """
          ));
    }

    @Test
    void migratesUnhealthyResult() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;

              public class MyHealthCheck extends HealthCheck {
                  @Override
                  protected HealthCheck.Result check() {
                      return Result.unhealthy("something is wrong");
                  }
              }
              """,
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;
              import org.springframework.boot.actuate.health.Health;

              public class MyHealthCheck extends HealthCheck {
                  @Override
                  public Health health() {
                      return  Health.down().build();
                  }
              }
              """
          ));
    }

    @Test
    void wrapsInTryCatchWhenMethodThrows() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;

              public class DbHealthCheck extends HealthCheck {
                  @Override
                  protected HealthCheck.Result check() throws Exception {
                      doSomethingRisky();
                      return Result.healthy();
                  }

                  private void doSomethingRisky() throws Exception {
                  }
              }
              """,
            """
              package com.example;

              import com.codahale.metrics.health.HealthCheck;
              import org.springframework.boot.actuate.health.Health;

              public class DbHealthCheck extends HealthCheck {
                  @Override
                  public Health health() { try {
                      doSomethingRisky();
                      return  Health.up().build();
                  } catch (Exception e) { return Health.down(e).build(); } }

                  private void doSomethingRisky() throws Exception {
                  }
              }
              """
          ));
    }

    @Test
    void doesNotModifyNonHealthCheckClass() {
        rewriteRun(
          java(
            """
              package com.example;

              public class PlainClass {
                  protected Object check() {
                      return null;
                  }
              }
              """
          ));
    }
}

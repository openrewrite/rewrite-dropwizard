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
package org.openrewrite.java.dropwizard.lifecycle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateManagedLifecycleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateManagedLifecycle())
          .parser(JavaParser.fromJavaVersion().classpath("dropwizard-lifecycle"))
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void migratesManagedStartStop() {
        rewriteRun(
          java(
            """
              package com.example;

              import io.dropwizard.lifecycle.Managed;

              public class MyService implements Managed {
                  @Override
                  public void start() throws Exception {
                      // initialize
                  }

                  @Override
                  public void stop() throws Exception {
                      // cleanup
                  }
              }
              """,
            """
              package com.example;

              import jakarta.annotation.PostConstruct;
              import jakarta.annotation.PreDestroy;
              import org.springframework.stereotype.Component;

              @Component
              public class MyService {
                  @PostConstruct
                  public void start() throws Exception {
                      // initialize
                  }

                  @PreDestroy
                  public void stop() throws Exception {
                      // cleanup
                  }
              }
              """
          ));
    }

    @Test
    void doesNotModifyClassWithoutManaged() {
        rewriteRun(
          java(
            """
              package com.example;

              public class PlainService {
                  public void start() {
                      // initialize
                  }
              }
              """
          ));
    }

    @Test
    void preservesOtherMethods() {
        rewriteRun(
          java(
            """
              package com.example;

              import io.dropwizard.lifecycle.Managed;

              public class MyService implements Managed {
                  @Override
                  public void start() throws Exception {
                      // initialize
                  }

                  @Override
                  public void stop() throws Exception {
                      // cleanup
                  }

                  public void doWork() {
                      // business logic
                  }
              }
              """,
            """
              package com.example;

              import jakarta.annotation.PostConstruct;
              import jakarta.annotation.PreDestroy;
              import org.springframework.stereotype.Component;

              @Component
              public class MyService {
                  @PostConstruct
                  public void start() throws Exception {
                      // initialize
                  }

                  @PreDestroy
                  public void stop() throws Exception {
                      // cleanup
                  }

                  public void doWork() {
                      // business logic
                  }
              }
              """
          ));
    }
}

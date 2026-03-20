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
package org.openrewrite.java.dropwizard.test;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DropwizardExtensionsJUnit5ToSpringBootTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DropwizardExtensionsJUnit5ToSpringBoot());
    }

    @DocumentExample
    @Test
    void convertDropwizardAppExtension() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .dependsOn(
                  """
                    package io.dropwizard;
                    public class Configuration {}
                    """,
                  """
                    package io.dropwizard.core;
                    public class Application<T> {}
                    """,
                  """
                    package io.dropwizard.testing.junit5;
                    public class DropwizardAppExtension<C extends io.dropwizard.Configuration> {
                        public DropwizardAppExtension(Class<?> appClass, String configPath) {}
                    }
                    """,
                  """
                    package io.dropwizard.testing.junit5;
                    public class DropwizardExtensionsSupport {}
                    """,
                  """
                    package org.junit.jupiter.api.extension;
                    public @interface ExtendWith { Class<?>[] value(); }
                    """)),
          java(
            """
              import io.dropwizard.testing.junit5.DropwizardAppExtension;
              import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
              import org.junit.jupiter.api.extension.ExtendWith;

              @ExtendWith(DropwizardExtensionsSupport.class)
              class MyAppIT {
                  private static final DropwizardAppExtension<?> APP =
                          new DropwizardAppExtension<>(Object.class, "config.yml");

                  // test methods...
              }
              """,
            """
              import org.springframework.boot.test.context.SpringBootTest;

              @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
              class MyAppIT {

                  // test methods...
              }
              """
          ));
    }

    @Test
    void doesNotMakeChangesWithExistingSpringBootTest() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .classpath("spring-boot-test")
                .dependsOn(
                  """
                    package io.dropwizard;
                    public class Configuration {}
                    """,
                  """
                    package io.dropwizard.testing.junit5;
                    public class DropwizardAppExtension<C extends io.dropwizard.Configuration> {
                        public DropwizardAppExtension(Class<?> appClass, String configPath) {}
                    }
                    """,
                  """
                    package io.dropwizard.testing.junit5;
                    public class DropwizardExtensionsSupport {}
                    """,
                  """
                    package org.junit.jupiter.api.extension;
                    public @interface ExtendWith { Class<?>[] value(); }
                    """)),
          java(
            """
              import io.dropwizard.testing.junit5.DropwizardAppExtension;
              import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
              import org.junit.jupiter.api.extension.ExtendWith;
              import org.springframework.boot.test.context.SpringBootTest;

              @SpringBootTest
              @ExtendWith(DropwizardExtensionsSupport.class)
              class MyAppIT {
                  private static final DropwizardAppExtension<?> APP =
                          new DropwizardAppExtension<>(Object.class, "config.yml");

                  // test methods...
              }
              """
          ));
    }
}

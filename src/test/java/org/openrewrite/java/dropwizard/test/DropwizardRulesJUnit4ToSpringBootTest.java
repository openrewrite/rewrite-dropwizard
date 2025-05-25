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

class DropwizardRulesJUnit4ToSpringBootTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DropwizardRulesJUnit4ToSpringBoot());
    }

    @DocumentExample
    @Test
    void convertDropwizardAppRule() {
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
                    package io.dropwizard.testing.junit;
                    public class DropwizardAppRule<C extends io.dropwizard.Configuration> {
                        public DropwizardAppRule(Object object) {}
                    }
                    """,
                  "package org.junit; public @interface ClassRule {}")),
          java(
            """
              import io.dropwizard.testing.junit.DropwizardAppRule;
              import org.junit.ClassRule;

              class MyAppTest {
                  @ClassRule
                  public static final DropwizardAppRule<?> RULE = new DropwizardAppRule<>(Object.class);

                  // test methods...
              }
              """,
            """
              import org.springframework.boot.test.context.SpringBootTest;

              @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
              class MyAppTest {

                  // test methods...
              }
              """));
    }

    @Test
    void convertResourceTestRule() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .dependsOn(
                  """
                    package io.dropwizard.testing.junit;
                    public class ResourceTestRule {
                        public static Builder builder() {
                            return new Builder();
                        }

                        public static class Builder {
                            public Builder addResource(Object resource) {
                                return this;
                            }

                            public ResourceTestRule build() {
                                return new ResourceTestRule();
                            }
                        }
                    }
                    """,
                  """
                    package com.example;
                    public class MyResource {
                        public MyResource() {}
                    }
                    """,
                  "package org.junit; public @interface Rule {}")),
          java(
            """
              import io.dropwizard.testing.junit.ResourceTestRule;
              import org.junit.Rule;

              class MyResourceTest {
                  @Rule
                  public ResourceTestRule resources = ResourceTestRule.builder()
                      .addResource(new com.example.MyResource())
                      .build();

                  // test methods...
              }
              """,
            """
              import org.springframework.boot.test.context.SpringBootTest;

              @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
              class MyResourceTest {

                  // test methods...
              }
                """));
    }

    @Test
    void convertDAOTestRule() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .dependsOn(
                  """
                    package io.dropwizard.testing.junit;
                    public class DAOTestRule {
                        private DAOTestRule() {}
                        public static Builder newBuilder() {
                            return new Builder();
                        }
                        public static class Builder {
                            public Builder addEntityClass(Class<?> entityClass) {
                                return this;
                            }
                            public DAOTestRule build() {
                                return new DAOTestRule();
                            }
                        }
                    }
                    """,
                  """
                    package com.example;
                    public class MyEntity {}
                    """,
                  "package org.junit; public @interface Rule {}")),
          java(
            """
              import io.dropwizard.testing.junit.DAOTestRule;
              import org.junit.Rule;

              class MyDAOTest {
                  @Rule
                  public DAOTestRule database = DAOTestRule.newBuilder()
                      .addEntityClass(com.example.MyEntity.class)
                      .build();

                  // test methods...
              }
              """,
            """
                  import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

                  @DataJpaTest
                  class MyDAOTest {

                      // test methods...
                  }
              """));
    }

    @Test
    void convertDropwizardClientRule() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .dependsOn(
                  """
                    package io.dropwizard.testing.junit;
                    public class DropwizardClientRule {
                        public DropwizardClientRule(Object object) {}
                    }
                    """,
                  """
                    package com.example;
                    public class MyResource {}
                    """,
                  "package org.junit; public @interface Rule {}")),
          java(
            """
              import io.dropwizard.testing.junit.DropwizardClientRule;
              import org.junit.Rule;

              class MyClientTest {
                  @Rule
                  public DropwizardClientRule clientRule = new DropwizardClientRule(new com.example.MyResource());

                  // test methods...
              }
              """,
            """
                import org.springframework.boot.test.context.SpringBootTest;

                @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
                class MyClientTest {

                    // test methods...
                }
              """));
    }

    @Test
    void doesNotMakeChangesWithExistingAnnotations() {
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
                    package io.dropwizard.testing.junit;
                    public class DropwizardAppRule<C extends io.dropwizard.Configuration> {
                        public DropwizardAppRule(Object object) {}
                    }
                    """,
                  "package org.junit; public @interface ClassRule {}")),
          java(
            """
              import io.dropwizard.testing.junit.DropwizardAppRule;
              import org.junit.ClassRule;
              import org.springframework.boot.test.context.SpringBootTest;

              @SpringBootTest
              class MyAppTest {
                  @ClassRule
                  public static final DropwizardAppRule<?> RULE = new DropwizardAppRule<>(Object.class);

                  // test methods...
              }
              """));
    }
}

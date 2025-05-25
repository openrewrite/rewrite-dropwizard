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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DaoTestRuleLambdaExtractorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            new MethodLambdaExtractor(
              "io.dropwizard.DaoTestRule", "*..DaoTestRule inTransaction(..)"))
          .parser(
            JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  package io.dropwizard;

                  import java.util.function.Supplier;
                  import java.lang.Runnable;

                  public class DaoTestRule {
                      public void inTransaction(Runnable operation) {}
                      public <T> T inTransaction(Supplier<T> operation) {
                          return null;
                      }
                  }
                  """,
                """
                  class Person {
                      private String fullName;
                      private String jobTitle;

                      public Person(String fullName, String jobTitle) {
                          this.fullName = fullName;
                          this.jobTitle = jobTitle;
                      }
                  }
                    """,
                """
                  import java.util.List;
                  import java.util.Optional;

                  interface PersonDAO {
                      Person create(Person person);
                      List<Person> findAll();
                      Optional<Person> findById(long id);
                  }
                  """));
    }

    @Test
    void doNotTransformSimpleLambda() {
        rewriteRun(
          java(
            """
              import javax.transaction.Transactional;

              class Test {
                  public void test() {
                      String result = "test";
                  }
              }
              """));
    }

    @Test
    void transformSimpleLambda() {
        rewriteRun(
          java(
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;

                  public void test() {
                      String result = daoTestRule.inTransaction(() -> "test");
                  }
              }
              """,
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;

                  public void test() {
                      String result = "test";
                  }
              }
              """));
    }

    @Test
    void transformSimpleLambdaBlock() {
        rewriteRun(
          java(
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;

                  public void test() {
                      String result = daoTestRule.inTransaction(() -> {
                           return "test";
                      });
                  }
              }
              """,
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;

                  public void test() {
                      String result = "test";
                  }
              }
              """));
    }

    @Test
    void transformsLambdasInMethodCalls() {
        rewriteRun(
          java(
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;
                  private PersonDAO personDAO;

                  @Test
                  public void test() {
                      daoTestRule.inTransaction(() -> {
                          personDAO.create(new Person("Jeff", "The plumber"));
                          personDAO.create(new Person("Jim", "The cook"));
                      });

                      final Person jeff = personDAO.create(new Person("Jeff", "The plumber"));
                      final List<Person> persons = personDAO.findAll();
                  }
              }
              """,
            """
              import io.dropwizard.DaoTestRule;

              class Test {
                  private DaoTestRule daoTestRule;
                  private PersonDAO personDAO;

                  @Test
                  public void test() {
                      personDAO.create(new Person("Jeff", "The plumber"));
                      personDAO.create(new Person("Jim", "The cook"));

                      final Person jeff = personDAO.create(new Person("Jeff", "The plumber"));
                      final List<Person> persons = personDAO.findAll();
                  }
              }
                """));
    }
}

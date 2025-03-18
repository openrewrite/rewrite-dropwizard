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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddMethodAnnotationIfAnnotationExistsTest implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(
            new AddMethodAnnotationIfAnnotationExists(
                "org.lombok.SneakyThrows", "org.junit.jupiter.api.Test"))
        .parser(JavaParser.fromJavaVersion().classpath("lombok", ""));
  }

  @DocumentExample
  @Test
  void addsAnnotationWhenTargetExists() {
    rewriteRun(
        java(
          //language=java

          """
              import org.junit.jupiter.api.Test;

              class TestClass {
                  @Test
                  void testMethod() {}
              }
              """,
          //language=java
          """
              import org.junit.jupiter.api.Test;

              class TestClass {
                  @org.lombok.SneakyThrows
                  @Test
                  void testMethod() {
                  }
              }
              """));
  }

    @Test
    void doesNotAddWhenNoTargetExists() {
        rewriteRun(
          //language=java
          java(
            """
              public class Customer {
                  private String name;
              }
              """
          )
        );
    }
}

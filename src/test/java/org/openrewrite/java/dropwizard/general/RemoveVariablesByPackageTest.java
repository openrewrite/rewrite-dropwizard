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
package org.openrewrite.java.dropwizard.general;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveVariablesByPackageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveVariablesByPackage("java.lang", false));
    }

    @Test
    void removesMatchingClassVariables() {
        rewriteRun(
          java(
            """
              package com.example;

              class TestClass {
                  private String shouldBeRemoved = "test";
                  private static final int STAYS = 42;

                  public void method() {
                      String localVar = "REMOVED";
                  }
              }
              """,
            """
              package com.example;

              class TestClass {
                  private static final int STAYS = 42;

                  public void method() {
                  }
              }
              """));
    }

    @Test
    void preservesNonMatchingPackage() {
        rewriteRun(
          spec -> spec.recipe(new RemoveVariablesByPackage("java.util", false)),
          java(
            """
              package com.other;

              class TestClass {
                  private String shouldStay = "test";
                  private static final int ALSO_STAYS = 42;

                  public void method() {
                      String localVar = "stays";
                  }
              }
              """));
    }

    @Test
    void preservesMethodParameters() {
        rewriteRun(
          java(
            """
              package com.example;

              class TestClass {
                  private String remove = "test";

                  public void method(String param1, int param2) {
                      String localVar = param1 + param2;
                  }
              }
              """,
            """
              package com.example;

              class TestClass {

                  public void method(String param1, int param2) {
                  }
              }
              """));
    }

    @Test
    void removesInferredType() {
        rewriteRun(
          java(
            """
              package com.example;

              class TestClass {
                  private var inferredShouldBeRemoved = "test";
              }
              """,
            """
              package com.example;

              class TestClass {
              }
              """));
    }

    @Test
    void removesImports() {
        rewriteRun(
          spec -> spec
            .recipe(new RemoveVariablesByPackage("io.dropwizard", false))
            .parser(JavaParser.fromJavaVersion().classpath("dropwizard-testing")),
          java(
            """
              package com.example;

              import io.dropwizard.testing.ResourceHelpers;

              class TestClass {
                  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-example.yml");
                  private String otherVar = "stays";
              }
              """,
            """
              package com.example;

              class TestClass {
                  private String otherVar = "stays";
              }
              """));
    }

}

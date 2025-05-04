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

class RemoveMethodsByPackageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveMethodsByPackage("com.example.deprecated"))
          .parser(
            JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  package com.example.deprecated;
                  public class OldType {
                      private String value;
                  }
                  """));
    }

    @Test
    void removesMethodWithDeprecatedReturnType() {
        rewriteRun(
          java(
            """
              package org.example;

              import com.example.deprecated.OldType;

              class Test {
                  public OldType methodToRemove() {
                      return null;
                  }

                  public String methodToKeep() {
                      return "test";
                  }
              }
              """,
            """
              package org.example;

              class Test {

                  public String methodToKeep() {
                      return "test";
                  }
              }
              """));
    }

    @Test
    void removesMethodWithDeprecatedParameter() {
        rewriteRun(
          java(
            """
              package org.example;

              import com.example.deprecated.OldType;

              class Test {
                  public void methodToRemove(OldType param) {
                  }

                  public void methodToKeep(String param) {
                  }
              }
              """,
            """
              package org.example;

              class Test {

                  public void methodToKeep(String param) {
                  }
              }
              """));
    }

    @Test
    void preservesMethodsWithoutDeprecatedTypes() {
        rewriteRun(
          java(
            """
              package org.example;

              class Test {
                  public String method1() {
                      return "";
                  }

                  public void method2(int param) {
                  }
              }
              """));
    }
}

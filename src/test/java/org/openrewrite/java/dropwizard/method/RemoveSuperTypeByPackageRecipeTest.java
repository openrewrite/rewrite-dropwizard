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
package org.openrewrite.java.dropwizard.method;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveSuperTypeByPackageRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveSuperTypeByPackage("com.example"));
    }

    @Test
    void removesExtends() {
        rewriteRun(
          java(
            """
              package com.example;

              class BaseClass {
                  protected void baseMethod() {}
              }
              """),
          java(
            """
              package com.example;

              class ChildClass extends BaseClass {
                  void someMethod() {
                      baseMethod();
                  }
              }
              """,
            """
              package com.example;

              class ChildClass {
                  void someMethod() {
                      baseMethod();
                  }
              }
              """));
    }

    @Test
    void removesInterface() {
        rewriteRun(
          java(
            """
              package com.example;

              interface MyInterface {
                  void interfaceMethod();
              }
              """),
          java(
            """
              package com.example;

              class TestClass implements MyInterface {
                  @Override
                  public void interfaceMethod() {}
                  void someMethod() {}
              }
              """,
            """
              package com.example;

              class TestClass {
                  @Override
                  public void interfaceMethod() {}
                  void someMethod() {}
              }
              """));
    }

    @Test
    void removesOneOfMultipleInterfaces() {
        rewriteRun(
          java(
            """
              package com.other.example;

              public interface InterfaceToKeep {
                  void keepMethod();
              }
              """),
          java(
            """
              package com.example;

              interface InterfaceToRemove {
                  void removeMethod();
              }
              """),
          java(
            """
              package com.example;

              import com.other.example.InterfaceToKeep;

              class TestClass implements InterfaceToKeep, InterfaceToRemove {
                  @Override
                  public void keepMethod() {}
                  @Override
                  public void removeMethod() {}
              }
              """,
            """
              package com.example;

              import com.other.example.InterfaceToKeep;

              class TestClass implements InterfaceToKeep {
                  @Override
                  public void keepMethod() {}
                  @Override
                  public void removeMethod() {}
              }
                """));
    }

    @Test
    void handlesGenericType() {
        rewriteRun(
          java(
            """
              package com.example;

              class BaseClass<T> {
                  protected T getValue() { return null; }
              }
              """),
          java(
            """
              package com.example;

              class ChildClass extends BaseClass<String> {
                  void someMethod() {
                      String value = getValue();
                  }
              }
              """,
            """
              package com.example;

              class ChildClass {
                  void someMethod() {
                      String value = getValue();
                  }
              }
              """));
    }

    @Test
    void doesNotModifyUnrelatedClasses() {
        rewriteRun(
          java(
            """
              package com.other.example;

              public class DifferentBase {
                  protected void differentMethod() {}
              }
              """),
          java(
            """
              package com.example;

              import com.other.example.DifferentBase;

              class UnrelatedClass extends DifferentBase {
                  void someMethod() {
                      differentMethod();
                  }
              }
              """));
    }
}

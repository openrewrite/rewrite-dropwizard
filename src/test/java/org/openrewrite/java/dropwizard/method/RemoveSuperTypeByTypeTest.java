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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveSuperTypeByTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveSuperTypeByType("com.example.BaseClass"));
    }

    @Test
    void removesExtends() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  protected void baseMethod() {}
              }
              """),
          java(
            """
              class ChildClass extends BaseClass {
                  void someMethod() {
                      baseMethod();
                  }
              }
              """,
            """
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
          spec -> spec.recipe(new RemoveSuperTypeByType("com.example.MyInterface")),
          java(
            """
              interface MyInterface {
                  void interfaceMethod();
              }
              """),
          java(
            """
              class TestClass implements MyInterface {
                  @Override
                  public void interfaceMethod() {}
                  void someMethod() {}
              }
              """,
            """
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
          spec -> spec.recipes(new RemoveSuperTypeByType("com.example.InterfaceToRemove")),
          java(
            """
              interface InterfaceToKeep {
                  void keepMethod();
              }
              """),
          java(
            """
              interface InterfaceToRemove {
                  void removeMethod();
              }
              """),
          java(
            """
              class TestClass implements InterfaceToKeep, InterfaceToRemove {
                  @Override
                  public void keepMethod() {}
                  @Override
                  public void removeMethod() {}
              }
              """,
            """
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
              class BaseClass<T> {
                  protected T getValue() { return null; }
              }
              """),
          java(
            """
              class ChildClass extends BaseClass<String> {
                  void someMethod() {
                      String value = getValue();
                  }
              }
              """,
            """
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
              class DifferentBase {
                  protected void differentMethod() {}
              }
              """),
          java(
            """
              class UnrelatedClass extends DifferentBase {
                  void someMethod() {
                      differentMethod();
                  }
              }
              """));
    }

    @Test
    @Disabled
    void handlesImportRemoval() {
        rewriteRun(
          java(
            """
              package com.example.base;

              public class BaseClass {
                  protected void importedMethod() {}
              }
              """),
          java(
            """
              package com.example.child;

              import com.example.base.BaseClass;

              class ChildClass extends BaseClass {
                  void someMethod() {
                      importedMethod();
                  }
              }
              """,
            """
              package com.example.child;

              class ChildClass {
                  void someMethod() {
                      importedMethod();
                  }
              }
              """));
    }
}

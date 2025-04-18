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
              package com.example;

              class BaseClass {
                  protected void baseMethod() {}
              }
              """
          ),
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
              """
          )
        );
    }

    @Test
    void removesInterface() {
        rewriteRun(
          spec -> spec.recipe(new RemoveSuperTypeByType("com.example.MyInterface")),
          java(
            """
              package com.example;

              interface MyInterface {
                  void interfaceMethod();
              }
              """
          ),
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
              """
          )
        );
    }

    @Test
    void removesOneOfMultipleInterfaces() {
        rewriteRun(
          spec -> spec.recipes(new RemoveSuperTypeByType("com.example.InterfaceToRemove")),
          java(
            """
              package com.example;

              interface InterfaceToKeep {
                  void keepMethod();
              }
              """
          ),
          java(
            """
              package com.example;

              interface InterfaceToRemove {
                  void removeMethod();
              }
              """
          ),
          java(
            """
              package com.example;

              class TestClass implements InterfaceToKeep, InterfaceToRemove {
                  @Override
                  public void keepMethod() {}
                  @Override
                  public void removeMethod() {}
              }
              """,
            """
              package com.example;

              class TestClass implements InterfaceToKeep {
                  @Override
                  public void keepMethod() {}
                  @Override
                  public void removeMethod() {}
              }
              """
          )
        );
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
              """
          ),
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
              """
          )
        );
    }

    @Test
    void doesNotModifyUnrelatedClasses() {
        rewriteRun(
          java(
            """
              package com.example;

              class DifferentBase {
                  protected void differentMethod() {}
              }
              """
          ),
          java(
            """
              package com.example;

              class UnrelatedClass extends DifferentBase {
                  void someMethod() {
                      differentMethod();
                  }
              }
              """
          )
        );
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
              """
          ),
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
              """
          )
        );
    }
}

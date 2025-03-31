package org.openrewrite.java.dropwizard.method;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveSuperTypeRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveSuperTypeRecipe("com.example.BaseClass"));
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
          spec -> spec.recipe(new RemoveSuperTypeRecipe("com.example.MyInterface")),
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
          spec -> spec.recipes(new RemoveSuperTypeRecipe("com.example.InterfaceToRemove")),
          java(
            """
              package com.example;

              interface InterfaceToKeep {
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
              package com.example;

              class DifferentBase {
                  protected void differentMethod() {}
              }
              """),
          java(
            """
              package com.example;

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

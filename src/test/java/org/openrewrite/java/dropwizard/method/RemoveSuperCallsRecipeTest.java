package org.openrewrite.java.dropwizard.method;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class RemoveSuperCallsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new RemoveUnnecessarySuperCalls())
          .typeValidationOptions(TypeValidation.builder().methodInvocations(false).build())
          .parser(
            JavaParser.fromJavaVersion()
              .classpath(JavaParser.runtimeClasspath())
              .logCompilationWarningsAndErrors(false));
    }

    @DocumentExample
    @Test
    void removesSuperCallsWhenNoSuperclass() {
        rewriteRun(
          java(
            """
              class A {
                  void method() {
                      super.method();
                      System.out.println("hello");
                  }
              }
              """,
            """
              class A {
                  void method() {
                      System.out.println("hello");
                  }
              }
              """));
    }

    @Test
    void keepsSuperCallsWhenMethodIsOverridden() {
        rewriteRun(
          java(
            """
              class Parent {
                  void method() {}
              }
              """),
          java(
            """
              class Child extends Parent {
                  @Override
                  void method() {
                      super.method();
                      System.out.println("hello");
                  }
              }
              """,
            """
              class Child extends Parent {
                  @Override
                  void method() {
                      System.out.println("hello");
                  }
              }
              """));
    }

    @Test
    void removesSuperConstructorCallWhenNoSuperclass() {
        rewriteRun(
          java(
            """
              class A {
                  public A() {
                      super();
                      System.out.println("initializing");
                  }
              }
              """,
            """
              class A {
                  public A() {
                      System.out.println("initializing");
                  }
              }
              """));
    }

    @Test
    void keepsSuperConstructorCallWhenExtendingClass() {
        rewriteRun(
          java(
            """
              class Child {
                  public Child() {
                      super();
                      System.out.println("child init");
                  }
              }
              """,
            """
              class Child {
                  public Child() {
                      System.out.println("child init");
                  }
              }
              """));
    }

    @Test
    void removesSuperConstructorCallWithArgumentsWhenNoSuperclass() {
        rewriteRun(
          spec ->
            spec.recipes(
              new ChangeSuperclassRecipe("old.Super", "new.Super", false, false, false, false),
              new RemoveUnnecessarySuperCalls()),
          java(
            """
              class A {
                  public A(String name) {
                      super("test");
                      System.out.println("Hello " + name);
                  }
              }
              """,
            """
              class A {
                  public A(String name) {
                      System.out.println("Hello " + name);
                  }
              }
              """));
    }
}

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
              new ChangeSuperType("old.Super", "new.Super", false, false, false, false),
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

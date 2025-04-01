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

import static org.openrewrite.java.Assertions.java;

class AddMissingAbstractMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddMissingAbstractMethods())
          .parser(
            JavaParser.fromJavaVersion()
              .classpath(JavaParser.runtimeClasspath())
              .logCompilationWarningsAndErrors(false));
    }

    @Test
    void implementsAbstractMethod() {
        rewriteRun(
          java(
            """
              abstract class AbstractParent {
                  abstract void doSomething(String input);
              }
              """),
          java(
            """
              class Child extends AbstractParent {
              }
              """,
            """
              class Child extends AbstractParent {
                  @Override
                  public void doSomething(java.lang.String input) {
                      throw new UnsupportedOperationException();
                  }
              }
              """));
    }

    @Test
    void implementsInterfaceMethod() {
        rewriteRun(
          java(
            """
              interface MyInterface {
                  void processData(int value);
              }
              """),
          java(
            """
              class Implementation implements MyInterface {
              }
              """,
            """
              class Implementation implements MyInterface {
                  @Override
                  public void processData(int value) {
                      throw new UnsupportedOperationException();
                  }
              }
              """));
    }

    @Test
    void implementsMultipleInterfaces() {
        rewriteRun(
          java(
            """
              interface FirstInterface {
                  void methodOne();
              }
              """),
          java(
            """
              interface SecondInterface {
                  void methodTwo(String input);
              }
              """),
          java(
            """
              class Implementation implements FirstInterface, SecondInterface {
              }
              """,
            """
              class Implementation implements FirstInterface, SecondInterface {
                  @Override
                  public void methodOne() {
                      throw new UnsupportedOperationException();
                  }

                  @Override
                  public void methodTwo(java.lang.String input) {
                      throw new UnsupportedOperationException();
                  }
              }
              """));
    }

    @Test
    void doesNotDuplicateExistingImplementation() {
        rewriteRun(
          java(
            """
              interface MyInterface {
                  void process();
              }
              """),
          java(
            """
              class Implementation implements MyInterface {
                  @Override
                  public void process() {
                      System.out.println("Processing");
                  }
              }
              """));
    }

    @Test
    void implementsAbstractMethodWithComplexParameters() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Map;

              abstract class ComplexParent {
                  abstract <T> List<T> transform(Map<String, T> input, Class<T> type);
              }
              """),
          java(
            """
              class Child extends ComplexParent {
              }
              """,
            """
              class Child extends ComplexParent {
                  @Override
                  public <T> java.util.List<T> transform(java.util.Map<java.lang.String, T> input, java.lang.Class<T> type) {
                      throw new UnsupportedOperationException();
                  }
              }
              """));
    }

    @Test
    void shouldNotDuplicateImplementedMethods() {
        rewriteRun(
          java(
            """
              abstract class AbstractParent {
                  public abstract String getName();
              }
              """),
          java(
            """
              class Child extends AbstractParent {
                  private String name;

                  public String getName() {
                      return name;
                  }
              }
              """));
    }

    @Test
    void shouldNotAddAnyDefaultOrObjectMethods() {
        rewriteRun(
          java(
            """
              import java.security.Principal;
              import java.util.Set;

              public class User implements Principal {
                  private final String name;

                  private final Set<String> roles;

                  public User(String name) {
                      this.name = name;
                      this.roles = null;
                  }

                  public User(String name, Set<String> roles) {
                      this.name = name;
                      this.roles = roles;
                  }

                  public String getName() {
                      return name;
                  }

                  public int getId() {
                      return (int) (Math.random() * 100);
                  }

                  public Set<String> getRoles() {
                      return roles;
                  }
              }
              """));
    }

    @DocumentExample
    @Test
    void shouldTransformCommandClass() {
        rewriteRun(
          spec ->
            spec.recipes(new AddMissingAbstractMethods())
              .parser(
                JavaParser.fromJavaVersion()
                  .classpath("")
                  .logCompilationWarningsAndErrors(false)),
          java(
            """
              import org.springframework.boot.CommandLineRunner;

              public class RenderCommand implements CommandLineRunner {
              }
              """,
            """
              import org.springframework.boot.CommandLineRunner;

              public class RenderCommand implements CommandLineRunner {
                  @Override
                  public void run(java.lang.String[] args) throws java.lang.Exception {
                      throw new UnsupportedOperationException();
                  }
              }
              """));
    }
}

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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.openrewrite.java.Assertions.java;
import static org.assertj.core.api.Assertions.assertThat;

class RemoveUnnecessaryOverrideTest implements RewriteTest {

    @Test
    void concurrentRunsUseIndependentAnnotationVisitors() throws Exception {
        StringBuilder methods = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            methods.append("@Override public void missing").append(i)
              .append("(String value) { System.out.println(value.trim()); }\n");
        }
        J.CompilationUnit source = (J.CompilationUnit) JavaParser.fromJavaVersion().build()
          .parse("class Example {\n" + methods + "}").findFirst().orElseThrow();
        RemoveUnnecessaryOverride recipe = new RemoveUnnecessaryOverride(false);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                results.add(workers.submit(() -> {
                    start.await();
                    for (int run = 0; run < 4; run++) {
                        J.CompilationUnit after = (J.CompilationUnit) recipe.getVisitor()
                          .visitNonNull(source, new InMemoryExecutionContext());
                        assertThat(after.printAll()).doesNotContain("@Override");
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                result.get();
            }
        } finally {
            workers.shutdownNow();
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new RemoveUnnecessaryOverride(false));
    }

    @DocumentExample
    @Test
    void configuresRenderCommandArguments() {
        rewriteRun(
          spec ->
            spec.parser(JavaParser.fromJavaVersion().classpath("argparse4j", "spring-boot")),
          java(
            """
              import net.sourceforge.argparse4j.impl.Arguments;
              import net.sourceforge.argparse4j.inf.Subparser;
              import org.springframework.boot.CommandLineRunner;

              public class RenderCommand implements CommandLineRunner {
                  @Override
                  public void configure(Subparser subparser) {
                      subparser.addArgument("-i", "--include-default")
                               .action(Arguments.storeTrue())
                               .dest("include-default")
                               .help("Also render the template with the default name");
                      subparser.addArgument("names").nargs("*");
                  }
              }
              """,
            """
              import net.sourceforge.argparse4j.impl.Arguments;
              import net.sourceforge.argparse4j.inf.Subparser;
              import org.springframework.boot.CommandLineRunner;

              public class RenderCommand implements CommandLineRunner {
                  public void configure(Subparser subparser) {
                      subparser.addArgument("-i", "--include-default")
                               .action(Arguments.storeTrue())
                               .dest("include-default")
                               .help("Also render the template with the default name");
                      subparser.addArgument("names").nargs("*");
                  }
              }
              """
          )
        );
    }

    @Test
    void removesUnnecessaryOverride() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  void baseMethod() {}
              }
              """
          ),
          java(
            """
              class TestClass extends BaseClass {
                  @Override
                  void nonExistentMethod() {}
              }
              """,
            """
              class TestClass extends BaseClass {
                  void nonExistentMethod() {}
              }
              """
          )
        );
    }

    @Test
    void keepsValidOverride() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  void baseMethod() {}
              }
              """
          ),
          java(
            """
              class TestClass extends BaseClass {
                  @Override
                  void baseMethod() {}
              }
              """
          )
        );
    }

    @Test
    void handlesMultipleAnnotations() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  void someMethod() {}
              }
              """
          ),
          java(
            """
              class TestClass extends BaseClass {
                  @Deprecated
                  @Override
                  void wrongMethod() {}
              }
              """,
            """
              class TestClass extends BaseClass {
                  @Deprecated
                  void wrongMethod() {}
              }
              """
          )
        );
    }

    @Test
    void handlesInterfaceImplementation() {
        rewriteRun(
          java(
            """
              interface MyInterface {
                  void correctMethod();
                  void anotherMethod();
              }
              """
          ),
          java(
            """
              class TestClass implements MyInterface {
                  @Override
                  public void correctMethod() {}

                  @Override
                  public void wrongMethod() {}

                  @Override
                  public void anotherMethod() {}
              }
              """,
            """
              class TestClass implements MyInterface {
                  @Override
                  public void correctMethod() {}

                  public void wrongMethod() {}

                  @Override
                  public void anotherMethod() {}
              }
              """
          )
        );
    }

    @Test
    void handlesAnonymousClass() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnnecessaryOverride(true)),
          java(
            """
              class BaseClass {
                  void baseMethod() {}
              }
              """
          ),
          java(
            """
              class TestClass {
                  void test() {
                      BaseClass anon = new BaseClass() {
                          @Override
                          void wrongMethod() {}
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesSuperInterface() {
        rewriteRun(
          java(
            """
              interface SuperInterface {
                  void superMethod();
              }

              interface SubInterface extends SuperInterface {
              }
              """
          ),
          java(
            """
              class TestClass implements SubInterface {
                  @Override
                  public void superMethod() {}

                  @Override
                  public void nonExistentMethod() {}
              }
              """,
            """
              class TestClass implements SubInterface {
                  @Override
                  public void superMethod() {}

                  public void nonExistentMethod() {}
              }
              """
          )
        );
    }

    @Test
    void removesOverrideFromNonexistentSuper() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(false)),
          java(
            """
              class TestClass {
                  @Override
                  public void execute() {

                  }
              }
              """,
            """
              class TestClass {
                  public void execute() {

                  }
              }
              """
          )
        );
    }

    @Test
    void removesOverrideFromChangedSuper() {
        rewriteRun(
          spec ->
            spec.recipes(
                new RemoveSuperTypeByType("ee.test.BaseClass"),
                new RemoveUnnecessaryOverride(false))
              .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(false)),
          java(
            """
              package ee.test;

              class BaseClass {
                  void baseMethod() {}
              }
              """
          ),
          java(
            """
              package ee.test;

              class TestClass extends BaseClass {
                  @Override
                  void baseMethod() {}
              }
              """,
            """
              package ee.test;

              class TestClass {
                  void baseMethod() {}
              }
              """
          )
        );
    }

    @Test
    void preservesSurvivingOverridesAfterRemovingSuperclass() {
        rewriteRun(
          spec -> spec.recipes(new RemoveSuperTypeByType("example.Base"), new RemoveUnnecessaryOverride(false))
            .cycles(3).expectedCyclesThatMakeChanges(1),
          java(
            """
              package example;

              class Base {
                  public void removed() {
                  }
              }

              interface Contract {
                  void run();
              }

              class Child extends Base implements Contract {
                  @Override
                  public void removed() {
                  }

                  @Override
                  public void run() {
                  }

                  @Override
                  public String toString() {
                      return "child";
                  }
              }
              """,
            """
              package example;

              class Base {
                  public void removed() {
                  }
              }

              interface Contract {
                  void run();
              }

              class Child implements Contract {
                  public void removed() {
                  }

                  @Override
                  public void run() {
                  }

                  @Override
                  public String toString() {
                      return "child";
                  }
              }
              """,
            source -> source.afterRecipe(cu -> {
                J.ClassDeclaration child = cu.getClasses().stream()
                  .filter(c -> c.getSimpleName().equals("Child")).findFirst().orElseThrow();
                assertThat(child.getType().getSupertype().getFullyQualifiedName()).isEqualTo("java.lang.Object");
                child.getBody().getStatements().stream()
                  .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                  .forEach(method -> assertThat(method.getMethodType().getDeclaringType().getFullyQualifiedName())
                    .isEqualTo("example.Child"));
            })
          )
        );
    }

    @Test
    void preservesGenericInterfaceAndUnrelatedClassOverrides() {
        rewriteRun(
          spec -> spec.recipes(new RemoveSuperTypeByType("example.Base"), new RemoveUnnecessaryOverride(false))
            .cycles(3).expectedCyclesThatMakeChanges(1),
          java(
            """
              package example;

              class Base {
              }

              interface Contract<T> {
                  T convert(T value);
              }

              class Child extends Base implements Contract<String> {
                  @Override
                  public String convert(String value) {
                      return value;
                  }

                  static class Nested implements Runnable {
                      @Override
                      public void run() {
                      }
                  }
              }

              class Unrelated implements Runnable {
                  @Override
                  public void run() {
                  }

                  @Override
                  public String toString() {
                      return "other";
                  }
              }
              """,
            """
              package example;

              class Base {
              }

              interface Contract<T> {
                  T convert(T value);
              }

              class Child implements Contract<String> {
                  @Override
                  public String convert(String value) {
                      return value;
                  }

                  static class Nested implements Runnable {
                      @Override
                      public void run() {
                      }
                  }
              }

              class Unrelated implements Runnable {
                  @Override
                  public void run() {
                  }

                  @Override
                  public String toString() {
                      return "other";
                  }
              }
              """,
            source -> source.afterRecipe(cu -> {
                J.ClassDeclaration child = cu.getClasses().stream()
                  .filter(c -> c.getSimpleName().equals("Child")).findFirst().orElseThrow();
                J.ClassDeclaration nested = child.getBody().getStatements().stream()
                  .filter(J.ClassDeclaration.class::isInstance).map(J.ClassDeclaration.class::cast)
                  .findFirst().orElseThrow();
                J.ClassDeclaration unrelated = cu.getClasses().stream()
                  .filter(c -> c.getSimpleName().equals("Unrelated")).findFirst().orElseThrow();
                for (J.ClassDeclaration owner : new J.ClassDeclaration[]{nested, unrelated}) {
                    owner.getBody().getStatements().stream()
                      .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                      .forEach(method -> assertThat(method.getMethodType().getDeclaringType())
                        .isSameAs(owner.getType()));
                }
            })
          )
        );
    }

    @Test
    void updatesDescendantOverridesAfterRemovingSuperclass() {
        rewriteRun(
          spec -> spec.recipes(new RemoveSuperTypeByType("example.Base"), new RemoveUnnecessaryOverride(false))
            .cycles(3).expectedCyclesThatMakeChanges(1),
          java(
            """
              package example;

              class Base {
                  public void removed() {
                  }
              }

              interface Contract<T> {
                  T convert(T value);
              }

              class Child<T> extends Base implements Contract<T> {
                  @Override
                  public T convert(T value) {
                      return value;
                  }
              }

              class Grandchild extends Child<String> {
                  @Override
                  public String convert(String value) {
                      return value;
                  }

                  @Override
                  public void removed() {
                  }

                  @Override
                  public String toString() {
                      return "grandchild";
                  }
              }
              """,
            """
              package example;

              class Base {
                  public void removed() {
                  }
              }

              interface Contract<T> {
                  T convert(T value);
              }

              class Child<T> implements Contract<T> {
                  @Override
                  public T convert(T value) {
                      return value;
                  }
              }

              class Grandchild extends Child<String> {
                  @Override
                  public String convert(String value) {
                      return value;
                  }

                  public void removed() {
                  }

                  @Override
                  public String toString() {
                      return "grandchild";
                  }
              }
              """,
            source -> source.afterRecipe(cu -> {
                J.ClassDeclaration grandchild = cu.getClasses().stream()
                  .filter(c -> c.getSimpleName().equals("Grandchild")).findFirst().orElseThrow();
                JavaType.FullyQualified parent = grandchild.getType().getSupertype();
                assertThat(parent).isInstanceOf(JavaType.Parameterized.class);
                JavaType.Parameterized parameterized = (JavaType.Parameterized) parent;
                assertThat(parameterized.getType().getFullyQualifiedName()).isEqualTo("example.Child");
                assertThat(parameterized.getTypeParameters()).containsExactly(JavaType.buildType("java.lang.String"));
                assertThat(parameterized.getSupertype().getFullyQualifiedName()).isEqualTo("java.lang.Object");
                grandchild.getBody().getStatements().stream()
                  .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                  .forEach(method -> assertThat(method.getMethodType().getDeclaringType().getSupertype())
                    .isSameAs(parent));
            })
          )
        );
    }
}

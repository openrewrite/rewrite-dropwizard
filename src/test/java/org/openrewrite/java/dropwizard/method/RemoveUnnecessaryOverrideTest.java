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

class RemoveUnnecessaryOverrideTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new RemoveUnnecessaryOverride(false));
    }

    @Test
    void removesUnnecessaryOverride() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  void baseMethod() {}
              }
              """),
          java(
            """
              class TestClass extends BaseClass {
                  @Override
                  void nonExistentMethod() {}
              }
              """,
            """
              class TestClass extends BaseClass {

                  void nonExistentMethod() {
                  }
              }
              """
          )
        );
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
    void keepsValidOverride() {
        rewriteRun(
          java(
            """
              class BaseClass {
                  void baseMethod() {}
              }
              """),
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
              """),
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
                  void wrongMethod() {
                  }
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
              """),
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


                  public void wrongMethod() {
                  }

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
              """),
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
              """),
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


                  public void nonExistentMethod() {
                  }
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
              """),
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

                  void baseMethod() {
                  }
              }
              """
          )
        );
    }
}

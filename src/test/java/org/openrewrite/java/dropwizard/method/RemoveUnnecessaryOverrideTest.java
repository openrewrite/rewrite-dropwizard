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
              """));
    }

    @DocumentExample
    @Test
    void configuresRenderCommandArguments() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion()
                .classpath(JavaParser.runtimeClasspath())
                .logCompilationWarningsAndErrors(false)),
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
              """));
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
              """));
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
              """));
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
              """));
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
              """));
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
              """));
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
              """));
    }

    @Test
    void removesOverrideFromChangedSuper() {
        rewriteRun(
          spec ->
            spec.recipes(
                new RemoveSuperTypeRecipe("ee.test.BaseClass"),
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
              """));
    }
}

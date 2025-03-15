package org.openrewrite.dropwizard;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddAnnotationIfAnnotationExistsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void addsAnnotationWhenTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          java(
            """
                  import javax.persistence.Table;

                  @Table
                  public class Customer {
                      private String name;
                  }
              """,
            """
                  import javax.persistence.Entity;
                  import javax.persistence.Table;

                  @Entity
                  @Table
                  public class Customer {
                      private String name;
                  }
              """));
    }

    @Test
    void addsAnnotationWithParametersWhenTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity(name = \"customer\")",
                "javax.persistence.Table",
                false)),
          java(
            """
              import javax.persistence.Table;

              @Table
              public class Customer {
                  private String name;
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Table;

              @Entity(name = "customer")
              @Table
              public class Customer {
                  private String name;
              }
              """));
    }

    @Test
    void doesNotAddWhenNoTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          java(
            """
              public class Customer {
                  private String name;
              }
              """));
    }

    @Test
    void doesNotAddWhenAnnotationAlreadyExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          java(
            """
                  import javax.persistence.Entity;
                  import javax.persistence.Table;

                  @Entity
                  @Table
                  public class Customer {
                      private String name;
                  }
              """));
    }

    @Test
    void handlesFullyQualifiedAnnotations() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          java(
            """
              @javax.persistence.Table
              public class Customer {
                  private String name;
              }
              """,
            """
                  import javax.persistence.Entity;
                  import javax.persistence.Table;

                  @Entity
                  @Table
                  public class Customer {
                      private String name;
                  }
              """));
    }

    @Test
    void handlesInnerClasses() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfAnnotationExistsRecipe(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          java(
            """
                  import javax.persistence.Table;

                  public class Outer {
                      @Table
                      public class Inner {
                          private String name;
                      }
                  }
              """,
            """
                  import javax.persistence.Entity;
                  import javax.persistence.Table;

                  public class Outer {
                      @Entity
                      @Table
                      public class Inner {
                          private String name;
                      }
                  }
              """));
    }
}

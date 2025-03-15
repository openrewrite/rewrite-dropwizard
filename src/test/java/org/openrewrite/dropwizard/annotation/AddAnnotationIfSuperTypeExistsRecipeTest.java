package org.openrewrite.dropwizard.annotation;

import org.junit.jupiter.api.Test;
import org.openrewrite.dropwizard.annotation.AddAnnotationIfSuperTypeExistsRecipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddAnnotationIfSuperTypeExistsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void addsAnnotationWhenExtendsMatch() {
        rewriteRun(
          spec ->
            spec
              .recipe(
                new AddAnnotationIfSuperTypeExistsRecipe(
                  "javax.persistence.Entity", "java.util.AbstractList", false)),
          java(
            """
              import java.util.AbstractList;

              public class CustomList extends AbstractList<String> {
                  @Override
                  public String get(int index) {
                      return null;
                  }

                  @Override
                  public int size() {
                      return 0;
                  }
              }
              """,
            """
              import javax.persistence.Entity;

              import java.util.AbstractList;

              @Entity
              public class CustomList extends AbstractList<String> {
                  @Override
                  public String get(int index) {
                      return null;
                  }

                  @Override
                  public int size() {
                      return 0;
                  }
              }
              """));
    }

    @Test
    void addsAnnotationWhenImplementsMatch() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          java(
            """
              import java.io.Serializable;

              public class Customer implements Serializable {
                  private String name;
              }
              """,
            """
              import javax.persistence.Entity;

              import java.io.Serializable;

              @Entity
              public class Customer implements Serializable {
                  private String name;
              }
              """));
    }

    @Test
    void addsAnnotationWhenMultipleImplements() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          java(
            """
              import java.io.Serializable;
              import java.lang.Cloneable;

              public class Customer implements Cloneable, Serializable {
                  private String name;
              }
              """,
            """
              import javax.persistence.Entity;

              import java.io.Serializable;
              import java.lang.Cloneable;

              @Entity
              public class Customer implements Cloneable, Serializable {
                  private String name;
              }
              """));
    }

    @Test
    void doesNotAddWhenNoMatch() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          java(
            """
              public class Customer implements Cloneable {
                  private String name;
              }
              """));
    }

    @Test
    void doesNotAddWhenAlreadyAnnotated() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          java(
            """
              import java.io.Serializable;
              import javax.persistence.Entity;

              @Entity
              public class Customer implements Serializable {
                  private String name;
              }
              """));
    }

    @Test
    void handlesInnerClassesRegular() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          java(
            """
              import java.io.Serializable;

              public class Outer {
                  public class Inner implements Serializable {
                      private String name;
                  }
              }
              """,
            """
              import javax.persistence.Entity;

              import java.io.Serializable;

              public class Outer {
                  @Entity
                  public class Inner implements Serializable {
                      private String name;
                  }
              }
              """));
    }

    @Test
    void handlesInnerClasses() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", true)),
          java(
            """
              import java.io.Serializable;

              public class Outer implements Serializable {
                  public class Inner {
                      private String name;
                  }
              }
              """,
            """
              import javax.persistence.Entity;

              import java.io.Serializable;

              @Entity
              public class Outer implements Serializable {
                  @Entity
                  public class Inner {
                      private String name;
                  }
              }
              """));
    }

    @Test
    void handlesMultipleSupertypeTargets() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.lang.Cloneable", false)),
          java(
            """
              public class Customer implements Cloneable {
                  private String name;
              }
              """,
            """
              import javax.persistence.Entity;

              @Entity
              public class Customer implements Cloneable {
                  private String name;
              }
              """));
    }

    @Test
    void handlesGenericTypes() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.util.List", false)),
          java(
            """
              import java.util.List;

              public class StringList implements List<String> {
              }
              """,
            """
              import javax.persistence.Entity;

              import java.util.List;

              @Entity
              public class StringList implements List<String> {
              }
              """));
    }
}

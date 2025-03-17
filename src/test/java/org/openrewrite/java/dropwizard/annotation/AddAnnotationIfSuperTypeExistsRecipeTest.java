/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.dropwizard.annotation;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddAnnotationIfSuperTypeExistsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @DocumentExample
    @Test
    void addsAnnotationWhenExtendsMatch() {
        rewriteRun(
          spec ->
            spec
              .recipe(
                new AddAnnotationIfSuperTypeExistsRecipe(
                  "javax.persistence.Entity", "java.util.AbstractList", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void addsAnnotationWhenImplementsMatch() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void addsAnnotationWhenMultipleImplements() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void doesNotAddWhenNoMatch() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          //language=java
          java(
            """
              public class Customer implements Cloneable {
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void doesNotAddWhenAlreadyAnnotated() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          //language=java
          java(
            """
              import java.io.Serializable;
              import javax.persistence.Entity;

              @Entity
              public class Customer implements Serializable {
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void handlesInnerClassesRegular() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void handlesInnerClasses() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.io.Serializable", true)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void handlesMultipleSupertypeTargets() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.lang.Cloneable", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void handlesGenericTypes() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddAnnotationIfSuperTypeExistsRecipe(
                "javax.persistence.Entity", "java.util.List", false)),
          //language=java
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
              """
          )
        );
    }
}

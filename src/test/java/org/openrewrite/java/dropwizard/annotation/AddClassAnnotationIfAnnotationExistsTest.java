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
package org.openrewrite.java.dropwizard.annotation;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddClassAnnotationIfAnnotationExistsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("javax.persistence-api"));
    }

    @DocumentExample
    @Test
    void addsAnnotationWhenTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void addsAnnotationWithParametersWhenTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity(name = \"customer\")",
                "javax.persistence.Table",
                false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void doesNotAddWhenNoTargetExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          //language=java
          java(
            """
              public class Customer {
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void doesNotAddWhenAnnotationAlreadyExists() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          //language=java
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Table;
              @Entity
              @Table
              public class Customer {
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void handlesFullyQualifiedAnnotations() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          //language=java
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
              """
          )
        );
    }

    @Test
    void handlesInnerClasses() {
        rewriteRun(
          spec ->
            spec.recipe(
              new AddClassAnnotationIfAnnotationExists(
                "javax.persistence.Entity", "javax.persistence.Table", false)),
          //language=java
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
              """
          )
        );
    }
}

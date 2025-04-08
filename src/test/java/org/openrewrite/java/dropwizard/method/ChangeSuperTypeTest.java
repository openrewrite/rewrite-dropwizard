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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeSuperTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            new ChangeSuperType(
              "org.example.OldParent", "org.example.NewParent", false, false, false, true))
          .parser(JavaParser.fromJavaVersion().classpath("metrics-healthchecks", "spring-boot-actuator"));
    }

    @Test
    void changesSuperclass() {
        rewriteRun(
          java(
            """
              package org.example;

              public class OldParent {
                  protected String oldField;
              }
              """),
          java(
            """
              package org.example;

              public class NewParent {
                  protected String oldField;
              }
              """),
          java(
            """
              package org.example;

              public class Child extends OldParent {
                  public void someMethod() {
                      this.oldField = "test";
                  }
              }
              """,
            """
              package org.example;

              public class Child extends org.example.NewParent {
                  public void someMethod() {
                      this.oldField = "test";
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeWhenNotMatching() {
        rewriteRun(
          java(
            """
              package org.example;

              public class DifferentParent {
              }
              """),
          java(
            """
              package org.example;

              public class Child extends DifferentParent {
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void handlesImportsCorrectly() {
        rewriteRun(
          spec ->
            spec.recipes(
              new ChangeSuperType(
                "java.util.Vector", "java.util.ArrayList", false, false, false, false)),
          java(
            """
              package org.example;

              public class Child extends java.util.Vector<String> {
                  public void someMethod() {
                      this.add("test");
                  }
              }
              """,
            """
              package org.example;

              import java.util.ArrayList;

              public class Child extends ArrayList {
                  public void someMethod() {
                      this.add("test");
                  }
              }
              """
          )
        );
    }

    @Test
    void changesGenerics() {
        rewriteRun(
          spec ->
            spec.recipes(
              new ChangeSuperType(
                "org.example.OldParent", "org.example.NewParent", true, false, false, false)),
          java(
            """
              package org.example;

              public class OldParent<T> {
                  protected T value;
              }
              """),
          java(
            """
              package org.example;

              public class NewParent<T> {
                  protected T value;
              }
              """),
          java(
            """
              package org.example;

              public class Child extends OldParent<String> {
                  public void setValue(String value) {
                      this.value = value;
                  }
              }
              """,
            """
              package org.example;

              public class Child extends org.example.NewParent<String> {
                  public void setValue(String value) {
                      this.value = value;
                  }
              }
              """
          )
        );
    }

    @Test
    void removesSuperCallWithOverrides() {
        rewriteRun(
          java(
            """
              package org.example;

              public class OldParent {

                  public OldParent(String test) {
                  }

                  protected void doSomething() {}
                  protected void doSomethingElse() {}
              }
              """),
          java(
            """
              package org.example;

              public class NewParent {
                  protected void doSomething() {}
                  protected void doSomethingElse() {}
              }
              """),
          java(
            """
              package org.example;

              public class Child extends OldParent {

                  public Child() {
                      super("test");
                  }

                  @Override
                  protected void doSomething() {
                      super.doSomething();
                      System.out.println("child");
                      super.doSomethingElse();
                  }
              }
              """,
            """
              package org.example;

              public class Child extends org.example.NewParent {

                  public Child() {
                  }


                  protected void doSomething() {
                      System.out.println("child");
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled
    @Deprecated
    void removesConstructorSuperCall() {
        rewriteRun(
          java(
            """
              package org.example;

              public class OldParent {
                  public OldParent(String value) {}
              }
              """),
          java(
            """
              package org.example;

              public class NewParent {
                  public NewParent(String value) {}
              }
              """),
          java(
            """
              package org.example;

              public class Child extends OldParent {
                  public Child(String value) {
                      super(value);
                      System.out.println("child constructor");
                  }
              }
              """,
            """
              package org.example;

              public class Child extends org.example.NewParent {
                  public Child(String value) {
                      System.out.println("child constructor");
                  }
              }
              """
          )
        );
    }

    @Test
    void changeExtendsToImplements() {
        rewriteRun(
          spec ->
            spec.recipe(
              new ChangeSuperType(
                "com.codahale.metrics.health.HealthCheck",
                "org.springframework.boot.actuate.health.HealthIndicator",
                false,
                true,
                false,
                true)),
          java(
            """
              import com.codahale.metrics.health.HealthCheck;

              public class TemplateHealthCheck extends HealthCheck {

                  @Override
                  protected Result check() throws Exception {
                      return Result.healthy();
                  }
              }
              """,
            """
              import org.springframework.boot.actuate.health.HealthIndicator;

              public class TemplateHealthCheck implements HealthIndicator {


                  protected Result check() throws Exception {
                      return Result.healthy();
                  }
              }
              """
          )
        );
    }
}

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
package org.openrewrite.java.dropwizard;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateApplicationRunMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateApplicationRunMethod())
          .parser(JavaParser.fromJavaVersion().classpath("dropwizard-core", "spring-boot"))
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void migratesNewAppRunToSpringApplicationRun() {
        rewriteRun(
          java(
            """
              package com.example;

              import io.dropwizard.Application;
              import io.dropwizard.Configuration;
              import io.dropwizard.setup.Environment;

              public class MyApp extends Application<Configuration> {
                  public static void main(String[] args) throws Exception {
                      new MyApp().run(args);
                  }

                  @Override
                  public void run(Configuration configuration, Environment environment) {
                  }
              }
              """,
            """
              package com.example;

              import io.dropwizard.Application;
              import io.dropwizard.Configuration;
              import io.dropwizard.setup.Environment;
              import org.springframework.boot.SpringApplication;

              public class MyApp extends Application<Configuration> {
                  public static void main(String[] args) throws Exception {
                      SpringApplication.run(MyApp.class, args);
                  }

                  @Override
                  public void run(Configuration configuration, Environment environment) {
                  }
              }
              """
          ));
    }

    @Test
    void doesNotModifyNonMainMethod() {
        rewriteRun(
          java(
            """
              package com.example;

              import io.dropwizard.Application;
              import io.dropwizard.Configuration;
              import io.dropwizard.setup.Environment;

              public class MyApp extends Application<Configuration> {
                  public static void main(String[] args) throws Exception {
                      new MyApp().run(args);
                  }

                  public void someOtherMethod() {
                      // new MyApp().run(args) would not match here since not in main
                  }

                  @Override
                  public void run(Configuration configuration, Environment environment) {
                  }
              }
              """,
            """
              package com.example;

              import io.dropwizard.Application;
              import io.dropwizard.Configuration;
              import io.dropwizard.setup.Environment;
              import org.springframework.boot.SpringApplication;

              public class MyApp extends Application<Configuration> {
                  public static void main(String[] args) throws Exception {
                      SpringApplication.run(MyApp.class, args);
                  }

                  public void someOtherMethod() {
                      // new MyApp().run(args) would not match here since not in main
                  }

                  @Override
                  public void run(Configuration configuration, Environment environment) {
                  }
              }
              """
          ));
    }

    @Test
    void doesNotModifyDifferentClassName() {
        rewriteRun(
          java(
            """
              package com.example;

              import io.dropwizard.Application;
              import io.dropwizard.Configuration;
              import io.dropwizard.setup.Environment;

              public class MyApp extends Application<Configuration> {
                  public static void main(String[] args) throws Exception {
                      new OtherApp().run(args);
                  }

                  @Override
                  public void run(Configuration configuration, Environment environment) {
                  }
              }
              """
          ));
    }
}

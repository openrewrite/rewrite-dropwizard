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
package org.openrewrite.java.dropwizard.jetty;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateJettyHandlerSignatureTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateJettyHandlerSignature())
          .parser(JavaParser.fromJavaVersion().classpath(
            "jetty-server",
            "jakarta.servlet-api"));
    }

    @DocumentExample
    @Test
    void migratesAbstractHandlerToHandlerAbstract() {
        rewriteRun(
          //language=java
          java(
            """
              import org.eclipse.jetty.server.Request;
              import org.eclipse.jetty.server.handler.AbstractHandler;
              import jakarta.servlet.http.HttpServletRequest;
              import jakarta.servlet.http.HttpServletResponse;

              public class MyHandler extends AbstractHandler {
                  @Override
                  public void handle(String target, Request baseRequest,
                                     HttpServletRequest request, HttpServletResponse response) throws Exception {
                      response.setStatus(200);
                      baseRequest.setHandled(true);
                  }
              }
              """,
            """
              import org.eclipse.jetty.server.Handler;
              import org.eclipse.jetty.server.Request;
              import org.eclipse.jetty.server.Response;
              import org.eclipse.jetty.util.Callback;
              import jakarta.servlet.http.HttpServletResponse;

              public class MyHandler extends Handler.Abstract {
                  @Override
                  public boolean handle(Request request, Response response, Callback callback) throws Exception {
                      response.setStatus(200);
                      callback.succeeded();
                  }
              }
              """
          )
        );
    }
}

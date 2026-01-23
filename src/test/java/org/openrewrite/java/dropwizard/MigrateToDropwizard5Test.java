/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.yaml.Assertions.yaml;

class MigrateToDropwizard5Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.dropwizard.MigrateToDropwizard5");
    }

    @DocumentExample
    @Test
    void upgradesDropwizardBom() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.dropwizard</groupId>
                              <artifactId>dropwizard-bom</artifactId>
                              <version>4.0.10</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>io.dropwizard</groupId>
                          <artifactId>dropwizard-core</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom ->
              // The BOM version should be upgraded to 5.0.x
              assertThat(pom)
                .contains("<artifactId>dropwizard-bom</artifactId>")
                .doesNotContain("<version>4.0.10</version>")
                .actual())
          )
        );
    }

    @Test
    void upgradesExplicitDropwizardVersions() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>io.dropwizard</groupId>
                          <artifactId>dropwizard-core</artifactId>
                          <version>4.0.10</version>
                      </dependency>
                      <dependency>
                          <groupId>io.dropwizard</groupId>
                          <artifactId>dropwizard-hibernate</artifactId>
                          <version>4.0.10</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom ->
              assertThat(pom).doesNotContain("<version>4.0.10</version>").actual())
          )
        );
    }

    @Test
    void noChangeWhenAlreadyOnVersion5() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.dropwizard</groupId>
                              <artifactId>dropwizard-bom</artifactId>
                              <version>5.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removesMaxQueuedRequests() {
        rewriteRun(
          //language=yaml
          yaml(
            """
              server:
                maxQueuedRequests: 1024
                applicationConnectors:
                  - type: http
                    port: 8080
              logging:
                level: INFO
              """,
            """
              server:
                applicationConnectors:
                  - type: http
                    port: 8080
              logging:
                level: INFO
              """
          )
        );
    }
}

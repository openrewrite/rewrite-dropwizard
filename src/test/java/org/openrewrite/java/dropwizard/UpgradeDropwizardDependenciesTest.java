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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeDropwizardDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.dropwizard.UpgradeDropwizardDependencies_4_To_5");
    }

    @DocumentExample
    @Test
    void upgradesDropwizardBom() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
            spec -> spec.after(pom -> {
                // The BOM version should be upgraded to 5.0.x
                org.assertj.core.api.Assertions.assertThat(pom)
                        .contains("<artifactId>dropwizard-bom</artifactId>")
                        .doesNotContain("<version>4.0.10</version>");
                return pom;
            })
          )
        );
    }

    @Test
    void upgradesExplicitDropwizardVersions() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
            spec -> spec.after(pom -> {
                org.assertj.core.api.Assertions.assertThat(pom)
                        .doesNotContain("<version>4.0.10</version>");
                return pom;
            })
          )
        );
    }

    @Test
    void noChangeWhenAlreadyOnVersion5() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
}

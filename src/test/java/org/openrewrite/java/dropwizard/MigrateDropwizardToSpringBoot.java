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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class MigrateDropwizardToSpringBoot implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.dropwizard.MigrateDropwizardToSpringBoot");
    }

    @Test
    void run() {
        // Validates yaml recipe
        rewriteRun( // TODO We likely should not unconditionally create these files with these values!
          //language=properties
          properties(
            null,
            """
            management.endpoint.health.probes.enabled=true
            management.endpoint.health.show-components=ALWAYS
            management.endpoint.health.show-details=ALWAYS
            management.endpoints.web.base-path=/
            management.endpoints.web.exposure.include=*
            management.health.livenessstate.enabled=true
            management.health.readinessstate.enabled=true
            management.server.base-path=/management
            management.server.port=8081
            server.port=8080
            server.shutdown=graceful
            spring.application.name=my-application
            spring.datasource.driverClassName=org.h2.Driver
            spring.datasource.password=org.h2.Driver
            spring.datasource.url=jdbc:h2:mem:mydb
            spring.datasource.username=org.h2.Driver
            spring.jersey.application-path=/api
            spring.jersey.type=servlet
            spring.jpa.hibernate.ddl-auto=validate
            spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
            spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
            spring.jpa.show-sql=true
            spring.main.allow-circular-references=true
            """,
            spec -> spec.path("src/main/resources/application.properties")),
          //language=properties
          properties(
            null,
            """
              management.endpoint.health.probes.enabled=true
              management.endpoint.health.show-components=ALWAYS
              management.endpoint.health.show-details=ALWAYS
              management.endpoints.web.base-path=/
              management.endpoints.web.exposure.include=*
              management.health.livenessstate.enabled=true
              management.health.readinessstate.enabled=true
              management.server.base-path=/management
              management.server.port=8081
              server.port=8080
              server.shutdown=graceful
              spring.application.name=my-application
              spring.datasource.driverClassName=org.h2.Driver
              spring.datasource.password=org.h2.Driver
              spring.datasource.url=jdbc:h2:mem:mydb
              spring.datasource.username=org.h2.Driver
              spring.jersey.application-path=/api
              spring.jersey.type=servlet
              spring.jpa.hibernate.ddl-auto=validate
              spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
              spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
              spring.jpa.show-sql=true
              spring.main.allow-circular-references=true
              """,
            spec -> spec.path("src/test/resources/application.properties"))
        );
    }
}

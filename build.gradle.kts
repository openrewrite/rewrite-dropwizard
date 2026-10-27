plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate between Dropwizard versions. Automatically."

recipeDependencies {
    parserClasspath("org.springframework.boot:spring-boot:2.+")
    parserClasspath("org.springframework.boot:spring-boot-actuator:2.+")
    parserClasspath("org.springframework.boot:spring-boot-test:2.+")
    parserClasspath("org.springframework.boot:spring-boot-test-autoconfigure:2.+")
    parserClasspath("org.springframework:spring-core:5.+")
    parserClasspath("org.springframework:spring-web:5.+")

    testParserClasspath("com.fasterxml.jackson.core:jackson-databind:2.9.10.8")
    testParserClasspath("io.dropwizard:dropwizard-core:1.3.29")
    testParserClasspath("io.dropwizard:dropwizard-db:1.3.29")
    testParserClasspath("io.dropwizard:dropwizard-testing:1.3.29")
    testParserClasspath("io.dropwizard.metrics:metrics-annotation:4.1.+")
    testParserClasspath("io.dropwizard.metrics:metrics-healthchecks:4.1.+")
    testParserClasspath("io.micrometer:micrometer-core:1.7.+")
    testParserClasspath("jakarta.servlet:jakarta.servlet-api:5.+")
    testParserClasspath("javax.persistence:javax.persistence-api:2.2")
    testParserClasspath("javax.ws.rs:javax.ws.rs-api:2.0.1")
    testParserClasspath("net.sourceforge.argparse4j:argparse4j:0.9.0")
    testParserClasspath("org.eclipse.jetty:jetty-server:11.+")
    testParserClasspath("org.glassfish.jersey.core:jersey-client:2.25.1")
    testParserClasspath("org.mockito:mockito-core:3.+")
}

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite:rewrite-properties")
    implementation("org.openrewrite:rewrite-yaml")

    implementation("org.openrewrite.recipe:rewrite-hibernate:${rewriteVersion}")
    implementation("org.openrewrite.recipe:rewrite-migrate-java:${rewriteVersion}")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:${rewriteVersion}")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:${rewriteVersion}")
    implementation("org.openrewrite.recipe:rewrite-testing-frameworks:${rewriteVersion}")

    runtimeOnly("org.openrewrite:rewrite-java-21")

    testImplementation("org.openrewrite:rewrite-test")
}

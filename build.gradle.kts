plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate between Dropwizard versions. Automatically."

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

    implementation("org.openrewrite.recipe:rewrite-static-analysis:${rewriteVersion}")
    implementation("org.openrewrite.recipe:rewrite-testing-frameworks:${rewriteVersion}")

    runtimeOnly("org.openrewrite:rewrite-java-17")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test")

    testRuntimeOnly("io.dropwizard.metrics:metrics-annotation:4.1.+")
    testRuntimeOnly("io.dropwizard.metrics:metrics-healthchecks:4.1.+")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-actuator:2.5.+")
    testRuntimeOnly("javax.persistence:javax.persistence-api:2.2")
    testRuntimeOnly("org.projectlombok:lombok:1.18.+")
    testRuntimeOnly("net.sourceforge.argparse4j:argparse4j:0.9.0")
    testRuntimeOnly("io.dropwizard:dropwizard-testing:1.3.29")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-test:2.5.+")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-web:2.5.+")
    testRuntimeOnly("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6")


}

                                                                                                    <p align="center">

  <a href="https://docs.openrewrite.org">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-dark.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg">
      <img alt="OpenRewrite Logo" src="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg" width='600px'>
    </picture>
  </a>
</p>

<div align="center">
  <h1>rewrite-dropwizard</h1>
</div>

<div align="center">

<!-- Keep the gap above this line, otherwise they won't render correctly! -->
[![ci](https://github.com/openrewrite/rewrite-dropwizard/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-dropwizard/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-dropwizard.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://community.develocity.cloud/scans)
</div>

### This repository is archived

The Dropwizard recipes are now developed and released as part of the Moderne recipe catalog, as `io.moderne.recipe:rewrite-dropwizard`.
That module upgrades Dropwizard 4.x to 5.x and migrates Dropwizard 4.x applications to Spring Boot 3.
Version 0.10.3 of `org.openrewrite.recipe:rewrite-dropwizard` remains available on Maven Central, but will receive no further changes.

| Recipe in this repository | Replacement |
|---|---|
| `org.openrewrite.java.dropwizard.MigrateToDropwizard5` | `io.moderne.java.dropwizard.MigrateToDropwizard5` |
| `org.openrewrite.java.dropwizard.MigrateDropwizardToSpringBoot` | `io.moderne.java.dropwizard.boot.MigrateDropwizardToSpringBoot3`, which starts from Dropwizard 4.x |
| `org.openrewrite.java.dropwizard.jetty.MigrateJettyHandlerSignature` | `io.moderne.java.dropwizard.dw5.MigrateJettyHandlerSignature` |
| `org.openrewrite.java.dropwizard.{annotation,general,method,test}.*` | `io.moderne.java.dropwizard.boot.{annotation,general,method,test}.*` |

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
package org.openrewrite.java.dropwizard.general;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddMissingAnnotationImports extends Recipe {

    String displayName = "Add missing imports for annotations added by the migration";

    String description = "Ensures that annotations added during the Dropwizard-to-Spring Boot migration " +
            "have their corresponding import statements, even when the annotation nodes lack type attribution.";

    private static final Map<String, String> KNOWN_ANNOTATIONS = new HashMap<>();
    private static final Map<String, String> KNOWN_TYPES = new HashMap<>();

    static {
        KNOWN_ANNOTATIONS.put("SpringBootApplication", "org.springframework.boot.autoconfigure.SpringBootApplication");
        KNOWN_ANNOTATIONS.put("Configuration", "org.springframework.context.annotation.Configuration");
        KNOWN_ANNOTATIONS.put("ConfigurationProperties", "org.springframework.boot.context.properties.ConfigurationProperties");
        KNOWN_ANNOTATIONS.put("Component", "org.springframework.stereotype.Component");
        KNOWN_ANNOTATIONS.put("Repository", "org.springframework.stereotype.Repository");
        KNOWN_ANNOTATIONS.put("Transactional", "org.springframework.transaction.annotation.Transactional");
        KNOWN_ANNOTATIONS.put("PostConstruct", "jakarta.annotation.PostConstruct");
        KNOWN_ANNOTATIONS.put("PreDestroy", "jakarta.annotation.PreDestroy");

        KNOWN_TYPES.put("SpringApplication", "org.springframework.boot.SpringApplication");
        KNOWN_TYPES.put("Health", "org.springframework.boot.actuate.health.Health");
        KNOWN_TYPES.put("HealthIndicator", "org.springframework.boot.actuate.health.HealthIndicator");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);

                // Collect existing imports
                Set<String> importedFqns = new HashSet<>();
                Set<String> importedSimpleNames = new HashSet<>();
                for (J.Import anImport : c.getImports()) {
                    String fqn = anImport.getQualid().toString();
                    importedFqns.add(fqn);
                    int dot = fqn.lastIndexOf('.');
                    if (dot >= 0) {
                        importedSimpleNames.add(fqn.substring(dot + 1));
                    }
                }

                // Find annotations and type references used in the source
                Set<String> namesUsed = new HashSet<>();
                new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, Set<String> used) {
                        used.add(annotation.getSimpleName());
                        return super.visitAnnotation(annotation, used);
                    }

                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> used) {
                        used.add(identifier.getSimpleName());
                        return super.visitIdentifier(identifier, used);
                    }
                }.visit(c, namesUsed);

                // Add imports for known annotations that are used but not imported
                for (String name : namesUsed) {
                    String fqn = KNOWN_ANNOTATIONS.get(name);
                    if (fqn == null) {
                        fqn = KNOWN_TYPES.get(name);
                    }
                    if (fqn != null && !importedFqns.contains(fqn) && !importedSimpleNames.contains(name)) {
                        doAfterVisit(new org.openrewrite.java.AddImport<>(fqn, null, false));
                    }
                }

                return c;
            }
        };
    }
}

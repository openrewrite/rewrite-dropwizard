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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;

import static org.openrewrite.java.JavaParser.fromJavaVersion;
import static org.openrewrite.java.JavaParser.runtimeClasspath;

/**
 * Transforms the Dropwizard Application main method pattern:
 * <pre>{@code
 * new MyApp().run(args);
 * }</pre>
 * to the Spring Boot pattern:
 * <pre>{@code
 * SpringApplication.run(MyApp.class, args);
 * }</pre>
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateApplicationRunMethod extends Recipe {

    String displayName = "Migrate Dropwizard Application.run() to SpringApplication.run()";

    String description = "Replaces the `new MyApp().run(args)` pattern in the main method with " +
            "`SpringApplication.run(MyApp.class, args)`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public org.openrewrite.java.tree.J.MethodInvocation visitMethodInvocation(
                    org.openrewrite.java.tree.J.MethodInvocation method, ExecutionContext ctx) {
                org.openrewrite.java.tree.J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                // Match pattern: new SomeClass().run(args)
                if (!"run".equals(mi.getSimpleName())) {
                    return mi;
                }
                if (mi.getArguments().size() != 1) {
                    return mi;
                }

                // Check that select is new SomeClass()
                if (!(mi.getSelect() instanceof org.openrewrite.java.tree.J.NewClass)) {
                    return mi;
                }
                org.openrewrite.java.tree.J.NewClass newClass = (org.openrewrite.java.tree.J.NewClass) mi.getSelect();

                // Get the class name from the new expression
                String className = null;
                if (newClass.getClazz() instanceof org.openrewrite.java.tree.J.Identifier) {
                    className = ((org.openrewrite.java.tree.J.Identifier) newClass.getClazz()).getSimpleName();
                }
                if (className == null) {
                    return mi;
                }

                // Verify we're in a main method
                org.openrewrite.java.tree.J.MethodDeclaration enclosingMethod =
                        getCursor().firstEnclosing(org.openrewrite.java.tree.J.MethodDeclaration.class);
                if (enclosingMethod == null || !"main".equals(enclosingMethod.getSimpleName())) {
                    return mi;
                }

                // Verify the enclosing class matches the class being instantiated
                org.openrewrite.java.tree.J.ClassDeclaration enclosingClass =
                        getCursor().firstEnclosing(org.openrewrite.java.tree.J.ClassDeclaration.class);
                if (enclosingClass == null || !className.equals(enclosingClass.getSimpleName())) {
                    return mi;
                }

                // Get the argument name (typically "args")
                String argName = null;
                org.openrewrite.java.tree.Expression arg = mi.getArguments().get(0);
                if (arg instanceof org.openrewrite.java.tree.J.Identifier) {
                    argName = ((org.openrewrite.java.tree.J.Identifier) arg).getSimpleName();
                }
                if (argName == null) {
                    return mi;
                }

                // Replace with SpringApplication.run(ClassName.class, args)
                doAfterVisit(new org.openrewrite.java.AddImport<>(
                        "org.springframework.boot.SpringApplication", null, false));

                return JavaTemplate.builder("SpringApplication.run(" + className + ".class, " + argName + ")")
                        .javaParser(fromJavaVersion().classpath(runtimeClasspath()))
                        .imports("org.springframework.boot.SpringApplication")
                        .build()
                        .apply(updateCursor(mi), mi.getCoordinates().replace());
            }
        };
    }
}

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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveImportsByPackage extends Recipe {

    @Option(displayName = "Package pattern",
            description = "Imports whose fully qualified type starts with this prefix will be removed if they are unused.",
            example = "io.dropwizard")
    String packagePattern;

    String displayName = "Remove imports by package prefix";

    String description = "Removes import statements whose fully qualified type starts with the given package prefix, " +
            "but only if the imported type is not referenced elsewhere in the compilation unit.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                for (J.Import anImport : c.getImports()) {
                    String fqn = anImport.getQualid().toString();
                    if (fqn.startsWith(packagePattern)) {
                        maybeRemoveImport(fqn);
                    }
                }
                return c;
            }
        };
    }
}

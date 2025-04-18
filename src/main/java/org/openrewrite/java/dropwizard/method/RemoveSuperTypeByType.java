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
package org.openrewrite.java.dropwizard.method;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSuperTypeByType extends Recipe {

    @Option(displayName = "Fully qualified name of the superclass to remove",
            description = "Supertypes that match this name are to be removed",
            example = "io.dropwizard.Configuration")
    String typeToRemove;

    @Override
    public String getDisplayName() {
        return "Remove supertype by fully qualified name matches";
    }

    @Override
    public String getDescription() {
        return "Removes a specified type from class extends or implements clauses.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(typeToRemove, false),
                new RemoveSuperTypeVisitor() {
                    @Override
                    protected boolean shouldRemoveType(JavaType type) {
                        return isOfClassType(type, typeToRemove);
                    }
                }
        );
    }
}

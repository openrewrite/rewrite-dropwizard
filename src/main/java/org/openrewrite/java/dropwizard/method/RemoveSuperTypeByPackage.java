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

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSuperTypeByPackage extends Recipe {

    @Option(displayName = "Package to match",
            description = "Supertypes that match this package are to be removed",
            example = "io.dropwizard")
    String packageToMatch;

    @Override
    public String getDisplayName() {
        return "Remove supertypes by package";
    }

    @Override
    public String getDescription() {
        return "Removes all supertypes from a specified package in class extends or implements clauses.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(packageToMatch + ".*", false),
                new RemoveSuperTypeVisitor() {
                    @Override
                    protected boolean shouldRemoveType(JavaType type) {
                        if (type instanceof JavaType.FullyQualified) {
                            JavaType.FullyQualified fqType = (JavaType.FullyQualified) type;
                            String typePackage = fqType.getPackageName();
                            return typePackage != null && typePackage.startsWith(packageToMatch);
                        }
                        return false;
                    }
                }
        );
    }
}

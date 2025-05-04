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
package org.openrewrite.java.dropwizard.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.maven.ExcludeDependency;
import org.openrewrite.maven.RemoveDependency;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveAndExcludeDependency extends Recipe {

    @Option(
            displayName = "Group ID to remove",
            description = "The first part of a dependency coordinate to remove 'org.mockito'",
            example = "org.mockito")
    String groupId;

    @Option(
            displayName = "Artifact ID to remove",
            description = "The second part of a dependency coordinate to remove 'mockito-core'",
            example = "mockito-core")
    String artifactId;

    @Override
    public String getDisplayName() {
        return "Combined dependency management to remove and exclude";
    }

    @Override
    public String getDescription() {
        return "Combines excluding transitive dependencies and removing direct dependencies.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new RemoveDependency(groupId, artifactId, null),
                new ExcludeDependency(groupId, artifactId, null));
    }
}

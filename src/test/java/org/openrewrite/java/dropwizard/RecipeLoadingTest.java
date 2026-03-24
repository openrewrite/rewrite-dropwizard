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

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RewriteTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class RecipeLoadingTest implements RewriteTest {

    /**
     * Recipes that generate files on empty source sets always consume at least
     * 1 cycle, which conflicts with the 0-cycle expectation of
     * {@code rewriteRun} when called without source specs.
     * These recipes and their composites are tested separately in
     * {@link MigrateDropwizardToSpringBoot}.
     */
    private static final Set<String> FILE_GENERATING_RECIPES = Set.of(
            "org.openrewrite.java.dropwizard.AddMissingApplicationProperties",
            "org.openrewrite.java.dropwizard.CoreSetup",
            "org.openrewrite.java.dropwizard.MigrateDropwizardToSpringBoot"
    );

    @Test
    void recipeLoading() {
        String packageName = getClass().getPackage().getName();
        var recipes = Environment.builder()
                .scanRuntimeClasspath(packageName)
                .build()
                .listRecipes();
        assertThat(recipes).as("No recipes found in %s", packageName).isNotEmpty();
        SoftAssertions softly = new SoftAssertions();
        for (Recipe recipe : recipes) {
            if (!recipe.getName().startsWith(packageName)) {
                continue;
            }
            // Recipes with required options cannot be validated without configuration.
            // They are tested through the composite recipes that configure them.
            if (!recipe.validate().isValid()) {
                continue;
            }
            if (FILE_GENERATING_RECIPES.contains(recipe.getName())) {
                continue;
            }
            softly.assertThatCode(() -> {
                try {
                    rewriteRun(spec -> spec.recipe(recipe));
                } catch (Throwable t) {
                    fail("Recipe " + recipe.getName() + " failed to configure", t);
                }
            }).doesNotThrowAnyException();
        }
        softly.assertAll();
    }
}

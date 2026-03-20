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
package org.openrewrite.java.dropwizard.health;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Collections;
import java.util.List;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

/**
 * Transforms Dropwizard health check methods to Spring Boot HealthIndicator methods:
 * <ul>
 *   <li>{@code protected Result check()} becomes {@code public Health health()}</li>
 *   <li>{@code Result.healthy(...)} becomes {@code Health.up().build()}</li>
 *   <li>{@code Result.unhealthy(...)} becomes {@code Health.down().build()}</li>
 * </ul>
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateHealthCheckMethod extends Recipe {

    String displayName = "Migrate Dropwizard health check methods to Spring Boot HealthIndicator";

    String description = "Renames `check()` to `health()`, changes return type from `Result` to `Health`, " +
            "and transforms `Result.healthy()`/`Result.unhealthy()` calls to `Health.up().build()`/`Health.down().build()`.";

    private static final String HEALTH_CHECK_TYPE = "com.codahale.metrics.health.HealthCheck";
    private static final String HEALTH_INDICATOR_TYPE = "org.springframework.boot.actuate.health.HealthIndicator";
    private static final String HEALTH_TYPE = "org.springframework.boot.actuate.health.Health";

    private static final MethodMatcher CHECK_METHOD = new MethodMatcher(
            HEALTH_CHECK_TYPE + " check()", true);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>(HEALTH_CHECK_TYPE, false),
                        new UsesType<>(HEALTH_INDICATOR_TYPE, false)
                ),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                        // Match check() method in HealthCheck or HealthIndicator subclasses
                        if (!"check".equals(md.getSimpleName())) {
                            return md;
                        }
                        if (md.getParameters().size() != 1 || !(md.getParameters().get(0) instanceof J.Empty)) {
                            // check() should have no parameters
                            if (!md.getParameters().isEmpty() &&
                                    !(md.getParameters().size() == 1 && md.getParameters().get(0) instanceof J.Empty)) {
                                return md;
                            }
                        }

                        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (enclosingClass == null || !implementsHealthIndicator(enclosingClass)) {
                            return md;
                        }

                        // Rename check -> health
                        JavaType.ShallowClass healthType = JavaType.ShallowClass.build(HEALTH_TYPE);
                        md = md.withName(md.getName().withSimpleName("health"));

                        // Change return type from Result to Health
                        md = md.withReturnTypeExpression(
                                new J.Identifier(randomId(),
                                        md.getReturnTypeExpression() != null ? md.getReturnTypeExpression().getPrefix() : Space.SINGLE_SPACE,
                                        Markers.EMPTY,
                                        emptyList(), "Health", healthType, null));

                        // Change access from protected to public
                        List<J.Modifier> modifiers = new java.util.ArrayList<>(md.getModifiers());
                        for (int i = 0; i < modifiers.size(); i++) {
                            if (modifiers.get(i).getType() == J.Modifier.Type.Protected) {
                                modifiers.set(i, modifiers.get(i).withType(J.Modifier.Type.Public).withKeyword("public"));
                            }
                        }
                        md = md.withModifiers(modifiers);

                        // If method had a throws clause, wrap body in try-catch
                        // since HealthIndicator.health() doesn't declare checked exceptions
                        if (method.getThrows() != null && !method.getThrows().isEmpty() && md.getBody() != null) {
                            J.Block origBody = md.getBody();

                            // Build: Health.down(e).build()
                            J.Identifier eRef = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), "e", null, null);
                            J.Identifier healthId = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), "Health", healthType, null);
                            J.MethodInvocation downCall = new J.MethodInvocation(
                                    randomId(), Space.EMPTY, Markers.EMPTY,
                                    JRightPadded.build((Expression) healthId), null,
                                    new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "down", null, null),
                                    JContainer.build(Space.EMPTY,
                                            Collections.singletonList(JRightPadded.build((Expression) eRef)), Markers.EMPTY),
                                    null);
                            J.MethodInvocation buildCall = new J.MethodInvocation(
                                    randomId(), Space.EMPTY, Markers.EMPTY,
                                    JRightPadded.build((Expression) downCall), null,
                                    new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "build", null, null),
                                    JContainer.build(Space.EMPTY, emptyList(), Markers.EMPTY),
                                    null);

                            // return Health.down(e).build();
                            J.Return returnStmt = new J.Return(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    buildCall.withPrefix(Space.SINGLE_SPACE));

                            J.Block catchBody = new J.Block(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    JRightPadded.build(false),
                                    Collections.singletonList(JRightPadded.build((Statement) returnStmt)),
                                    Space.SINGLE_SPACE);

                            // catch (Exception e)
                            J.Identifier exType = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), "Exception", null, null);
                            J.VariableDeclarations.NamedVariable eVar = new J.VariableDeclarations.NamedVariable(
                                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "e", null, null),
                                    emptyList(), null, null);
                            J.VariableDeclarations catchDecl = new J.VariableDeclarations(
                                    randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), emptyList(), exType, null, emptyList(),
                                    Collections.singletonList(JRightPadded.build(eVar)));
                            J.ControlParentheses<J.VariableDeclarations> catchControl = new J.ControlParentheses<>(
                                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    JRightPadded.build(catchDecl));
                            J.Try.Catch catchClause = new J.Try.Catch(
                                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    catchControl, catchBody);

                            // try { ... } catch (Exception e) { ... }
                            J.Try tryStmt = new J.Try(
                                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    null, origBody.withPrefix(Space.SINGLE_SPACE),
                                    Collections.singletonList(catchClause),
                                    null);

                            J.Block newBody = new J.Block(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    JRightPadded.build(false),
                                    Collections.singletonList(JRightPadded.build((Statement) tryStmt)),
                                    Space.SINGLE_SPACE);
                            md = md.withBody(newBody);
                        }

                        // Remove throws clause (HealthIndicator.health() doesn't throw)
                        md = md.withThrows(null);

                        maybeAddImport(HEALTH_TYPE);
                        maybeRemoveImport(HEALTH_CHECK_TYPE + ".Result");

                        return md;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                        // Transform Result.healthy(...) -> Health.up().build()
                        // Transform Result.unhealthy(...) -> Health.down().build()
                        if (mi.getSelect() == null) {
                            return mi;
                        }

                        String selectName = null;
                        if (mi.getSelect() instanceof J.Identifier) {
                            selectName = ((J.Identifier) mi.getSelect()).getSimpleName();
                        }
                        if (!"Result".equals(selectName)) {
                            return mi;
                        }

                        String methodName = mi.getSimpleName();
                        String springMethod;
                        if ("healthy".equals(methodName)) {
                            springMethod = "up";
                        } else if ("unhealthy".equals(methodName)) {
                            springMethod = "down";
                        } else {
                            return mi;
                        }

                        JavaType.ShallowClass healthType = JavaType.ShallowClass.build(HEALTH_TYPE);

                        // Build Health.<springMethod>().build()
                        // First: Health.<springMethod>()
                        J.Identifier healthSelect = new J.Identifier(
                                randomId(), mi.getSelect().getPrefix(), Markers.EMPTY,
                                emptyList(), "Health", healthType, null);

                        J.MethodInvocation innerCall = mi
                                .withSelect(healthSelect)
                                .withName(mi.getName().withSimpleName(springMethod))
                                .withArguments(emptyList());

                        // Then: .build()
                        J.Identifier buildName = new J.Identifier(
                                randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), "build", null, null);

                        return new J.MethodInvocation(
                                randomId(), mi.getPrefix(), Markers.EMPTY,
                                JRightPadded.build(innerCall),
                                null, buildName,
                                JContainer.build(Space.EMPTY, emptyList(), Markers.EMPTY),
                                null);
                    }

                    private boolean implementsHealthIndicator(J.ClassDeclaration cd) {
                        if (cd.getExtends() != null && isOfClassType(cd.getExtends().getType(), HEALTH_CHECK_TYPE)) {
                            return true;
                        }
                        if (cd.getImplements() != null) {
                            // Check by type attribution first
                            if (cd.getImplements().stream()
                                    .anyMatch(impl -> isOfClassType(impl.getType(), HEALTH_INDICATOR_TYPE))) {
                                return true;
                            }
                            // Fall back to text-based check for unattributed types
                            if (cd.getImplements().stream()
                                    .anyMatch(impl -> impl.toString().contains("HealthIndicator"))) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
        );
    }
}

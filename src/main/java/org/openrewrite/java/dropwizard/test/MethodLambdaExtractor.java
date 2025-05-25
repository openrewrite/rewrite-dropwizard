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
package org.openrewrite.java.dropwizard.test;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class MethodLambdaExtractor extends Recipe {

    @Option(
            displayName = "Ignore methods in anonymous classes",
            description = "When enabled, ignore @Override annotations on methods in anonymous classes.",
            required = false)
    @Nullable
    String preconditionType;

    @Option(
            displayName = "Ignore methods in anonymous classes",
            description = "When enabled, ignore @Override annotations on methods in anonymous classes.",
            required = false)
    @Nullable
    String matchingPattern;

    @Override
    public String getDisplayName() {
        return "Extract lambda expressions";
    }

    @Override
    public String getDescription() {
        return "Extracts the body of lambda expressions and inlines them into the surrounding code.";
    }

    private boolean shouldExtractMethodInvocation(J.MethodInvocation methodInvocation) {
        return new MethodMatcher(matchingPattern).matches(methodInvocation);
    }

    private boolean shouldExtractLambda(J.VariableDeclarations varDecl) {
        return varDecl.getVariables().stream()
                .map(J.VariableDeclarations.NamedVariable::getInitializer)
                .filter(init -> init instanceof J.MethodInvocation)
                .map(init -> (J.MethodInvocation) init)
                .anyMatch(this::shouldExtractMethodInvocation);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesType<>(preconditionType, true)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(
                            J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                        if (md.getBody() == null) {
                            return md;
                        }

                        List<Statement> updatedStatements = new ArrayList<>();
                        for (Statement stmt : md.getBody().getStatements()) {
                            if (stmt instanceof J.MethodInvocation
                                    && shouldExtractMethodInvocation((J.MethodInvocation) stmt)) {
                                processMethodInvocationStatement((J.MethodInvocation) stmt, updatedStatements);
                            } else if (stmt instanceof J.VariableDeclarations) {
                                processVariableDeclaration((J.VariableDeclarations) stmt, updatedStatements);
                            } else {
                                updatedStatements.add(stmt);
                            }
                        }

                        return maybeAutoFormat(
                                method, md.withBody(md.getBody().withStatements(updatedStatements)), ctx);
                    }

                    private void processMethodInvocationStatement(
                            J.MethodInvocation methodInv, List<Statement> updatedStatements) {
                        boolean handled = false;
                        for (Expression arg : methodInv.getArguments()) {
                            if (arg instanceof J.Lambda) {
                                List<Statement> statements = extractLambdaBody((J.Lambda) arg);
                                if (!statements.isEmpty()) {
                                    updatedStatements.addAll(statements);
                                    handled = true;
                                    break;
                                }
                            }
                        }
                        if (!handled) {
                            updatedStatements.add(methodInv);
                        }
                    }

                    private void processVariableDeclaration(
                            J.VariableDeclarations varDecl, List<Statement> updatedStatements) {
                        if (!shouldExtractLambda(varDecl) || varDecl.getVariables().isEmpty()) {
                            updatedStatements.add(varDecl);
                            return;
                        }

                        List<J.VariableDeclarations.NamedVariable> processedVars = new ArrayList<>();
                        for (J.VariableDeclarations.NamedVariable var : varDecl.getVariables()) {
                            Expression initializer = var.getInitializer();
                            if (initializer instanceof J.MethodInvocation) {
                                J.MethodInvocation methodInv = (J.MethodInvocation) initializer;
                                processedVars.add(processMethodInvocationVar(var, methodInv));
                            } else {
                                processedVars.add(var);
                            }
                        }

                        updatedStatements.add(varDecl.withVariables(processedVars));
                    }

                    private J.VariableDeclarations.NamedVariable processMethodInvocationVar(
                            J.VariableDeclarations.NamedVariable var, J.MethodInvocation methodInv) {
                        if (methodInv.getArguments().isEmpty()) {
                            return var;
                        }

                        for (Expression arg : methodInv.getArguments()) {
                            if (arg instanceof J.Lambda) {
                                J.Lambda lambda = (J.Lambda) arg;
                                if (lambda.getBody() instanceof Expression) {
                                    return var.withInitializer((Expression) lambda.getBody());
                                } else if (lambda.getBody() instanceof J.Block) {
                                    J.Block block = (J.Block) lambda.getBody();
                                    if (!block.getStatements().isEmpty()) {
                                        Statement lastStmt =
                                                block.getStatements().get(block.getStatements().size() - 1);
                                        if (lastStmt instanceof J.Return) {
                                            J.Return returnStmt = (J.Return) lastStmt;
                                            if (returnStmt.getExpression() != null) {
                                                return var.withInitializer(returnStmt.getExpression());
                                            }
                                        } else if (lastStmt instanceof Expression) {
                                            return var.withInitializer((Expression) lastStmt);
                                        }
                                    }
                                }
                            }
                        }

                        return var;
                    }

                    private List<Statement> extractLambdaBody(J.Lambda lambda) {
                        if (lambda.getBody() instanceof J.Block) {
                            J.Block block = (J.Block) lambda.getBody();
                            return block.getStatements();
                        } else if (lambda.getBody() instanceof Expression) {
                            return Collections.singletonList((Statement) lambda.getBody());
                        }
                        return Collections.emptyList();
                    }
                });
    }
}

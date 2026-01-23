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
package org.openrewrite.java.dropwizard.jetty;

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
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateJettyHandlerSignature extends Recipe {

    String displayName = "Migrate Jetty `AbstractHandler` to Jetty 12 `Handler.Abstract`";

    String description = "Migrates custom Jetty handler implementations from Jetty 11's `AbstractHandler` " +
            "(used in Dropwizard 4.x) to Jetty 12's `Handler.Abstract` (used in Dropwizard 5.x). " +
            "This changes the `handle` method signature and updates `baseRequest.setHandled(true)` " +
            "to use `Callback` and return `true`.";

    private static final String ABSTRACT_HANDLER = "org.eclipse.jetty.server.handler.AbstractHandler";
    private static final String JETTY_REQUEST = "org.eclipse.jetty.server.Request";
    private static final String JETTY_RESPONSE = "org.eclipse.jetty.server.Response";
    private static final String JETTY_CALLBACK = "org.eclipse.jetty.util.Callback";

    private static final MethodMatcher HANDLE_METHOD_MATCHER = new MethodMatcher(
            ABSTRACT_HANDLER + " handle(String, org.eclipse.jetty.server.Request, ..)", true);
    private static final MethodMatcher SET_HANDLED_MATCHER = new MethodMatcher(
            JETTY_REQUEST + " setHandled(boolean)", false);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(ABSTRACT_HANDLER, false),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                        if (cd.getExtends() != null && isOfClassType(cd.getExtends().getType(), ABSTRACT_HANDLER)) {
                            JavaType.ShallowClass handlerAbstractType = JavaType.ShallowClass.build(
                                    "org.eclipse.jetty.server.Handler.Abstract");

                            J.Identifier handlerIdent = new J.Identifier(
                                    randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), "Handler",
                                    JavaType.ShallowClass.build("org.eclipse.jetty.server.Handler"), null
                            );
                            J.Identifier abstractIdent = new J.Identifier(
                                    randomId(), Space.EMPTY, Markers.EMPTY,
                                    emptyList(), "Abstract",
                                    handlerAbstractType, null
                            );
                            cd = cd.withExtends(new J.FieldAccess(
                                    randomId(), cd.getExtends().getPrefix(), Markers.EMPTY,
                                    handlerIdent, JLeftPadded.build(abstractIdent),
                                    handlerAbstractType
                            ));

                            maybeRemoveImport(ABSTRACT_HANDLER);
                            maybeRemoveImport("jakarta.servlet.ServletException");
                            maybeRemoveImport("jakarta.servlet.http.HttpServletRequest");
                            maybeRemoveImport("jakarta.servlet.http.HttpServletResponse");
                            maybeRemoveImport("javax.servlet.ServletException");
                            maybeRemoveImport("javax.servlet.http.HttpServletRequest");
                            maybeRemoveImport("javax.servlet.http.HttpServletResponse");
                            maybeAddImport(JETTY_CALLBACK);
                            maybeAddImport("org.eclipse.jetty.server.Handler");
                            maybeAddImport(JETTY_RESPONSE);
                        }

                        return cd;
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (enclosingClass == null || !HANDLE_METHOD_MATCHER.matches(md, enclosingClass)) {
                            return md;
                        }

                        // Change return type from void to boolean
                        md = md.withReturnTypeExpression(
                                new J.Primitive(randomId(),
                                        md.getReturnTypeExpression() != null ? md.getReturnTypeExpression().getPrefix() : Space.SINGLE_SPACE,
                                        Markers.EMPTY, JavaType.Primitive.Boolean)
                        );

                        // Build new parameter list: Request request, Response response, Callback callback
                        List<Statement> newParams = new ArrayList<>();
                        newParams.add(buildParameter("Request", JETTY_REQUEST, "request"));
                        newParams.add(buildParameter("Response", JETTY_RESPONSE, "response"));
                        newParams.add(buildParameter("Callback", JETTY_CALLBACK, "callback"));
                        newParams.set(1, newParams.get(1).withPrefix(Space.SINGLE_SPACE));
                        newParams.set(2, newParams.get(2).withPrefix(Space.SINGLE_SPACE));
                        md = md.withParameters(newParams);

                        // Update throws clause to just Exception
                        if (md.getThrows() != null) {
                            J.Identifier exceptionType = new J.Identifier(
                                    randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                    emptyList(), "Exception",
                                    JavaType.ShallowClass.build("java.lang.Exception"), null
                            );
                            md = md.withThrows(singletonList(exceptionType));
                        }

                        return md;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                        if (!SET_HANDLED_MATCHER.matches(mi)) {
                            return mi;
                        }

                        Expression arg = mi.getArguments().get(0);
                        if (!(arg instanceof J.Literal) || !Boolean.TRUE.equals(((J.Literal) arg).getValue())) {
                            return mi;
                        }

                        // Replace with callback.succeeded()
                        JavaType.Method succeededType = new JavaType.Method(
                                null, 1L, JavaType.ShallowClass.build(JETTY_CALLBACK),
                                "succeeded", JavaType.Primitive.Void,
                                emptyList(), emptyList(),
                                emptyList(), emptyList(),
                                emptyList(), emptyList()
                        );
                        J.Identifier newName = new J.Identifier(
                                randomId(), mi.getName().getPrefix(), Markers.EMPTY,
                                emptyList(), "succeeded",
                                succeededType, null
                        );
                        return mi
                                .withSelect(new J.Identifier(
                                        randomId(), mi.getSelect().getPrefix(), Markers.EMPTY,
                                        emptyList(), "callback",
                                        JavaType.ShallowClass.build(JETTY_CALLBACK), null
                                ))
                                .withName(newName)
                                .withMethodType(succeededType)
                                .withArguments(emptyList());
                    }

                    @Override
                    public J.Return visitReturn(J.Return return_, ExecutionContext ctx) {
                        J.Return ret = super.visitReturn(return_, ctx);

                        J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (enclosingMethod != null && enclosingClass != null &&
                                HANDLE_METHOD_MATCHER.matches(enclosingMethod, enclosingClass) &&
                                ret.getExpression() == null) {
                            ret = ret.withExpression(
                                    new J.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, true, "true",
                                            null, JavaType.Primitive.Boolean)
                            );
                        }

                        return ret;
                    }

                    private J.VariableDeclarations buildParameter(String typeName, String fqn, String paramName) {
                        JavaType.ShallowClass type = JavaType.ShallowClass.build(fqn);
                        J.Identifier typeExpr = new J.Identifier(
                                randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), typeName, type, null
                        );
                        J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                                randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY,
                                        emptyList(), paramName, type,
                                        new JavaType.Variable(null, 0, paramName, null, type, null)),
                                emptyList(), null, null
                        );
                        return new J.VariableDeclarations(
                                randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), emptyList(),
                                typeExpr, null,
                                singletonList(JRightPadded.build(namedVar))
                        );
                    }
                }
        );
    }
}

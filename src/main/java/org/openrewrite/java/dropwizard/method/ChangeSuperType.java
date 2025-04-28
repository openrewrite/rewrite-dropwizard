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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import static java.lang.Boolean.TRUE;
import static org.openrewrite.java.tree.TypeUtils.asFullyQualified;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeSuperType extends Recipe {

    @Option(displayName = "Target class",
            description = "The fully qualified name of the class whose superclass should be changed.",
            example = "com.myorg.MyClass")
    String targetClass;

    @Option(displayName = "New superclass",
            description = "The fully qualified name of the new superclass to extend or interface to implement.",
            example = "com.myorg.NewSuperclass")
    String newSuperclass;

    @Option(displayName = "Keep type parameters",
            description = "Whether to keep existing type parameters on the target class declaration.",
            required = false)
    Boolean keepTypeParameters;

    @Option(displayName = "Convert to interface",
            description = "If the new supertype is an interface, setting this to true converts 'extends' to 'implements'.",
            required = false)
    Boolean convertToInterface;

    @Option(displayName = "Remove unnecessary overrides",
            description = "Remove method Override annotations that override methods from the *old* superclass but are no longer necessary with the new superclass.",
            required = false)
    Boolean removeUnnecessaryOverrides;

    @Override
    public String getDisplayName() {
        return "Change superclass";
    }

    @Override
    public String getDescription() {
        return "Changes the superclass of a specified class to a new superclass.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                if (cd.getExtends() == null || !isOfClassType(cd.getExtends().getType(), targetClass)) {
                    return cd;
                }

                String typeParams = getTypeParams(cd.getExtends());

                maybeAddImport(newSuperclass);
                maybeRemoveImport(targetClass);

                JavaTemplate extendsTemplate =
                        JavaTemplate.builder("#{}" + typeParams)
                                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                                .imports(newSuperclass)
                                .contextSensitive()
                                .build();

                JavaType.FullyQualified newSuperType;

                if (TRUE.equals(convertToInterface)) {
                    cd =
                            extendsTemplate.apply(
                                    updateCursor(cd), cd.getCoordinates().addImplementsClause(), newSuperclass);
                    cd = cd.withExtends(null);
                    TypeTree lastInterface = cd.getImplements().get(cd.getImplements().size() - 1);
                    newSuperType = asFullyQualified(lastInterface.getType());
                } else {
                    cd =
                            extendsTemplate.apply(
                                    updateCursor(cd), cd.getCoordinates().replaceExtendsClause(), newSuperclass);
                    newSuperType = asFullyQualified(cd.getExtends().getType());
                }

                // If changing to a class that was not compiled initially (eg. from a new library)
                if (newSuperType == null) {
                    newSuperType = asFullyQualified(JavaType.buildType(newSuperclass));
                }

                cd = cd.withType(((JavaType.Class) cd.getType()).withSupertype(newSuperType));

                doAfterVisit(new UpdateMethodTypesVisitor(cd.getType()));

                if (TRUE.equals(removeUnnecessaryOverrides)) {
                    doAfterVisit(new RemoveUnnecessaryOverride(false).getVisitor());
                }

                doAfterVisit(new RemoveUnnecessarySuperCalls.RemoveUnnecessarySuperCallsVisitor());
                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(cd));

                return cd;
            }

            private String getTypeParams(TypeTree extendsType) {
                StringBuilder typeParams = new StringBuilder();

                if (TRUE.equals(keepTypeParameters) && extendsType instanceof J.ParameterizedType) {
                    J.ParameterizedType parameterizedType = (J.ParameterizedType) extendsType;

                    boolean hasParameters = false;
                    typeParams.append('<');

                    for (Expression typeParameter : parameterizedType.getTypeParameters()) {
                        if (hasParameters) {
                            typeParams.append(", ");
                        }
                        typeParams.append(typeParameter.toString());
                        hasParameters = true;
                    }

                    if (hasParameters) {
                        typeParams.append('>');
                    } else {
                        typeParams.setLength(0);
                    }
                }

                return typeParams.toString();
            }
        };
    }
}

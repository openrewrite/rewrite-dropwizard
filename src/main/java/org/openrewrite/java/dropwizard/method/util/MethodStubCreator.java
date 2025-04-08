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
package org.openrewrite.java.dropwizard.method.util;

import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodStubCreator {

    public static String buildThrowsClause(List<JavaType> thrownExceptions) {
        if (thrownExceptions == null || thrownExceptions.isEmpty()) {
            return "";
        }

        List<String> exceptionNames = new ArrayList<>();
        for (JavaType exception : thrownExceptions) {
            String exceptionName = buildTypeName(exception);
            // Skip if we got the fallback type
            if (!exceptionName.equals("Object")) {
                exceptionNames.add(exceptionName);
            }
        }

        return exceptionNames.isEmpty() ? "" : " throws " + String.join(", ", exceptionNames);
    }

    private static void collectGenericTypeVariables(
            JavaType type, List<JavaType.GenericTypeVariable> vars) {
        if (type == null) {
            return;
        }
        if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) type;
            // Only add if we haven't already (or use a Set)
            if (!vars.contains(gtv)) {
                vars.add(gtv);
            }
            // GTV may have bounds, so check them too
            if (gtv.getBounds() != null) {
                for (JavaType bound : gtv.getBounds()) {
                    collectGenericTypeVariables(bound, vars);
                }
            }
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            // Recurse into type parameters
            if (parameterized.getTypeParameters() != null) {
                for (JavaType tp : parameterized.getTypeParameters()) {
                    collectGenericTypeVariables(tp, vars);
                }
            }
        } else if (type instanceof JavaType.Array) {
            // Recurse into the element type
            JavaType.Array arr = (JavaType.Array) type;
            collectGenericTypeVariables(arr.getElemType(), vars);
        }
        // For FullyQualified, Primitive, etc. do nothing special
    }

    private static String buildMethodTypeParametersFromCollected(
            List<JavaType.GenericTypeVariable> generics) {
        if (generics.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JavaType.GenericTypeVariable gtv : generics) {
            String name = gtv.getName();
            List<JavaType> bounds = gtv.getBounds();
            if (bounds == null || bounds.isEmpty()) {
                // No bounds
                parts.add(name);
            } else {
                // e.g. "T extends java.lang.Comparable & SomeOtherBound"
                String boundsStr =
                        bounds.stream()
                                .map(MethodStubCreator::buildTypeName)
                                .collect(Collectors.joining(" & "));
                parts.add(name + " extends " + boundsStr);
            }
        }
        return "<" + String.join(", ", parts) + ">";
    }

    public static String buildMethodStub(JavaType.Method methodType) {
        // 1) Collect all GenericTypeVariable from return + param types
        List<JavaType.GenericTypeVariable> collectedVars = new ArrayList<>();

        // Return type
        collectGenericTypeVariables(methodType.getReturnType(), collectedVars);

        // Parameter types
        List<JavaType> paramTypes = methodType.getParameterTypes();
        if (paramTypes != null) {
            for (JavaType pt : paramTypes) {
                collectGenericTypeVariables(pt, collectedVars);
            }
        }

        // 2) Build e.g. "<T, U extends Something>"
        String methodTypeParameters = buildMethodTypeParametersFromCollected(collectedVars);

        // 3) Build normal method signature
        String paramsSignature =
                buildParameters(methodType.getParameterTypes(), methodType.getParameterNames());
        String returnTypeName = buildReturnType(methodType.getReturnType());
        String throwsClause = buildThrowsClause(methodType.getThrownExceptions());
        String methodName = methodType.getName();

        String methodTypeParametersTrimmed = methodTypeParameters.trim();
        String maybeSpace = methodTypeParametersTrimmed.isEmpty() ? "" : " ";

        return String.format(
                "@Override%npublic %s%s%s %s(%s)%s {%n    throw new UnsupportedOperationException();%n}",
                methodTypeParametersTrimmed,
                maybeSpace,
                returnTypeName,
                methodName,
                paramsSignature,
                throwsClause);
    }

    public static String buildParameters(List<JavaType> parameterTypes, List<String> parameterNames) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return "";
        }

        List<String> params = new ArrayList<>();
        for (int i = 0; i < parameterTypes.size(); i++) {
            JavaType paramType = parameterTypes.get(i);
            String paramName = getParameterName(parameterNames, i);
            String paramTypeName = buildTypeName(paramType);
            params.add(paramTypeName + " " + paramName);
        }
        return String.join(", ", params);
    }

    private static String getParameterName(List<String> parameterNames, int index) {
        if (parameterNames != null &&
                index < parameterNames.size() &&
                parameterNames.get(index) != null &&
                !parameterNames.get(index).trim().isEmpty()) {
            return parameterNames.get(index).trim();
        }
        // fallback name
        return "arg" + index;
    }

    public static String buildReturnType(JavaType returnType) {
        return buildTypeName(returnType);
    }

    /**
     * Builds a textual representation of any JavaType (including generics, arrays, etc.).
     */
    private static String buildTypeName(JavaType type) {
        if (type == null) {
            return "void";
        }

        // 1) Primitive types
        if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword();
        }

        // 2) Array types (recursive)
        if (type instanceof JavaType.Array) {
            JavaType.Array array = (JavaType.Array) type;
            return buildTypeName(array.getElemType()) + "[]";
        }

        // 3) Parameterized types, e.g. List<String>
        if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            StringBuilder sb = new StringBuilder();

            // Base type
            JavaType.FullyQualified baseType = TypeUtils.asFullyQualified(parameterized.getType());
            if (baseType != null && baseType.getFullyQualifiedName() != null) {
                sb.append(baseType.getFullyQualifiedName());
            } else {
                sb.append("Object");
            }

            // Type parameters
            List<JavaType> typeParameters = parameterized.getTypeParameters();
            if (!typeParameters.isEmpty()) {
                sb.append('<');
                List<String> paramTypes = new ArrayList<>();
                for (JavaType typeParam : typeParameters) {
                    paramTypes.add(buildTypeName(typeParam)); // Recursive call
                }
                sb.append(String.join(", ", paramTypes));
                sb.append('>');
            }
            return sb.toString();
        }

        // 4) A single generic type variable, e.g. "T"
        if (type instanceof JavaType.GenericTypeVariable) {
            return ((JavaType.GenericTypeVariable) type).getName();
        }

        // 5) A fully qualified type, e.g. java.util.List
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        if (fq != null && fq.getFullyQualifiedName() != null) {
            return fq.getFullyQualifiedName();
        }

        // 6) Fallback if unresolvable
        return "Object";
    }
}

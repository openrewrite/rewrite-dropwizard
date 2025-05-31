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

import org.openrewrite.java.dropwizard.test.DropwizardCallParser.ParsedCall;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;


public class RestTemplateCallBuilder implements DropwizardToSpringCallBuilder {

    @Override
    public String buildMethod(ParsedCall callInfo, JavaType returnType) {
        boolean wantsResponseEntity = TypeUtils.isOfClassType(returnType,
                "javax.ws.rs.core.Response");

        if (wantsResponseEntity) {
            returnType = JavaType.buildType("java.lang.Object");
        }

        String urlTemplate = buildUrlTemplate(callInfo);

        if (isGenericType(returnType)) {
            return buildGenericExchange(callInfo, returnType, urlTemplate, wantsResponseEntity);
        }

        return buildBodyExchange(
                callInfo,
                returnType,
                urlTemplate,
                wantsResponseEntity);
    }

    @Override
    public String buildVariable(J.VariableDeclarations variableDeclarations, JavaType returnType) {
        boolean wantsResponseEntity = TypeUtils.isOfClassType(returnType,
                "javax.ws.rs.core.Response");

        J.Identifier variableName = variableDeclarations.getVariables().get(0).getName();

        if (wantsResponseEntity) {
            return "ResponseEntity<Object> " + variableName;
        }

        return variableDeclarations.getVariables().get(0).getType() + " " + variableName;
    }


    @Override
    public String[] getImports() {
        return new String[]{
                "org.springframework.http.HttpEntity",
                "org.springframework.http.HttpHeaders",
                "org.springframework.http.MediaType",
                "org.springframework.http.HttpMethod",
                "org.springframework.core.ParameterizedTypeReference",
                "org.springframework.web.client.RestTemplate",
                "org.springframework.http.ResponseEntity",
                "java.util.Collections",
                "java.util.Map",
                "java.util.HashMap"
        };
    }

    @Override
    public String[] getImportsToRemove() {
        return new String[]{
                "javax.ws.rs.core.HttpHeaders",
                "javax.ws.rs.core.MediaType",
                "javax.ws.rs.core.Response"
        };
    }

    private String buildUrlTemplate(ParsedCall callInfo) {
        return String.format("\"%s\"", callInfo.getPath());
    }

    private String buildHttpEntity(ParsedCall callInfo) {
        boolean hasBody = !callInfo.getRequestBody().isEmpty();
        boolean needsHeaders = hasHeaders(callInfo);
        String body = hasBody ? callInfo.getRequestBody() : "null";
        if (needsHeaders) {
            String headers = buildHeaders(callInfo);
            return String.format("new HttpEntity<>(%s, %s)", body, headers);
        } else {
            return String.format("new HttpEntity<>(%s)", body);
        }
    }

    private String buildHeaders(ParsedCall callInfo) {
        if (!hasHeaders(callInfo)) {
            return "new HttpHeaders()";
        }
        StringBuilder headers = new StringBuilder("new org.springframework.http.HttpHeaders() {{ ");
        if (!callInfo.getContentMediaType().isEmpty()) {
            headers.append(String.format("setContentType(%s);", callInfo.getContentMediaType()));
        }
        if (!callInfo.getAcceptMediaType().isEmpty()) {
            headers.append(
                    String.format("setAccept(Collections.singletonList(%s));", callInfo.getAcceptMediaType()));
        }
        if (!callInfo.getAuthHeader().isEmpty()) {
            headers.append(String.format("set(HttpHeaders.AUTHORIZATION, %s);", callInfo.getAuthHeader()));
        }
        headers.append(" }}");
        return headers.toString();
    }

    private String getReturnTypeString(JavaType returnType) {
        if (returnType == null) {
            return "Object.class";
        }

        return returnType + ".class";
    }

    private boolean isGenericType(JavaType jt) {
        return jt instanceof JavaType.Parameterized;
    }

    private boolean hasHeaders(ParsedCall callInfo) {
        return !callInfo.getAuthHeader().isEmpty() ||
                !callInfo.getAcceptMediaType().isEmpty() ||
                !callInfo.getContentMediaType().isEmpty();
    }

    private String buildBodyExchange(
            ParsedCall callInfo,
            JavaType returnType,
            String urlTemplate,
            boolean wantsResponseEntity) {
        String httpEntity = buildHttpEntity(callInfo);
        String returnTypeClassString = getReturnTypeString(returnType);
        String suffix = wantsResponseEntity ? "" : ".getBody()";
        String httpMethod = callInfo.getHttpMethod().toUpperCase();

        if (wantsResponseEntity) {
            String uriParametersMap = "java.util.Collections.emptyMap()";

            return String.format(
                    "restTemplate.exchange(%s, HttpMethod.%s, %s, %s, %s)%s", // Hardcoded emptyMap
                    urlTemplate, httpMethod, httpEntity, returnTypeClassString, uriParametersMap, suffix);
        }

        return String.format(
                "restTemplate.exchange(%s, HttpMethod.%s, %s, %s)%s",
                urlTemplate, httpMethod, httpEntity, returnTypeClassString, suffix);
    }

    private String buildGenericExchange(
            ParsedCall callInfo,
            JavaType returnType, // This is the generic type
            String urlTemplate,
            boolean wantsResponseEntity) {
        String httpEntity = buildHttpEntity(callInfo);
        String httpMethod = callInfo.getHttpMethod().toUpperCase();

        // Use ParameterizedTypeReference for the generic type
        String parameterizedTypeRef =
                String.format("new ParameterizedTypeReference<%s>() {}", returnType.toString());


        String suffix = wantsResponseEntity ? "" : ".getBody()";

        return String.format(
                "restTemplate.exchange(%s, HttpMethod.%s, %s, %s)%s",
                urlTemplate, httpMethod.toUpperCase(), httpEntity, parameterizedTypeRef, suffix);
    }
}

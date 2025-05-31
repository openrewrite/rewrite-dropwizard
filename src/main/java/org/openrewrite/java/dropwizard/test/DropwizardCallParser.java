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

import lombok.Builder;
import lombok.Data;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropwizardCallParser {
    public static ParsedCall parse(J.MethodInvocation method, Cursor cursor) {
        String methodSource = method.print(cursor);
        return parseMethodInvocation(method, methodSource, cursor);
    }

    static ParsedCall parseMethodInvocation(
            J.MethodInvocation method, String methodSource, Cursor cursor) {

        return ParsedCall.builder()
                .pathInfo(extractPathInfo(method))
                .httpMethod(extractHttpMethod(method))
                .requestBody(extractRequestBody(methodSource))
                .queryParams(extractQueryParams(methodSource))
                .pathParams(extractPathParams(methodSource))
                .mediaTypes(extractMediaTypes(methodSource))
                .authHeader(extractAuthHeader(methodSource))
                .build();
    }

    static PathInfo extractPathInfo(J.MethodInvocation method) {
        return findMethodInChain(method, "target")
                .map(
                        targetMethod -> {
                            String fullPath = extractPathFromTarget(targetMethod);
                            String normalizedPath = normalizePath(fullPath);
                            boolean hasTemplates = normalizedPath.contains("{") && normalizedPath.contains("}");

                            return PathInfo.builder()
                                    .path(normalizedPath)
                                    .fullPath(fullPath)
                                    .hasTemplateVariables(hasTemplates)
                                    .build();
                        })
                .orElse(defaultPathInfo());
    }

    static String extractPathFromTarget(J.MethodInvocation targetMethod) {
        if (targetMethod.getArguments().isEmpty()) {
            return "/";
        }

        Expression pathArg = targetMethod.getArguments().get(0);
        if (pathArg instanceof J.Literal) {
            return ((J.Literal) pathArg).getValue().toString();
        } else if (pathArg instanceof J.Binary) {
            return pathArg.toString();
        }

        return "/";
    }

    static String normalizePath(String path) {
        // Extract path after port if present
        int portIndex = path.lastIndexOf(":");
        if (portIndex != -1) {
            int pathStart = path.indexOf("/", portIndex);
            if (pathStart != -1) {
                path = path.substring(pathStart);
            }
        }

        return path.replaceAll("^\"|\"$", "") // Remove quotes
                .replaceAll("/{2,}", "/") // Normalize multiple slashes
                .replaceAll("/+$", "") // Remove trailing slash
                .replaceFirst("^(?!/)", "/"); // Ensure leading slash
    }

    static List<ParamInfo> extractQueryParams(String methodSource) {
        List<ParamInfo> params = new ArrayList<>();
        Matcher matcher = Patterns.QUERY_PARAM.matcher(methodSource);

        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);

            params.add(ParamInfo.builder().name(name).value(value).build());
        }

        return params;
    }

    static List<ParamInfo> extractPathParams(String methodSource) {
        List<ParamInfo> params = new ArrayList<>();
        Matcher matcher = Patterns.PATH_PARAM.matcher(methodSource);

        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);

            params.add(ParamInfo.builder().name(name).value(value).build());
        }

        return params;
    }

    static MediaTypeInfo extractMediaTypes(String methodSource) {
        String accept =
                findFirstMatch(Patterns.REQUEST, methodSource)
                        .orElse(findFirstMatch(Patterns.ACCEPT, methodSource).orElse(""));

        String content =
                findFirstMatch(Patterns.ENTITY, methodSource, 2)
                        .map(m -> m.group(2).trim())
                        .orElseGet(() -> findFirstMatch(Patterns.CONTENT_TYPE, methodSource).orElse(""));

        return MediaTypeInfo.builder()
                .accept(normalizeMediaType(accept))
                .content(normalizeMediaType(content))
                .build();
    }

    static String normalizeMediaType(String mediaType) {
        if (mediaType.isEmpty() || isJsonMediaType(mediaType)) {
            return "org.springframework.http.MediaType.APPLICATION_JSON";
        }

        return String.format("org.springframework.http.MediaType.parseMediaType(%s)", mediaType);
    }

    static boolean isJsonMediaType(String mediaType) {
        return mediaType.toLowerCase().contains("json");
    }

    static String extractHttpMethod(J.MethodInvocation method) {
        String methodStr = method.print().toLowerCase();
        if (methodStr.contains(".post(")) return "POST";
        if (methodStr.contains(".put(")) return "PUT";
        if (methodStr.contains(".delete(")) return "DELETE";
        if (methodStr.contains(".patch(")) return "PATCH";
        return "GET";
    }

    static String extractRequestBody(String methodSource) {
        return findFirstMatch(Patterns.ENTITY, methodSource, 2).map(m -> m.group(1).trim()).orElse("");
    }

    static String extractAuthHeader(String methodSource) {
        Pattern patternToTest = Patterns.AUTH_HEADER;

        return findFirstMatch(patternToTest, methodSource).orElse("");
    }

    static Optional<String> findFirstMatch(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    static Optional<Matcher> findFirstMatch(
            Pattern pattern, String input, int expectedGroups) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find() && matcher.groupCount() >= expectedGroups) {
            return Optional.of(matcher);
        }
        return Optional.empty();
    }

    static Optional<J.MethodInvocation> findMethodInChain(
            J.MethodInvocation method, String methodName) {
        J.MethodInvocation current = method;
        while (current != null) {
            if (current.getName().getSimpleName().equals(methodName)) {
                return Optional.of(current);
            }
            if (current.getSelect() instanceof J.MethodInvocation) {
                current = (J.MethodInvocation) current.getSelect();
            } else {
                break;
            }
        }
        return Optional.empty();
    }

    static PathInfo defaultPathInfo() {
        return PathInfo.builder().path("/").fullPath("/").hasTemplateVariables(false).build();
    }

    static final class Patterns {
        static final Pattern ACCEPT = Pattern.compile("\\.accept\\(\\s*([^)]+?)\\s*\\)");
        static final Pattern CONTENT_TYPE = Pattern.compile("\\.contentType\\(\\s*([^)]+?)\\s*\\)");

        static final Pattern AUTH_HEADER = Pattern.compile(
                "\\.header\\s*\\(\\s*(?:" + // Start non-capturing group for name
                        "\"Authorization\"" +  // Option 1: Literal string "Authorization"
                        "|" +
                        // Option 2: Any valid Java identifier(s) followed by .AUTHORIZATION
                        // [\\w.]+ matches one or more word characters or dots (for package/class names)
                        "[\\w.]+\\.AUTHORIZATION" +
                        ")\\s*,\\s*" + // End non-capturing group for name + comma
                        "([^)]+?)" +   // Capture group 1: Value (non-greedy)
                        "\\s*\\)",     // Whitespace and closing parenthesis
                Pattern.DOTALL // Keep DOTALL flag for multi-line robustness
        );

        static final Pattern REQUEST = Pattern.compile("\\.request\\(\\s*([^)]+?)\\s*\\)");
        static final Pattern ENTITY =
                Pattern.compile("Entity\\.entity\\(\\s*([^,]+?)\\s*,\\s*([^)]+?)\\s*\\)");
        static final Pattern QUERY_PARAM =
                Pattern.compile("\\.queryParam\\(\\s*\"([^\"]+)\"\\s*,\\s*([^)]+?)\\s*\\)");
        static final Pattern PATH_PARAM =
                Pattern.compile("\\.pathParam\\(\\s*\"([^\"]+)\"\\s*,\\s*([^)]+?)\\s*\\)");
    }

    @Data
    @Builder
    public static class ParsedCall {
        final PathInfo pathInfo;
        final String httpMethod;
        final String requestBody;
        final List<ParamInfo> queryParams;
        final List<ParamInfo> pathParams;
        final MediaTypeInfo mediaTypes;
        final String authHeader;

        // Convenience methods
        public String getPath() {
            return pathInfo.getPath();
        }

        public String getAcceptMediaType() {
            return mediaTypes.getAccept();
        }

        public String getContentMediaType() {
            return mediaTypes.getContent();
        }
    }

    @Data
    @Builder
    public static class PathInfo {
        final String path;
        final String fullPath;
        final boolean hasTemplateVariables;
    }

    @Data
    @Builder
    public static class ParamInfo {
        final String name;
        final String value;

        public boolean hasValue() {
            return name != null && !name.isEmpty() && value != null && !value.isEmpty();
        }
    }

    @Data
    @Builder
    public static class MediaTypeInfo {
        final String accept;
        final String content;
    }
}

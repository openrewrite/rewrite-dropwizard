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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

public class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static @Nullable String getSimpleName(String name) {
        if (name == null) {
            return null;
        }

        int paramStart = name.indexOf('(');
        String baseName = paramStart == -1 ? name : name.substring(0, paramStart);
        int lastDot = baseName.lastIndexOf('.');

        return lastDot == -1 ? baseName : baseName.substring(lastDot + 1);
    }

    public static String getSimpleNameWithParams(String name) {
        if (name == null || name.indexOf('.') == -1) {
            return name;
        }

        int paramStart = name.indexOf('(');
        if (paramStart == -1) {
            return name.substring(name.lastIndexOf('.') + 1);
        }

        String baseName = name.substring(0, paramStart);
        String params = name.substring(paramStart);
        return baseName.substring(baseName.lastIndexOf('.') + 1) + params;
    }

    public static J.Annotation makeAnnotation(String fullyQualifiedName) {
        JavaType.FullyQualified annotationType =
                TypeUtils.asFullyQualified(JavaType.buildType(fullyQualifiedName));

        return makeAnnotation(annotationType);
    }

    public static J.Annotation makeAnnotation(JavaType.FullyQualified annotationType) {
        return new J.Annotation(
                Tree.randomId(),
                Space.SINGLE_SPACE,
                Markers.EMPTY,
                new J.Identifier(
                        Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        null,
                        annotationType.getClassName(),
                        annotationType,
                        null),
                null);
    }
}

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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Interface that defines how a Dropwizard call should be transformed
 * into a Spring call snippet.
 */
public interface DropwizardToSpringCallBuilder {

    /**
     * Build the final Java snippet given the parsed call information
     * and the return type. For example, for RestTemplate or MockMvc.
     *
     * @param callInfo   Parsed details about the Dropwizard invocation
     * @param returnType The type to which we want to cast/assign the returned data
     * @return The Java snippet (as a String) that replaces the Dropwizard invocation
     */
    String buildMethod(DropwizardCallParser.ParsedCall callInfo, JavaType returnType);

    String buildVariable(J.VariableDeclarations variableDeclarations, JavaType returnType);

    /**
     * @return The fully qualified imports required for the generated snippet.
     */
    String[] getImports();

    String[] getImportsToRemove();

    /**
     * @return The fully qualified static imports required for the generated snippet.
     * Defaulting to none, can be overridden by implementations if they need them.
     */
    default String[] getStaticImports() {
        return new String[0];
    }
}

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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for the package-private static helper methods in DropwizardCallParser,
 * primarily focusing on the regex-based extraction logic using representative String inputs.
 */
class DropwizardCallParserHelperTest {

    @Test
    void extractAuthHeader_literalNameAndValue() {
        String input = ".target(\"...\").request().header(\"Authorization\", \"Bearer someToken\").get(Object.class)";
        String actual = DropwizardCallParser.extractAuthHeader(input);
        assertThat(actual).isEqualTo("\"Bearer someToken\"");
    }

    @Test
    void extractAuthHeader_constantNameAndValue() {
        String input = ".target(\"/protected/admin\").request().header(HttpHeaders.AUTHORIZATION, \"Basic Z29vZC1ndXk6c2VjcmV0\").get(String.class)";
        String actual = DropwizardCallParser.extractAuthHeader(input);
        assertThat(actual).isEqualTo("\"Basic Z29vZC1ndXk6c2VjcmV0\"");
    }

    @Test
    void extractAuthHeader_multilineWithConstant() {
        String input = ".target ( \"/path\" ) .request ( ) .header( HttpHeaders.AUTHORIZATION , \n  \"Basic DEF\" \n ) .get()";
        String actual = DropwizardCallParser.extractAuthHeader(input);
        assertThat(actual).isEqualTo("\"Basic DEF\"");
    }

    @Test
    void extractAuthHeader_multilineWithConstant2() {
        String input = "RULE.target(\"/protected/admin\").request()\n" +
          "            .header(HttpEntity.AUTHORIZATION, \"Basic Y2hpZWYtd2l6YXJkOnNlY3JldA==\")\n" +
          "            .get(String.class)\n";
        String actual = DropwizardCallParser.extractAuthHeader(input);
        assertThat(actual).isEqualTo("\"Basic Y2hpZWYtd2l6YXJkOnNlY3JldA==\"");
    }

    @Test
    void extractAuthHeader_noMatch() {
        String input = ".target(\"/people/2\").request().get()";
        String actual = DropwizardCallParser.extractAuthHeader(input);
        assertThat(actual).isEqualTo("");
    }

    @Test
    void extractRequestBody_simple() {
        String input = ".post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE)).readEntity(Object.class)";
        String actual = DropwizardCallParser.extractRequestBody(input);
        assertThat(actual).isEqualTo("person");
    }

    @Test
    void extractRequestBody_noMatch() {
        String input = ".get(String.class)";
        String actual = DropwizardCallParser.extractRequestBody(input);
        assertThat(actual).isEqualTo("");
    }

    @Test
    void extractMediaTypes_fromJsonEntity() {
        String input = ".post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE)).readEntity(Object.class)";
        DropwizardCallParser.MediaTypeInfo actual = DropwizardCallParser.extractMediaTypes(input);
        assertThat(actual.getAccept()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
        assertThat(actual.getContent()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
    }

    @Test
    void extractMediaTypes_fromRequestCall() {
        String input = ".target(\"...\").request(MediaType.APPLICATION_XML_TYPE).get()";
        DropwizardCallParser.MediaTypeInfo actual = DropwizardCallParser.extractMediaTypes(input);
        assertThat(actual.getAccept()).isEqualTo("org.springframework.http.MediaType.parseMediaType(MediaType.APPLICATION_XML_TYPE)");
        assertThat(actual.getContent()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
    }

    @Test
    void extractMediaTypes_fromAcceptMethod() {
        String input = ".target(\"...\").request().accept(MediaType.TEXT_HTML).get()";
        DropwizardCallParser.MediaTypeInfo actual = DropwizardCallParser.extractMediaTypes(input);
        assertThat(actual.getAccept()).isEqualTo("org.springframework.http.MediaType.parseMediaType(MediaType.TEXT_HTML)");
        assertThat(actual.getContent()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
    }

    @Test
    void extractMediaTypes_defaults() {
        String input = ".target(\"/people/2\").request().get()";
        DropwizardCallParser.MediaTypeInfo actual = DropwizardCallParser.extractMediaTypes(input);
        assertThat(actual.getAccept()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
        assertThat(actual.getContent()).isEqualTo("org.springframework.http.MediaType.APPLICATION_JSON");
    }

    @Test
    void extractQueryParams_multipleParams() {
        String input = ".target(\"/search\").queryParam(\"q\", searchTerm).queryParam(\"limit\", 10).get()";
        List<DropwizardCallParser.ParamInfo> actual = DropwizardCallParser.extractQueryParams(input);

        assertThat(actual).hasSize(2)
          .extracting("name", "value")
          .containsExactlyInAnyOrder(
            tuple("q", "searchTerm"),
            tuple("limit", "10")
          );
    }

    @Test
    void extractQueryParams_none() {
        String input = ".target(\"/search\").get()";
        List<DropwizardCallParser.ParamInfo> actual = DropwizardCallParser.extractQueryParams(input);
        assertThat(actual).isEmpty();
    }

    @Test
    void extractPathParams_single() {
        String input = ".target(\"/users/{id}\").pathParam(\"id\", userId).get()";
        List<DropwizardCallParser.ParamInfo> actual = DropwizardCallParser.extractPathParams(input);
        assertThat(actual).hasSize(1)
          .extracting("name", "value")
          .containsExactly(
            tuple("id", "userId")
          );
    }

    @Test
    void extractPathParams_none() {
        String input = ".target(\"/users\").get()";
        List<DropwizardCallParser.ParamInfo> actual = DropwizardCallParser.extractPathParams(input);
        assertThat(actual).isEmpty();
    }
}

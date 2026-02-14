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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MockitoVariableToMockBeanTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MockitoVariableToMockBean())
          .parser(JavaParser.fromJavaVersion().classpath("mockito-core", "spring-boot-starter-test"));
    }

    @Test
    void convertsMockitoToMockBean() {
        rewriteRun(
          java(
            """
              package com.example.dao;

              public interface PersonDAO {
              }
              """),
          java(
            """
                  import org.mockito.Mockito;
                  import com.example.dao.PersonDAO;

                  class TestClass {

                      @Deprecated
                      private static final PersonDAO PERSON_DAO = Mockito.mock(PersonDAO.class);

                      void testMethod() {
                          // some test code
                      }
                  }
              """,
            """
                  import com.example.dao.PersonDAO;
                  import org.springframework.boot.test.mock.mockito.MockBean;

                  class TestClass {

                      @MockBean
                      private PersonDAO PERSON_DAO;

                      void testMethod() {
                          // some test code
                      }
                  }
              """));
    }

    @Test
    void handlesMultipleMocks() {
        rewriteRun(
          java(
            """
              package com.example.service;

              public interface EmailService {
              }
              """),
          java(
            """
              package com.example.dao;

              public interface PersonDAO {
              }
              """),
          java(
            """
                  import org.mockito.Mockito;
                  import com.example.dao.PersonDAO;
                  import com.example.service.EmailService;

                  class TestClass {
                      private static final PersonDAO PERSON_DAO = Mockito.mock(PersonDAO.class);
                      private static final EmailService EMAIL_SERVICE = Mockito.mock(EmailService.class);

                      void testMethod() {
                          // some test code
                      }
                  }
              """,
            """
                  import com.example.dao.PersonDAO;
                  import com.example.service.EmailService;
                  import org.springframework.boot.test.mock.mockito.MockBean;

                  class TestClass {
                      @MockBean
                      private PersonDAO PERSON_DAO;
                      @MockBean
                      private EmailService EMAIL_SERVICE;

                      void testMethod() {
                          // some test code
                      }
                  }
              """));
    }

    @Test
    void ignoresNonMockFields() {
        rewriteRun(
          java(
            """
              package com.example.dao;

              public interface PersonDAO {
              }
              """),
          java(
            """
                  import org.mockito.Mockito;
                  import com.example.dao.PersonDAO;

                  class TestClass {
                      private static final PersonDAO PERSON_DAO = Mockito.mock(PersonDAO.class);
                      private static final String CONSTANT = "test";

                      void testMethod() {
                          // some test code
                      }
                  }
              """,
            """
                  import com.example.dao.PersonDAO;
                  import org.springframework.boot.test.mock.mockito.MockBean;

                  class TestClass {
                      @MockBean
                      private PersonDAO PERSON_DAO;
                      private static final String CONSTANT = "test";

                      void testMethod() {
                          // some test code
                      }
                  }
              """));
    }

    @Test
    void convertsMockitoStaticImportToMockBean() {
        rewriteRun(
          java(
            """
              package com.example.dao;

              public interface PersonDAO {
              }
              """),
          java(
            """
                  import static org.mockito.Mockito.mock;
                  import com.example.dao.PersonDAO;

                  class TestClass {

                      private static final PersonDAO PERSON_DAO = mock(PersonDAO.class);

                      void testMethod() {
                          // some test code
                      }
                  }
              """,
            """
                  import com.example.dao.PersonDAO;
                  import org.springframework.boot.test.mock.mockito.MockBean;

                  class TestClass {

                      @MockBean
                      private PersonDAO PERSON_DAO;

                      void testMethod() {
                          // some test code
                      }
                  }
              """));
    }
}

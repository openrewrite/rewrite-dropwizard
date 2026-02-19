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
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class TransformDropwizardRuleInvocationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new TransformDropwizardRuleInvocations())
          .parser(JavaParser.fromJavaVersion().classpath("dropwizard-testing", "dropwizard-core", "jersey-client", "spring-web", "javax.ws.rs-api", "jackson-databind").logCompilationWarningsAndErrors(false)

            .dependsOn("""
              package io.dropwizard.testing.junit;

              import javax.ws.rs.client.WebTarget;
              import javax.ws.rs.client.Client;

              public class DropwizardAppRule<C> {
                  public DropwizardAppRule(Class<C> configClass) {}

                  public int getLocalPort() {
                      return 8080;
                  }

                  public Client client() {
                      return null;
                  }

                  public WebTarget target(String uri) {
                      return null;
                  }
              }
              """, """
              package javax.ws.rs.client;

              public interface Client {
                  WebTarget target(String uri);
              }
              """, """
              package javax.ws.rs.client;

              public interface WebTarget {
                  Invocation.Builder request();
              }
              """, """
              package javax.ws.rs.client;

              import javax.ws.rs.core.Response;
              import javax.ws.rs.core.GenericType;

              public interface Invocation {
                  public interface Builder {
                      Builder header(String name, Object value);
                      Builder accept(String mediaType);

                      Response get();
                      <T> T get(Class<T> responseType);
                      <T> T get(GenericType<T> responseType);

                      Response post(Entity<?> entity);
                      <T> T post(Entity<?> entity, Class<T> responseType);
                      // <T> T post(Entity<?> entity, GenericType<T> responseType); // Optional

                      // --- Add other methods like put, delete if needed by tests ---
                      // Response delete();
                      // <T> T delete(Class<T> responseType);
                  }
              }
              """, """
              package javax.ws.rs.client;

              public class Entity<T> {
                  public static <T> Entity<T> entity(T entity, javax.ws.rs.core.MediaType mediaType) {
                      return new Entity<>(entity, mediaType);
                  }

                  private Entity(T entity, javax.ws.rs.core.MediaType mediaType) {}
              }
              """, """
              package javax.ws.rs.core;

              public class MediaType {
                  public static final MediaType APPLICATION_JSON_TYPE = new MediaType();
                  public static final String APPLICATION_JSON_VALUE = "application/json";
              }
              """, """
              package javax.ws.rs.core;

              public class Response {
                  public <T> T readEntity(Class<T> type) {
                      return null;
                  }
              }
              """, """
              public class Person {
                  private String name;

                  public Person(String name) {
                      this.name = name;
                  }

                  public String getName() {
                      return name;
                  }
              }
              """));
    }

    @DocumentExample
    @Test
    void shouldConvertSimplePostRequest() {
        rewriteRun(java(
                """
                        import io.dropwizard.testing.junit.DropwizardAppRule;
                        import org.springframework.web.client.RestTemplate;
                        import javax.ws.rs.client.Entity;
                        import javax.ws.rs.core.MediaType;
                        
                        class TestApi {
                            private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                            private final RestTemplate restTemplate = new RestTemplate();
                        
                            private Object postObject(Object person) {
                                return RULE.client().target("http://localhost:8080/people")
                                    .request()
                                    .post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE))
                                    .readEntity(Object.class);
                            }
                        }
                        """,
                """
          import io.dropwizard.testing.junit.DropwizardAppRule;
          import org.springframework.http.HttpEntity;
          import org.springframework.http.HttpHeaders;
          import org.springframework.http.HttpMethod;
          import org.springframework.http.MediaType;
          import org.springframework.web.client.RestTemplate;
          import javax.ws.rs.client.Entity;

          import java.util.Collections;

          class TestApi {
              private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
              private final RestTemplate restTemplate = new RestTemplate();

              private Object postObject(Object person) {
                  return restTemplate.exchange("/people", HttpMethod.POST, new HttpEntity<>(person, new HttpHeaders() {
                      {
                          setContentType(MediaType.APPLICATION_JSON);
                          setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                      }
                  }), java.lang.Object.class).getBody();
              }
          }
            """));
    }

    @Test
    void shouldConvertSimpleGetRequest() {
        rewriteRun(java(
                """
            import io.dropwizard.testing.junit.DropwizardAppRule;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.web.client.RestTemplate;

            class TestApi {
                private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                private final ObjectMapper objectMapper = new ObjectMapper();
                private final RestTemplate restTemplate = new RestTemplate();

                void test() {
                    Object person = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/people")
                        .request()
                        .header("Authorization", "Bearer someToken")
                        .get(Object.class);
                }
            }
            """,
          """
            import io.dropwizard.testing.junit.DropwizardAppRule;
            import org.springframework.http.HttpEntity;
            import org.springframework.http.HttpHeaders;
            import org.springframework.http.HttpMethod;
            import org.springframework.http.MediaType;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.web.client.RestTemplate;

            import java.util.Collections;

            class TestApi {
                private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                private final ObjectMapper objectMapper = new ObjectMapper();
                private final RestTemplate restTemplate = new RestTemplate();

                void test() {
                    java.lang.Object person = restTemplate.exchange("/people", HttpMethod.GET, new HttpEntity<>(null, new HttpHeaders() {
                        {
                            setContentType(MediaType.APPLICATION_JSON);
                            setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                            set(HttpHeaders.AUTHORIZATION, "Bearer someToken");
                        }
                    }), java.lang.Object.class).getBody();
                }
            }
              """));
    }

    @Test
    void shouldConvertGenericTypeToParametrized() {
        rewriteRun(java(
                """
          package com.example.helloworld.resources;

          import io.dropwizard.testing.junit.DropwizardAppRule;
          import javax.ws.rs.client.Entity;
          import javax.ws.rs.core.GenericType;
          import javax.ws.rs.core.MediaType;
          import java.util.List;
          import org.springframework.web.client.RestTemplate;

          class TestApi {
              private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
              private final RestTemplate restTemplate = new RestTemplate();

              void test() {
                  // This returns a List<Person> using a GenericType
                  List<Object> people = RULE.client()
                      .target("http://localhost:" + RULE.getLocalPort() + "/people")
                      .request()
                      .get(new GenericType<List<Object>>() {});
              }
          }
          """,
                """
          package com.example.helloworld.resources;

          import io.dropwizard.testing.junit.DropwizardAppRule;
          import org.springframework.core.ParameterizedTypeReference;
          import org.springframework.http.HttpEntity;
          import org.springframework.http.HttpHeaders;
          import org.springframework.http.HttpMethod;

          import javax.ws.rs.client.Entity;
          import javax.ws.rs.core.GenericType;

          import java.util.Collections;
          import java.util.List;
          import org.springframework.web.client.RestTemplate;

          class TestApi {
              private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
              private final RestTemplate restTemplate = new RestTemplate();

              void test() {
                  // This returns a List<Person> using a GenericType
                  java.util.List<java.lang.Object> people = restTemplate.exchange("/people", HttpMethod.GET, new HttpEntity<>(null, new HttpHeaders() {
                      {
                          setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                          setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
                      }
                  }), new ParameterizedTypeReference<java.util.List<java.lang.Object>>() {
                  }).getBody();
              }
          }
            """));
    }

    @Test
    void shouldConvertPostRequest() {
        rewriteRun(java(
                """
          import io.dropwizard.testing.junit.DropwizardAppRule;
          import org.springframework.web.client.RestTemplate;
          import javax.ws.rs.client.Entity;
          import javax.ws.rs.core.MediaType;

          class TestApi {
              private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
              private final RestTemplate restTemplate = new RestTemplate();

              void test() {
                  Object person = new Object();
                  Object response = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/people")
                      .request()
                      .post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE))
                      .readEntity(Object.class);
              }
          }
          """,
                """
                        import io.dropwizard.testing.junit.DropwizardAppRule;
                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;
                        import javax.ws.rs.client.Entity;
                      
                        import java.util.Collections;
                      
                        class TestApi {
                            private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                            private final RestTemplate restTemplate = new RestTemplate();
                      
                            void test() {
                                Object person = new Object();
                                java.lang.Object response = restTemplate.exchange("/people", HttpMethod.POST, new HttpEntity<>(person, new HttpHeaders() {
                                    {
                                        setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                                        setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
                                    }
                                }), java.lang.Object.class).getBody();
                            }
                        }
                        """));
    }

    @Test
    void shouldConvertInvocationWithinTryCatch() {
        rewriteRun(java(
                """
                        import io.dropwizard.testing.junit.DropwizardAppRule;
                        import org.springframework.web.client.RestTemplate;
                        import org.springframework.http.HttpHeaders;
                        
                        class TestApi {
                            private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                            private final RestTemplate restTemplate = new RestTemplate();
                        
                            void test() {
                                try {
                                    RULE.client().target("/protected/admin").request()
                                        .header(HttpHeaders.AUTHORIZATION, "Basic Z29vZC1ndXk6c2VjcmV0")
                                        .get(String.class);
                                } catch (Exception e) {
                                }
                            }
                        }
                        """,
                """
          import io.dropwizard.testing.junit.DropwizardAppRule;
          import org.springframework.http.HttpEntity;
          import org.springframework.http.HttpMethod;
          import org.springframework.http.MediaType;
          import org.springframework.web.client.RestTemplate;
          import org.springframework.http.HttpHeaders;

          import java.util.Collections;

          class TestApi {
              private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
              private final RestTemplate restTemplate = new RestTemplate();

              void test() {
                  try {
                      restTemplate.exchange("/protected/admin", HttpMethod.GET, new HttpEntity<>(null, new org.springframework.http.HttpHeaders() {
                          {
                              setContentType(MediaType.APPLICATION_JSON);
                              setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                              set(HttpHeaders.AUTHORIZATION, "Basic Z29vZC1ndXk6c2VjcmV0");
                          }
                      }), java.lang.String.class).getBody();
                  } catch (Exception e) {
                  }
              }
          }
            """));
    }

    @Test
    void shouldConvertResponseToResponseEntity() {
        rewriteRun(java(
                """
            import io.dropwizard.testing.junit.DropwizardAppRule;
            import org.springframework.web.client.RestTemplate;
            import org.springframework.http.HttpHeaders;
            import javax.ws.rs.core.Response;

            class TestApi {
                private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                private final RestTemplate restTemplate = new RestTemplate();

                void test() {
                    Response response = RULE.client().target("/people/2").request().get();
                }
            }
            """,
          """
            import io.dropwizard.testing.junit.DropwizardAppRule;
            import org.springframework.http.HttpEntity;
            import org.springframework.http.HttpMethod;
            import org.springframework.http.MediaType;
            import org.springframework.http.ResponseEntity;
            import org.springframework.web.client.RestTemplate;
            import org.springframework.http.HttpHeaders;

            import java.util.Collections;

            class TestApi {
                private final DropwizardAppRule<Object> RULE = new DropwizardAppRule<>(Object.class);
                private final RestTemplate restTemplate = new RestTemplate();

                void test() {
                    ResponseEntity<Object> response = restTemplate.exchange("/people/2", HttpMethod.GET, new HttpEntity<>(null, new org.springframework.http.HttpHeaders() {
                        {
                            setContentType(MediaType.APPLICATION_JSON);
                            setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                        }
                    }), java.lang.Object.class, Collections.emptyMap());
                }
            }
              """));
    }
}

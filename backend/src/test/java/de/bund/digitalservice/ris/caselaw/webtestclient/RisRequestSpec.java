package de.bund.digitalservice.ris.caselaw.webtestclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

public class RisRequestSpec {
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final OidcLoginRequestPostProcessor login;
  private final Cookie csrfCookie;
  private HttpMethod httpMethod;
  private URI uri;
  private Supplier<?> bodySupplier;
  private String json;
  private byte[] bodyAsBytes;
  private MediaType mediaType;

  public RisRequestSpec(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      OidcLoginRequestPostProcessor login,
      Cookie csrfCookie) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.login = login;
    this.csrfCookie = csrfCookie;
  }

  public RisRequestSpec get() {
    this.httpMethod = HttpMethod.GET;
    return this;
  }

  public RisRequestSpec put() {
    this.httpMethod = HttpMethod.PUT;
    return this;
  }

  public RisRequestSpec delete() {
    this.httpMethod = HttpMethod.DELETE;
    return this;
  }

  public RisRequestSpec post() {
    this.httpMethod = HttpMethod.POST;
    return this;
  }

  public RisRequestSpec patch() {
    this.httpMethod = HttpMethod.PATCH;
    return this;
  }

  public RisRequestSpec uri(String uri) {
    this.uri = UriComponentsBuilder.fromUriString(uri).buildAndExpand().encode().toUri();

    return this;
  }

  public RisRequestSpec uri(URI uri) {
    this.uri = uri;

    return this;
  }

  public <T> RisRequestSpec bodyValue(T body) {
    this.bodySupplier = () -> body;
    return this;
  }

  public RisRequestSpec bodyJsonString(String json) {
    this.json = json;
    return this;
  }

  public RisRequestSpec bodyAsByteArray(byte[] bytes) {
    this.bodyAsBytes = bytes;
    return this;
  }

  public RisRequestSpec contentType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public RisResponseSpec exchange() {
    MockHttpServletRequestBuilder request = null;

    if (httpMethod.equals(HttpMethod.GET)) {
      request = MockMvcRequestBuilders.get(uri);
    } else if (httpMethod.equals(HttpMethod.PUT)) {
      request = MockMvcRequestBuilders.put(uri);
    } else if (httpMethod.equals(HttpMethod.POST)) {
      request = MockMvcRequestBuilders.post(uri);
    } else if (httpMethod.equals(HttpMethod.DELETE)) {
      request = MockMvcRequestBuilders.delete(uri);
    } else if (httpMethod.equals(HttpMethod.PATCH)) {
      request = MockMvcRequestBuilders.patch(uri);
    }

    if (request == null) {
      return new RisResponseSpec();
    }

    request.header("X-XSRF-TOKEN", csrfCookie.getValue()).cookie(csrfCookie);
    //    request.with(csrf());

    if (login != null) {
      request.with(login);
    }

    request.contentType(Objects.requireNonNullElse(mediaType, MediaType.APPLICATION_JSON));

    if (bodySupplier != null) {
      try {
        String jsonString = objectMapper.writeValueAsString(bodySupplier.get());
        request.content(jsonString);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    } else if (json != null) {
      request.content(json);
    } else if (bodyAsBytes != null) {
      request.content(bodyAsBytes);
    }

    ResultActions resultActions;
    try {
      resultActions = mockMvc.perform(request);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new RisResponseSpec(resultActions, objectMapper);
  }
}

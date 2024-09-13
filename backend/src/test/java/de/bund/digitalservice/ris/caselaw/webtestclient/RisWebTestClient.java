package de.bund.digitalservice.ris.caselaw.webtestclient;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.getMockLogin;
import static de.bund.digitalservice.ris.caselaw.AuthUtils.getMockLoginExternal;
import static de.bund.digitalservice.ris.caselaw.AuthUtils.getMockLoginWithDocOffice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class RisWebTestClient {
  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final Cookie csrfCookie;

  public RisWebTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;

    try {
      MockHttpServletRequestBuilder csrfRequestBuilder = MockMvcRequestBuilders.get("/csrf");
      MvcResult mvcResult = this.mockMvc.perform(csrfRequestBuilder).andReturn();
      csrfCookie = mvcResult.getResponse().getCookie("XSRF-TOKEN");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public RisRequestSpec withDefaultLogin() {
    return new RisRequestSpec(mockMvc, objectMapper, getMockLogin(), csrfCookie);
  }

  public RisRequestSpec withExternalLogin() {
    return new RisRequestSpec(mockMvc, objectMapper, getMockLoginExternal(), csrfCookie);
  }

  public RisRequestSpec withLogin(String docOfficeGroup) {
    return withLogin(docOfficeGroup, "Internal");
  }

  public RisRequestSpec withLogin(String docOfficeGroup, String role) {
    return new RisRequestSpec(
        mockMvc, objectMapper, getMockLoginWithDocOffice(docOfficeGroup, role), csrfCookie);
  }
}

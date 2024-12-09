package de.bund.digitalservice.ris.caselaw.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class RedirectingAuthenticationFailureHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response,
      AuthenticationException exception)
      throws IOException {

    response.sendRedirect("/error");
  }
}

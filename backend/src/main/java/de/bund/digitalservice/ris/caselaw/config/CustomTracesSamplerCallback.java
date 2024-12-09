package de.bund.digitalservice.ris.caselaw.config;

import io.sentry.SamplingContext;
import io.sentry.SentryOptions.TracesSamplerCallback;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CustomTracesSamplerCallback implements TracesSamplerCallback {
  @Override
  public @Nullable Double sample(@NotNull SamplingContext context) {
    if (context.getCustomSamplingContext() == null) {
      return null;
    }

    HttpServletRequest request =
        (HttpServletRequest) context.getCustomSamplingContext().get("request");

    if (request == null) {
      return null;
    }

    String url = request.getRequestURI();

    if (url.startsWith("/actuator") || request.getMethod().equals("PATCH")) {
      return 0d;
    } else {
      return null;
    }
  }
}

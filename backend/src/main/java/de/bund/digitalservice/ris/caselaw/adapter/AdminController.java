package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.MailTrackingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/admin")
@Slf4j
public class AdminController {

  private final MailTrackingService mailTrackingService;
  private final EnvironmentService environmentService;
  private final LdmlExporterService ldmlExporterService;

  @Autowired
  public AdminController(
      MailTrackingService mailTrackingService,
      EnvironmentService environmentService,
      LdmlExporterService ldmlExporterService) {
    this.mailTrackingService = mailTrackingService;
    this.environmentService = environmentService;
    this.ldmlExporterService = ldmlExporterService;
  }

  @PostMapping("/webhook")
  @PreAuthorize("permitAll")
  public ResponseEntity<String> trackMail(@RequestBody @Valid MailTrackingResponsePayload payload) {
    if (payload != null && payload.tags() != null && !payload.tags().isEmpty()) {
      return mailTrackingService.processMailSendingState(payload.tags().get(0), payload.event());
    }
    return ResponseEntity.badRequest().build();
  }

  @GetMapping("/env")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> getEnvironment() {
    return ResponseEntity.ok(environmentService.getEnvironment());
  }

  @GetMapping("/ldml")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> createLdml() {
    ldmlExporterService.exportMultipleRandomDocumentationUnits();
    return ResponseEntity.ok().build();
  }

  @GetMapping("/accountManagementUrl")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> getAccountManagementUrl() {
    return ResponseEntity.ok(environmentService.getAccountManagementUrl());
  }
}

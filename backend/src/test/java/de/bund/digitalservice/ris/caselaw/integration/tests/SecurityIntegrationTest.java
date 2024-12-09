package de.bund.digitalservice.ris.caselaw.integration.tests;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AdminController;
import de.bund.digitalservice.ris.caselaw.adapter.EnvironmentService;
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.MailTrackingService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@RISIntegrationTest(
    imports = {
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      TestConfig.class,
    },
    controllers = {AdminController.class})
class SecurityIntegrationTest {

  @Container
  static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14").withInitScript("init_db.sql");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @Autowired RisWebTestClient webTestClient;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean MailTrackingService mailTrackingService;
  @MockBean EnvironmentService environmentService;
  @MockBean LdmlExporterService ldmlExporterService;

  @Test
  void shouldHaveEnabledCSPHeader() {
    webTestClient
        .withDefaultLogin()
        .get()
        .uri("/")
        .exchange()
        .expectHeader()
        .valueEquals(
            "Content-Security-Policy",
            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-eval'; connect-src 'self' *.sentry.io data:");
  }

  @Test
  void shouldHaveEnabledXFrameOptionsHeader() {
    webTestClient
        .withDefaultLogin()
        .get()
        .uri("/")
        .exchange()
        .expectHeader()
        .valueEquals("X-Frame-Options", "SAMEORIGIN");
  }

  @Test
  void shouldHaveEnabledXContentTypeOptionsHeader() {
    webTestClient
        .withDefaultLogin()
        .get()
        .uri("/")
        .exchange()
        .expectHeader()
        .valueEquals("X-Content-Type-Options", "nosniff");
  }

  @Test
  void shouldHaveEnabledReferrerPolicyHeader() {
    webTestClient
        .withDefaultLogin()
        .get()
        .uri("/")
        .exchange()
        .expectHeader()
        .valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
  }

  @Test
  void shouldHaveEnabledPermissionsPolicyHeader() {
    webTestClient
        .withDefaultLogin()
        .get()
        .uri("/")
        .exchange()
        .expectHeader()
        .valueEquals(
            "Permissions-Policy",
            "accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), cross-origin-isolated=(), "
                + "display-capture=(), document-domain=(), encrypted-media=(), execution-while-not-rendered=(), "
                + "execution-while-out-of-viewport=(), fullscreen=(), geolocation=(), gyroscope=(), keyboard-map=(), "
                + "magnetometer=(), microphone=(), midi=(), navigation-override=(), payment=(), picture-in-picture=(), "
                + "publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(), usb=(), web-share=(), xr-spatial-tracking=(), "
                + "clipboard-read=(self), clipboard-write=(self), gamepad=(), speaker-selection=(), conversion-measurement=(), "
                + "focus-without-user-activation=(self), hid=(), idle-detection=(), interest-cohort=(), serial=(), sync-script=(), "
                + "trust-token-redemption=(), window-placement=(), vertical-scroll=(self)");
  }
}

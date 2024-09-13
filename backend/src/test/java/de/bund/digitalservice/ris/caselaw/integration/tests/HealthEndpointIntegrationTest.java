package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "otc.obs.bucket-name=testBucket",
      "otc.obs.endpoint=testUrl",
      "local.file-storage=.local-storage",
      "mail.from.address=test@test.com",
      "management.endpoint.health.probes.enabled=true",
      "management.health.livenessState.enabled=true",
      "management.health.readinessState.enabled=true",
      "management.endpoint.health.group.readiness.include=readinessState,db,redis"
    })
@Tag("integration")
@AutoConfigureMockMvc
class HealthEndpointIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14").withInitScript("init_db.sql");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7.0")).withExposedPorts(6379);

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());

    registry.add("spring.data.redis.host", () -> redis.getHost());
    registry.add("spring.data.redis.port", () -> redis.getFirstMappedPort());
    registry.add("spring.data.redis.timeout", () -> "200");
  }

  @Autowired MockMvc mockMvc;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean UserGroupService userGroupService;

  @Test
  void shouldExposeHealthEndpoint() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
  }

  @Test
  void shouldBeUnhealthyWithoutRedis() throws Exception {
    redis.stop();
    mockMvc.perform(get("/actuator/health")).andExpect(status().is5xxServerError());
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().is5xxServerError());
  }
}

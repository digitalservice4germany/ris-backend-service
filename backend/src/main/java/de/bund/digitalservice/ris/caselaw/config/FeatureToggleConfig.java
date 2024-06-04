package de.bund.digitalservice.ris.caselaw.config;

import de.bund.digitalservice.ris.caselaw.adapter.UnleashService;
import de.bund.digitalservice.ris.caselaw.domain.CurrentEnv;
import de.bund.digitalservice.ris.caselaw.domain.FeatureToggleService;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FeatureToggleConfig {
  @Value("${unleash.appName:unleash-proxy}")
  private String appName;

  @Value("${unleash.apiUrl:}")
  private String apiUrl;

  @Value("${unleash.apiToken:}")
  private String apiToken;

  public Unleash unleash() {
    UnleashConfig config =
        UnleashConfig.builder().appName(appName).unleashAPI(apiUrl).apiKey(apiToken).build();

    return new DefaultUnleash(config);
  }

  @Bean
  @Profile({"production", "uat", "staging"})
  public FeatureToggleService featureToggleService(CurrentEnv env) {
    return new UnleashService(unleash(), env.name());
  }

  @Bean
  @Profile({"!production & !staging & !uat"})
  public FeatureToggleService featureToggleServiceMock() {
    return new FeatureToggleService() {
      @Override
      public List<String> getFeatureToggleNames() {
        return Collections.emptyList();
      }

      @Override
      public boolean isEnabled(String toggleName) {
        return true;
      }
    };
  }
}

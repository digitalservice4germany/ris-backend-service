package de.bund.digitalservice.ris.caselaw.config;

import de.bund.digitalservice.ris.caselaw.adapter.UserGroupFromConfig;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserGroupsConfig {

  /**
   * Caution: Changing values will delete and recreate the affected group in our database. This
   * means all references to the affected group will be deleted, e.g., the links of the affected
   * user group to procedures will be cleared.
   */
  @Bean
  public List<UserGroupFromConfig> getDocumentationOfficeConfigUserGroups() {
    return List.of(
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BGH")
            .userGroupPathName("/caselaw/BGH")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BGH")
            .userGroupPathName("/caselaw/BGH/Intern")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BGH")
            .userGroupPathName("/caselaw/BGH/Extern/Miotke")
            .isInternal(false)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BGH")
            .userGroupPathName("/caselaw/BGH/Extern/Busenks")
            .isInternal(false)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BVerfG")
            .userGroupPathName("/caselaw/BVerfG")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BAG")
            .userGroupPathName("/caselaw/BAG")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BFH")
            .userGroupPathName("/caselaw/BFH")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BSG")
            .userGroupPathName("/caselaw/BSG")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BPatG")
            .userGroupPathName("/caselaw/BPatG")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BVerwG")
            .userGroupPathName("/caselaw/BVerwG")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("OVGNW")
            .userGroupPathName("/caselaw/OVG_NRW")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("BZSt")
            .userGroupPathName("/caselaw/BZSt")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("DS")
            .userGroupPathName("/DS")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("DS")
            .userGroupPathName("/DS/Intern")
            .isInternal(true)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("DS")
            .userGroupPathName("/DS/Extern")
            .isInternal(false)
            .build(),
        UserGroupFromConfig.builder()
            .docOfficeAbbreviation("CC-RIS")
            .userGroupPathName("/CC-RIS")
            .isInternal(true)
            .build());
  }
}

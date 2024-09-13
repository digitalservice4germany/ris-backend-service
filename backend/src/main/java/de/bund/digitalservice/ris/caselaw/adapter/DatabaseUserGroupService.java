package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.UserGroupDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.UserGroupTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserGroupService implements UserGroupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUserGroupService.class);
  private final DatabaseUserGroupRepository repository;
  private final DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  private List<UserGroupDTO> userGroups;

  private final List<UserGroupFromConfig> userGroupsFromConfig;

  public DatabaseUserGroupService(
      DatabaseUserGroupRepository repository,
      DatabaseDocumentationOfficeRepository documentationOfficeRepository,
      List<UserGroupFromConfig> documentationOfficeConfigUserGroups) {
    this.repository = repository;
    this.documentationOfficeRepository = documentationOfficeRepository;
    this.userGroups = new ArrayList<>();
    this.userGroupsFromConfig = documentationOfficeConfigUserGroups;
  }

  /**
   * On application start, we want to
   *
   * <ol>
   *   <li>sync the user group database with the statically configured list of user groups
   *   <li>load the user groups into memory for performance reasons (-> right checks)
   * </ol>
   */
  @EventListener
  public void onApplicationEvent(ContextRefreshedEvent event) {
    this.userGroups = this.repository.findAll();
    var docOffices = this.documentationOfficeRepository.findAll();

    var userGroupsToBeDeleted = userGroups.stream().filter(this::groupIsNotInConfig).toList();
    if (!userGroupsToBeDeleted.isEmpty()) {
      String groupsString = groupsToString(userGroupsToBeDeleted);
      LOGGER.info("Deleting doc office user groups: {}", groupsString);
      this.repository.deleteAll(userGroupsToBeDeleted);
    }

    var userGroupsToBeCreated =
        userGroupsFromConfig.stream()
            .filter(this::isGroupNotInDatabase)
            .map(groupFromConfig -> transformToDTO(groupFromConfig, docOffices))
            .toList();
    if (!userGroupsToBeCreated.isEmpty()) {
      String groupsString = groupsToString(userGroupsToBeCreated);
      LOGGER.info("Creating new doc office user groups: {}", groupsString);
      this.repository.saveAll(userGroupsToBeCreated);
    }

    this.userGroups = this.repository.findAll();
  }

  /**
   * Retrieves all {@link UserGroup user group} as domain objects.
   *
   * <p>This method transforms a list of user groups from internal representation to domain objects
   * using the {@link UserGroupTransformer}. It returns a list of these transformed user groups.
   *
   * @return a {@link List} of {@link UserGroup} in domain form
   */
  @Override
  public List<UserGroup> getAllUserGroups() {
    return this.userGroups.stream().map(UserGroupTransformer::transformToDomain).toList();
  }

  /**
   * Retrieves a list of external {@link UserGroup user group} associated with a given {@link
   * DocumentationOffice}.
   *
   * <p>This method filters all {@link UserGroup user group} to find those associated with the
   * specified {@link DocumentationOffice} and that are not marked as internal. It returns a list of
   * these external {@link UserGroup user group}s.
   *
   * @param documentationOffice the {@link DocumentationOffice} for which external user groups are
   *     to be retrieved
   * @return a {@link List} of {@link UserGroup} that are external and associated with the given
   *     {@link DocumentationOffice}
   */
  @Override
  public List<UserGroup> getExternalUserGroups(DocumentationOffice documentationOffice) {
    return getAllUserGroups().stream()
        .filter(group -> group.docOffice().equals(documentationOffice) && !group.isInternal())
        .toList();
  }

  private boolean isGroupNotInDatabase(UserGroupFromConfig groupFromConfig) {
    return this.userGroups.stream()
        .noneMatch(groupFromDb -> isConfigEqualToGroupFromDb(groupFromConfig, groupFromDb));
  }

  private boolean groupIsNotInConfig(UserGroupDTO groupFromDb) {
    return this.userGroupsFromConfig.stream()
        .noneMatch(groupFromConfig -> isConfigEqualToGroupFromDb(groupFromConfig, groupFromDb));
  }

  private boolean isConfigEqualToGroupFromDb(
      UserGroupFromConfig groupFromConfig, UserGroupDTO groupFromDb) {
    return groupFromConfig.userGroupPathName().equals(groupFromDb.getUserGroupPathName())
        && groupFromConfig.isInternal() == groupFromDb.isInternal()
        && groupFromConfig
            .docOfficeAbbreviation()
            .equals(groupFromDb.getDocumentationOffice().getAbbreviation());
  }

  /**
   * Will throw if a doc office for a configured user group does not exist -> Application won't
   * start. Make sure to mock this class in tests.
   */
  private @NotNull DocumentationOfficeDTO getMatchingDocumentationOffice(
      UserGroupFromConfig groupFromConfig, List<DocumentationOfficeDTO> docOffices) {
    return docOffices.stream()
        .filter(office -> office.getAbbreviation().equals(groupFromConfig.docOfficeAbbreviation()))
        .findFirst()
        .orElseThrow();
  }

  private UserGroupDTO transformToDTO(
      UserGroupFromConfig groupFromConfig, List<DocumentationOfficeDTO> docOffices) {
    return UserGroupDTO.builder()
        .documentationOffice(this.getMatchingDocumentationOffice(groupFromConfig, docOffices))
        .userGroupPathName(groupFromConfig.userGroupPathName())
        .isInternal(groupFromConfig.isInternal())
        .build();
  }

  private static String groupsToString(List<UserGroupDTO> userGroupsToBeCreated) {
    return userGroupsToBeCreated.stream()
        .map(UserGroupDTO::getUserGroupPathName)
        .collect(Collectors.joining(", "));
  }
}

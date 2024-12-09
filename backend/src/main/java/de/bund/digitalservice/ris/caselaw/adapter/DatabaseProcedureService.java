package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.UserGroupDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationOfficeTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitListItemTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.ProcedureTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseProcedureService implements ProcedureService {
  private final DatabaseProcedureRepository repository;
  private final DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  private final DatabaseUserGroupRepository userGroupRepository;

  private final UserService userService;

  public DatabaseProcedureService(
      DatabaseProcedureRepository repository,
      DatabaseDocumentationOfficeRepository documentationOfficeRepository,
      DatabaseUserGroupRepository userGroupRepository,
      UserService userService) {
    this.repository = repository;
    this.documentationOfficeRepository = documentationOfficeRepository;
    this.userGroupRepository = userGroupRepository;
    this.userService = userService;
  }

  @Override
  @Transactional
  public Slice<Procedure> search(
      Optional<String> query,
      DocumentationOffice documentationOffice,
      Pageable pageable,
      Optional<Boolean> withDocUnits,
      OidcUser oidcUser) {

    DocumentationOfficeDTO documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(documentationOffice.abbreviation());
    boolean shouldGetAllProcedures = !(withDocUnits.isPresent() && withDocUnits.get());

    if (shouldGetAllProcedures) {
      // retrieve all procedures (even those without linked documentation units)
      return getAllProcedures(query, pageable, documentationOfficeDTO);
    }
    // retrieve only procedures documentation units have been linked to
    return getOnlyLinkedProcedures(query, pageable, oidcUser, documentationOfficeDTO);
  }

  @NotNull
  private Slice<Procedure> getAllProcedures(
      Optional<String> query, Pageable pageable, DocumentationOfficeDTO documentationOfficeDTO) {
    return query
        .map(
            queryString ->
                repository.findAllByLabelContainingAndDocumentationOfficeOrderByCreatedAtDesc(
                    queryString.trim(), documentationOfficeDTO, pageable))
        .orElseGet(
            () ->
                repository.findAllByDocumentationOfficeOrderByCreatedAtDesc(
                    documentationOfficeDTO, pageable))
        .map(ProcedureTransformer::transformToDomain);
  }

  @NotNull
  private Slice<Procedure> getOnlyLinkedProcedures(
      Optional<String> query,
      Pageable pageable,
      OidcUser oidcUser,
      DocumentationOfficeDTO documentationOfficeDTO) {
    var userGroup = userService.getUserGroup(oidcUser);
    boolean isInternalUser = userService.isInternal(oidcUser);
    if (!isInternalUser && userGroup.isPresent()) {
      return getProceduresOfUserGroup(
          query, pageable, documentationOfficeDTO, userGroup.get().id());
    }
    return query
        .map(
            queryString ->
                repository.findLatestUsedProceduresByLabelAndDocumentationOffice(
                    queryString.trim(), documentationOfficeDTO, pageable))
        .orElseGet(
            () ->
                repository.findLatestUsedProceduresByDocumentationOffice(
                    documentationOfficeDTO, pageable))
        .map(ProcedureTransformer::transformToDomain);
  }

  @NotNull
  private Slice<Procedure> getProceduresOfUserGroup(
      Optional<String> query,
      Pageable pageable,
      DocumentationOfficeDTO documentationOfficeDTO,
      UUID userGroupId) {
    return query
        .map(
            queryString ->
                repository.findLatestUsedProceduresByLabelAndDocumentationOfficeAndUserGroupDTO_Id(
                    queryString.trim(), documentationOfficeDTO, userGroupId, pageable))
        .orElseGet(
            () ->
                repository.findLatestUsedProceduresByDocumentationOfficeAndUserGroupDTO_id(
                    documentationOfficeDTO, userGroupId, pageable))
        .map(ProcedureTransformer::transformToDomain);
  }

  @Override
  @Transactional
  public List<DocumentationUnitListItem> getDocumentationUnits(
      UUID procedureId, OidcUser oidcUser) {
    DocumentationOffice documentationOfficeOfUser = userService.getDocumentationOffice(oidcUser);
    boolean isInternalUser = userService.isInternal(oidcUser);
    return repository
        .findByIdAndDocumentationOffice(procedureId, documentationOfficeOfUser.uuid())
        .map(
            procedureDTO ->
                procedureDTO.getDocumentationUnits().stream()
                    .filter(
                        documentationUnitDTO -> {
                          List<DocumentationUnitProcedureDTO> procedures =
                              documentationUnitDTO.getProcedures();
                          return procedures
                              .get(procedures.size() - 1)
                              .getProcedure()
                              .equals(procedureDTO);
                        })
                    .distinct()
                    .map(DocumentationUnitListItemTransformer::transformToDomain)
                    .map(
                        documentationUnitListItem ->
                            documentationUnitListItem.toBuilder()
                                .isDeletable(isInternalUser)
                                .isEditable(
                                    isInternalUser || isUserAssigned(procedureDTO, oidcUser))
                                .build())
                    .toList())
        .orElse(null);
  }

  /**
   * Checks if a {@link OidcUser} is assigned to a {@link ProcedureDTO} based on their {@link
   * UserGroupDTO user group}.
   *
   * <p>This method compares the {@link UserGroupDTO user group} associated with the provided {@link
   * OidcUser} to the {@link UserGroupDTO user group} assigned to the {@link ProcedureDTO}. It
   * returns {@link Boolean#TRUE true} if the {@link UserGroupDTO user group}'s ID matches the
   * {@link ProcedureDTO}'s assigned group ID, otherwise {@link Boolean#FALSE false}.
   *
   * @param procedureDTO the {@link ProcedureDTO} containing the {@link DocumentationOffice} user
   *     group
   * @param oidcUser the {@link OidcUser} whose user group is being checked
   * @return {@link Boolean#TRUE true} if the user's group is assigned to the procedure, otherwise
   *     {@link Boolean#FALSE false}
   */
  private Boolean isUserAssigned(ProcedureDTO procedureDTO, OidcUser oidcUser) {
    Optional<UserGroup> userGroup = userService.getUserGroup(oidcUser);
    if (userGroup.isPresent() && procedureDTO.getUserGroupDTO() != null) {
      return userGroup.get().id().equals(procedureDTO.getUserGroupDTO().getId());
    }
    return false;
  }

  /**
   * Assigns a {@link UserGroupDTO user group} to a {@link ProcedureDTO procedure} identified by
   * their {@link UUID}s.
   *
   * <p>This method associates the specified {@link UserGroupDTO user group} with a {@link
   * ProcedureDTO procedure} by updating the {@link ProcedureDTO procedure}'s {@link UserGroupDTO
   * user group}. It validates the existence of both entities in the database. If either is missing,
   * it throws an {@link IllegalArgumentException}.
   *
   * @param procedureUUID the UUID of the {@link ProcedureDTO procedure} to which the {@link
   *     UserGroupDTO user group} will be assigned
   * @param userGroupId the UUID of the {@link UserGroupDTO user group} to be assigned to the {@link
   *     ProcedureDTO procedure}
   * @return a confirmation message indicating the {@link ProcedureDTO procedure} and {@link
   *     UserGroupDTO user group} that were associated
   * @throws IllegalArgumentException if the {@link ProcedureDTO procedure} or {@link UserGroupDTO
   *     user group} is not found in the database
   */
  @Override
  public String assignUserGroup(UUID procedureUUID, UUID userGroupId) {
    Optional<ProcedureDTO> procedureDTO = repository.findById(procedureUUID);
    Optional<UserGroupDTO> userGroupDTO = userGroupRepository.findById(userGroupId);
    if (procedureDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be assigned as procedure is missing in the data base.");
    }
    if (userGroupDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be assigned as user group is missing in the data base.");
    }
    ProcedureDTO result = procedureDTO.get();
    result.setUserGroupDTO(userGroupDTO.get());
    repository.save(result);
    return "Vorgang '"
        + procedureDTO.get().getLabel()
        + "' wurde Nutzergruppe '"
        + userGroupDTO.get().getUserGroupPathName()
        + "' zugewiesen.";
  }

  /**
   * Unassign a {@link UserGroupDTO user group} from a {@link ProcedureDTO procedure} identified by
   * its {@link UUID}.
   *
   * <p>This method removes the association of the {@link UserGroupDTO user group} with the {@link
   * ProcedureDTO procedure}. It validates the existence of the {@link ProcedureDTO procedure} in
   * the database. If it is missing, it throws an {@link IllegalArgumentException}.
   *
   * @param procedureUUID the UUID of the {@link ProcedureDTO procedure} from which the {@link
   *     UserGroupDTO user group} will be removed
   * @return a confirmation message mentioning the {@link ProcedureDTO procedure}
   * @throws IllegalArgumentException if the {@link ProcedureDTO procedure} is not found in the
   *     database
   */
  @Override
  public String unassignUserGroup(UUID procedureUUID) {
    Optional<ProcedureDTO> procedureDTO = repository.findById(procedureUUID);

    if (procedureDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be unassigned as procedure is missing in the data base.");
    }

    ProcedureDTO result = procedureDTO.get();
    result.setUserGroupDTO(null);
    repository.save(result);
    return "Die Zuweisung aus Vorgang '" + procedureDTO.get().getLabel() + "' wurde entfernt.";
  }

  /**
   * Retrieves the {@link DocumentationOffice} associated with a {@link ProcedureDTO procedure} by
   * its {@link UUID}.
   *
   * <p>This method fetches the {@link ProcedureDTO procedure} from the repository using the
   * provided {@link UUID}, and transforms the associated {@link DocumentationOffice} from a {@link
   * ProcedureDTO} to a domain object {@link DocumentationOffice}. If the {@link ProcedureDTO
   * procedure} is not found, it returns {@code null}.
   *
   * @param procedureId the {@link UUID} of the {@link ProcedureDTO procedure} whose {@link
   *     DocumentationOffice} is to be retrieved
   * @return the {@link DocumentationOffice} associated with the {@link ProcedureDTO procedure}, or
   *     {@code null} if the {@link ProcedureDTO procedure} is not found
   */
  @Override
  public DocumentationOffice getDocumentationOfficeByUUID(UUID procedureId) {
    Optional<ProcedureDTO> procedureDTO = repository.findById(procedureId);
    return procedureDTO
        .map(dto -> DocumentationOfficeTransformer.transformToDomain(dto.getDocumentationOffice()))
        .orElse(null);
  }

  @Override
  public void delete(UUID procedureId) {
    repository.deleteById(procedureId);
  }
}

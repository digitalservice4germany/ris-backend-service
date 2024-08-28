package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeUserGroupDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationOfficeTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitListItemTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.ProcedureTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOfficeUserGroup;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseProcedureService implements ProcedureService {
  private final DatabaseProcedureRepository repository;
  private final DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  private final DatabaseDocumentationOfficeUserGroupRepository userGroupRepository;

  private final UserService userService;

  public DatabaseProcedureService(
      DatabaseProcedureRepository repository,
      DatabaseDocumentationOfficeRepository documentationOfficeRepository,
      DatabaseDocumentationOfficeUserGroupRepository userGroupRepository,
      UserService userService,
      KeycloakUserService keycloakUserService) {
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
      Optional<Boolean> withDocUnits) {

    DocumentationOfficeDTO documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(documentationOffice.abbreviation());

    if (withDocUnits.isPresent() && Boolean.TRUE.equals(withDocUnits.get())) {
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
                                    isInternalUser
                                        || isExternalUserAssigned(procedureDTO, oidcUser))
                                .build())
                    .toList())
        .orElse(null);
  }

  private Boolean isExternalUserAssigned(ProcedureDTO procedureDTO, OidcUser oidcUser) {
    Optional<DocumentationOfficeUserGroup> userGroup = userService.getUserGroup(oidcUser);
    if (userGroup.isPresent() && procedureDTO.getDocumentationOfficeUserGroupDTO() != null) {
      return userGroup.get().id().equals(procedureDTO.getDocumentationOfficeUserGroupDTO().getId());
    }
    return false;
  }

  @Override
  public String assignUserGroup(UUID procedureUUID, UUID userGroupId) {
    Optional<ProcedureDTO> procedureDTO = repository.findById(procedureUUID);
    Optional<DocumentationOfficeUserGroupDTO> userGroupDTO =
        userGroupRepository.findById(userGroupId);
    if (procedureDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be assigned as procedure is missing in the data base.");
    }
    if (userGroupDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be assigned as user group is missing in the data base.");
    }
    ProcedureDTO result = procedureDTO.get();
    result.setDocumentationOfficeUserGroupDTO(userGroupDTO.get());
    repository.save(result);
    return "Vorgang '"
        + procedureDTO.get().getLabel()
        + "' wurde Nutzergruppe '"
        + userGroupDTO.get().getUserGroupPathName()
        + "' zugewiesen.";
  }

  @Override
  public String unassignUserGroup(UUID procedureUUID) {
    Optional<ProcedureDTO> procedureDTO = repository.findById(procedureUUID);

    if (procedureDTO.isEmpty()) {
      throw new IllegalArgumentException(
          "User group couldn't be unassigned as procedure is missing in the data base.");
    }

    ProcedureDTO result = procedureDTO.get();
    result.setDocumentationOfficeUserGroupDTO(null);
    repository.save(result);
    return "Die Zuweisung aus Vorgang '" + procedureDTO.get().getLabel() + "' wurde entfernt.";
  }

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

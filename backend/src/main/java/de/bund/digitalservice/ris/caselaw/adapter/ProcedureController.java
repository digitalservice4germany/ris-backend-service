package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/caselaw/procedure")
public class ProcedureController {
  private final ProcedureService service;
  private final UserService userService;

  public ProcedureController(ProcedureService service, UserService userService) {
    this.service = service;
    this.userService = userService;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public Slice<Procedure> search(
      @AuthenticationPrincipal OidcUser oidcUser,
      @RequestParam(value = "q") Optional<String> query,
      @RequestParam(value = "pg") Optional<Integer> page,
      @RequestParam(value = "sz") Integer size,
      @RequestParam(value = "withDocUnits") Optional<Boolean> withDocUnits) {
    return service.search(
        query,
        userService.getDocumentationOffice(oidcUser),
        PageRequest.of(page.orElse(0), size),
        withDocUnits,
        oidcUser);
  }

  @GetMapping(value = "/{procedureUUID}/documentunits", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public List<DocumentationUnitListItem> getDocumentationUnits(
      @AuthenticationPrincipal OidcUser oidcUser, @NonNull @PathVariable UUID procedureUUID) {
    return service.getDocumentationUnits(procedureUUID, oidcUser);
  }

  /**
   * Assign a procedure to a user group.
   *
   * @param procedureUUID The UUID of the procedure.
   * @param userGroupId The UUID of the user group.
   * @return Info message of the successful assignment.
   */
  @PutMapping("/{procedureUUID}/assign/{userGroupId}")
  @PreAuthorize(
      "isAuthenticated() and @userIsInternal.apply(#oidcUser) and @userHasWriteAccessByProcedureId.apply(#procedureUUID)")
  public ResponseEntity<String> assignUserGroup(
      @AuthenticationPrincipal OidcUser oidcUser,
      @NonNull @PathVariable UUID procedureUUID,
      @NonNull @PathVariable UUID userGroupId) {
    String result = service.assignUserGroup(procedureUUID, userGroupId);
    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  /**
   * Unassign a procedure from a user group.
   *
   * @param procedureUUID The UUID of the procedure.
   * @return Info message of the successful removal of assignment.
   */
  @PutMapping("/{procedureUUID}/unassign")
  @PreAuthorize(
      "isAuthenticated() and @userIsInternal.apply(#oidcUser) and @userHasWriteAccessByProcedureId.apply(#procedureUUID)")
  public ResponseEntity<String> unassignUserGroup(
      @AuthenticationPrincipal OidcUser oidcUser, @NonNull @PathVariable UUID procedureUUID) {
    String result = service.unassignUserGroup(procedureUUID);
    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @DeleteMapping(value = "/{procedureUUID}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal OidcUser oidcUser, @NonNull @PathVariable UUID procedureUUID) {

    service.delete(procedureUUID);
    return ResponseEntity.ok().build();
  }
}

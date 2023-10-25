package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.User;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KeycloakUserService implements UserService {
  private final DatabaseDocumentationOfficeRepository documentationOfficeRepository;

  private static final Map<String, String> documentationCenterClaims =
      Map.ofEntries(
          Map.entry("/caselaw/BGH", "BGH"),
          Map.entry("/caselaw/BVerfG", "BVerfG"),
          Map.entry("/caselaw/BAG", "BAG"),
          Map.entry("/caselaw/BFH", "BFH"),
          Map.entry("/caselaw/BPatG", "BPatG"),
          Map.entry("/caselaw/BSG", "BSG"),
          Map.entry("/caselaw/BVerwG", "BVerwG"),
          Map.entry("/caselaw/OVG_NRW", "OVG NRW"),
          Map.entry("/caselaw/BZSt", "BZSt"),
          Map.entry("/DigitalService", "DigitalService"),
          Map.entry("/CC-RIS", "CC-RIS"));

  public KeycloakUserService(DatabaseDocumentationOfficeRepository documentationOfficeRepository) {
    this.documentationOfficeRepository = documentationOfficeRepository;
  }

  public Mono<User> getUser(OidcUser oidcUser) {
    return extractDocumentationOffice(oidcUser)
        .map(documentationOffice -> createUser(oidcUser, documentationOffice))
        .switchIfEmpty(Mono.defer(() -> Mono.just(createUser(oidcUser, null))));
  }

  public Mono<DocumentationOffice> getDocumentationOffice(OidcUser oidcUser) {
    return getUser(oidcUser).map(User::documentationOffice);
  }

  public String getEmail(OidcUser oidcUser) {
    return oidcUser.getEmail();
  }

  private User createUser(OidcUser oidcUser, DocumentationOffice documentationOffice) {
    return User.builder()
        .name(oidcUser.getAttribute("name"))
        .email(oidcUser.getEmail())
        .documentationOffice(documentationOffice)
        .build();
  }

  private Mono<DocumentationOffice> extractDocumentationOffice(OidcUser oidcUser) {
    List<String> groups = Objects.requireNonNull(oidcUser.getAttribute("groups"));
    String documentationOfficeKey =
        groups.stream()
            .filter(documentationCenterClaims::containsKey)
            .findFirst()
            .map(documentationCenterClaims::get)
            .orElse(null);

    return Optional.ofNullable(
            documentationOfficeRepository.findByAbbreviation(documentationOfficeKey))
        .map(
            documentationOfficeDTO ->
                DocumentationOffice.builder()
                    .abbreviation(documentationOfficeDTO.getAbbreviation())
                    .build())
        .map(Mono::just)
        .orElse(Mono.empty());
  }
}

package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverException;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverRepository;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Implementation of the {@link MailService} interface that sends juris-XML files via email. */
@Service
public class HandoverMailService implements MailService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final XmlExporter xmlExporter;

  private final HttpMailSender mailSender;

  private final HandoverRepository repository;

  private final Environment env;

  @Value("${mail.exporter.senderAddress:export.test@neuris}")
  private String senderAddress;

  @Value("${mail.exporter.jurisUsername:invalid-user}")
  private String jurisUsername;

  public HandoverMailService(
      XmlExporter xmlExporter,
      HttpMailSender mailSender,
      HandoverRepository repository,
      Environment env) {
    this.xmlExporter = xmlExporter;
    this.mailSender = mailSender;
    this.repository = repository;
    this.env = env;
  }

  /**
   * Hands over a documentation unit as XML to jDV via email.
   *
   * @param documentationUnit the documentation unit to hand over
   * @param receiverAddress the email address of the receiver
   * @param issuerAddress the email address of the issuer
   * @return the result of the handover
   * @throws HandoverException if the XML export fails
   */
  @Override
  public HandoverMail handOver(
      DocumentationUnit documentationUnit, String receiverAddress, String issuerAddress) {
    XmlTransformationResult xml;
    try {
      xml = xmlExporter.transformToXml(getTestDocumentationUnit(documentationUnit));
    } catch (ParserConfigurationException | TransformerException ex) {
      throw new HandoverException("Couldn't generate xml for documentationUnit.", ex);
    }

    String mailSubject = generateMailSubject(documentationUnit);

    HandoverMail handoverMail =
        generateXmlHandoverMail(
            documentationUnit.uuid(),
            receiverAddress,
            mailSubject,
            List.of(xml),
            issuerAddress,
            HandoverEntityType.DOCUMENTATION_UNIT);
    if (!handoverMail.success()) {
      return handoverMail;
    }
    generateAndSendMail(handoverMail);
    return repository.save(handoverMail);
  }

  /**
   * Hands over all references of an edition as XML to jDV via email.
   *
   * @param edition the edition to hand over
   * @param receiverAddress the email address of the receiver
   * @param issuerAddress the email address of the issuer
   * @return the result of the handover
   * @throws HandoverException if the XML export fails
   */
  @Override
  public HandoverMail handOver(
      LegalPeriodicalEdition edition, String receiverAddress, String issuerAddress) {
    List<XmlTransformationResult> xml;
    try {
      xml = xmlExporter.transformToXml(edition);
    } catch (ParserConfigurationException | TransformerException ex) {
      throw new HandoverException("Couldn't generate xml for edition.", ex);
    }

    String mailSubject = generateMailSubject(edition);

    HandoverMail handoverMail =
        generateXmlHandoverMail(
            edition.id(),
            receiverAddress,
            mailSubject,
            xml,
            issuerAddress,
            HandoverEntityType.EDITION);
    generateAndSendMail(handoverMail);
    if (!handoverMail.success()) {
      return handoverMail;
    }
    return repository.save(handoverMail);
  }

  /**
   * Returns the results of performed handover operations for an entity (documentation unit or
   * edition).
   *
   * @param entityId the UUID of the entity
   * @return a list of results of all handover operations for the entity
   */
  @Override
  public List<HandoverMail> getHandoverResult(UUID entityId, HandoverEntityType entityType) {
    return repository.getHandoversByEntity(entityId, entityType);
  }

  /**
   * Generates a preview of the XML that would be sent via email.
   *
   * @param documentationUnit the documentation unit
   * @return the XML export result, containing the XML and possibly errors
   * @throws HandoverException if the XML export fails
   */
  @Override
  public XmlTransformationResult getXmlPreview(DocumentationUnit documentationUnit) {
    try {
      return xmlExporter.transformToXml(documentationUnit);
    } catch (ParserConfigurationException | TransformerException ex) {
      throw new HandoverException("Couldn't generate xml for documentation unit.", ex);
    }
  }

  /**
   * Generates a preview of the XMLs that would be sent via email.
   *
   * @param edition the edition
   * @return the XML export results, containing the XMLs and possibly errors
   */
  @Override
  public List<XmlTransformationResult> getXmlPreview(LegalPeriodicalEdition edition) {
    try {
      return xmlExporter.transformToXml(edition);
    } catch (ParserConfigurationException | TransformerException ex) {
      throw new HandoverException("Couldn't generate xml for edtion.", ex);
    }
  }

  private String generateMailSubject(DocumentationUnit documentationUnit) {
    if (documentationUnit.documentNumber() == null) {
      throw new HandoverException("No document number has been set in the document unit.");
    }
    return generateMailSubject(documentationUnit.documentNumber(), "N");
  }

  private String generateMailSubject(LegalPeriodicalEdition edition) {
    if (edition.id() == null) {
      throw new HandoverException("No id has been set in the edition.");
    }
    return generateMailSubject("edition-" + edition.id(), "F");
  }

  private String generateMailSubject(String vg, String dt) {
    String deliveryDate =
        LocalDate.now(Clock.system(ZoneId.of("Europe/Berlin"))).format(DATE_FORMATTER);

    return "id=juris name=%s da=R df=X dt=%s mod=T ld=%s vg=%s"
        .formatted(jurisUsername, dt, deliveryDate, vg);
  }

  private void generateAndSendMail(HandoverMail handoverMail) throws HandoverException {
    if (handoverMail == null) {
      throw new HandoverException("No xml mail is set");
    }

    if (!handoverMail.success()) {
      return;
    }

    if (handoverMail.receiverAddress() == null) {
      throw new HandoverException("No receiver mail address is set");
    }

    for (MailAttachment attachment : handoverMail.attachments()) {
      if (attachment.fileContent() == null) {
        throw new HandoverException("No file content is set for attachment");
      }
    }

    mailSender.sendMail(
        senderAddress,
        handoverMail.receiverAddress(),
        handoverMail.mailSubject(),
        "neuris",
        handoverMail.attachments(),
        handoverMail.entityId().toString());
  }

  private HandoverMail generateXmlHandoverMail(
      UUID entityId,
      String receiverAddress,
      String mailSubject,
      List<XmlTransformationResult> xml,
      String issuerAddress,
      HandoverEntityType entityType) {
    var xmlHandoverMailBuilder =
        HandoverMail.builder()
            .entityId(entityId)
            .success(xml.stream().allMatch(XmlTransformationResult::success))
            .statusMessages(
                xml.stream()
                    .map(XmlTransformationResult::statusMessages)
                    .flatMap(List::stream)
                    .toList());

    if (!xmlHandoverMailBuilder.build().isSuccess() || xml.isEmpty()) {
      return xmlHandoverMailBuilder.success(false).build();
    }

    return xmlHandoverMailBuilder
        .receiverAddress(receiverAddress)
        .mailSubject(mailSubject)
        .handoverDate(xml.get(0).creationDate())
        .issuerAddress(issuerAddress)
        .attachments(renameAndCreateMailAttachments(xml))
        .entityType(entityType)
        .build();
  }

  public static List<MailAttachment> renameAndCreateMailAttachments(
      List<XmlTransformationResult> xmlFiles) {
    // Step 1: Group by fileName
    Map<String, List<XmlTransformationResult>> groupedByFileName =
        xmlFiles.stream().collect(Collectors.groupingBy(XmlTransformationResult::fileName));

    // Step 2: Prepare the list of MailAttachment with renamed duplicates
    List<MailAttachment> renamedAttachments = new ArrayList<>();

    // Step 3: Iterate over each group of xmlFiles and rename duplicates if necessary
    for (Map.Entry<String, List<XmlTransformationResult>> entry : groupedByFileName.entrySet()) {
      String fileName = entry.getKey();
      List<XmlTransformationResult> filesWithSameName = entry.getValue();

      if (filesWithSameName.size() > 1) {
        // If there are duplicates, rename them
        for (int i = 0; i < filesWithSameName.size(); i++) {
          String[] fileNameParts = fileName.split("\\.");
          String newFileName = fileNameParts[0] + "_" + (i + 1) + "." + fileNameParts[1];
          XmlTransformationResult xmlFile = filesWithSameName.get(i);
          renamedAttachments.add(new MailAttachment(newFileName, xmlFile.xml()));
        }
      } else {
        // No duplicates, keep original file name
        XmlTransformationResult xmlFile = filesWithSameName.get(0);
        renamedAttachments.add(new MailAttachment(xmlFile.fileName(), xmlFile.xml()));
      }
    }

    return renamedAttachments;
  }

  private DocumentationUnit getTestDocumentationUnit(DocumentationUnit documentationUnit) {
    if (env.matchesProfiles("production")) {
      return documentationUnit.toBuilder()
          .coreData(
              Optional.ofNullable(documentationUnit.coreData())
                  .orElseGet(() -> CoreData.builder().build()))
          .build();
    }
    return documentationUnit.toBuilder()
        .coreData(
            Optional.ofNullable(documentationUnit.coreData())
                .map(
                    coreData ->
                        coreData.toBuilder()
                            .court(
                                Court.builder()
                                    .type("VGH")
                                    .location("Mannheim")
                                    .label("VGH Mannheim")
                                    .build())
                            .fileNumbers(
                                Stream.concat(
                                        Stream.of("TEST"),
                                        documentationUnit.coreData().fileNumbers().stream())
                                    .toList())
                            .build())
                .orElseGet(
                    () ->
                        CoreData.builder()
                            .court(
                                Court.builder()
                                    .type("VGH")
                                    .location("Mannheim")
                                    .label("VGH Mannheim")
                                    .build())
                            .fileNumbers(Collections.singletonList("TEST"))
                            .build()))
        .build();
  }
}

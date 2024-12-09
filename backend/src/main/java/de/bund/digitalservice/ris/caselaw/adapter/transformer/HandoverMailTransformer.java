package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.HandoverMailAttachmentDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.HandoverMailDTO;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class HandoverMailTransformer {
  private HandoverMailTransformer() {}

  public static HandoverMailDTO transformToDTO(HandoverMail handoverMail) {
    var mail =
        HandoverMailDTO.builder()
            .entityId(handoverMail.entityId())
            .statusMessages(String.join("|", handoverMail.statusMessages()))
            .statusCode(handoverMail.success() ? "200" : "400")
            .receiverAddress(handoverMail.receiverAddress())
            .sentDate(handoverMail.getHandoverDate())
            .mailSubject(handoverMail.mailSubject())
            .issuerAddress(handoverMail.issuerAddress())
            .attachments(new ArrayList<>())
            .build();

    for (MailAttachment attachment : handoverMail.attachments()) {
      mail.addAttachment(
          HandoverMailAttachmentDTO.builder()
              .xml(attachment.fileContent())
              .fileName(attachment.fileName())
              .build());
    }
    return mail;
  }

  public static HandoverMail transformToDomain(
      HandoverMailDTO handoverMailDTO, UUID entityId, HandoverEntityType entityType) {
    return HandoverMail.builder()
        .entityId(entityId)
        .entityType(entityType)
        .statusMessages(Arrays.stream(handoverMailDTO.getStatusMessages().split("\\|")).toList())
        .success(handoverMailDTO.getStatusCode().equals("200"))
        .receiverAddress(handoverMailDTO.getReceiverAddress())
        .handoverDate(handoverMailDTO.getSentDate())
        .mailSubject(handoverMailDTO.getMailSubject())
        .issuerAddress(handoverMailDTO.getIssuerAddress())
        .attachments(
            handoverMailDTO.getAttachments().stream()
                .map(
                    attachmentDTO ->
                        MailAttachment.builder()
                            .fileContent(attachmentDTO.getXml())
                            .fileName(attachmentDTO.getFileName())
                            .build())
                .toList())
        .build();
  }
}

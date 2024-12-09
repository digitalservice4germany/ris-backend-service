package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity for performed jDV handover operations. */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "handover_mail")
@Entity
public class HandoverMailDTO {

  @Id @GeneratedValue UUID id;

  @Column(name = "entity_id")
  UUID entityId;

  @Column(name = "receiver_address")
  String receiverAddress;

  @Column(name = "mail_subject")
  String mailSubject;

  @Column(name = "status_code")
  String statusCode;

  @Column(name = "status_messages")
  String statusMessages;

  @Column(name = "sent_date")
  Instant sentDate;

  @Column(name = "issuer_address")
  private String issuerAddress;

  // One-to-many relationship for attachments
  @Builder.Default
  @OneToMany(mappedBy = "handoverMail", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HandoverMailAttachmentDTO> attachments = new ArrayList<>();

  // Helper method to add an attachment and set the bidirectional relationship
  public void addAttachment(HandoverMailAttachmentDTO attachment) {
    attachments.add(attachment);
    attachment.setHandoverMail(this); // Ensure the back-reference is set
  }

  // Helper method to remove an attachment
  public void removeAttachment(HandoverMailAttachmentDTO attachment) {
    attachments.remove(attachment);
    attachment.setHandoverMail(null); // Nullify the back-reference when removing
  }
}

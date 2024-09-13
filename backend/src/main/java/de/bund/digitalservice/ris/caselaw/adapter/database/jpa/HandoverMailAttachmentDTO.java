package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity for attachments to the handover mail operations. */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "handover_mail_attachments")
@Entity
public class HandoverMailAttachmentDTO {

  @Id @GeneratedValue UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "handover_mail_id", nullable = false)
  private HandoverMailDTO handoverMail;

  String xml;

  @Column(name = "file_name")
  String fileName;
}

package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class RisMeta {
  @XmlElementWrapper(name = "decisionNames", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "decisionName", namespace = CaseLawLdml.RIS_NS)
  private List<String> decisionName;

  @XmlElementWrapper(name = "previousDecisions", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "previousDecision", namespace = CaseLawLdml.RIS_NS)
  private List<RelatedDecision> previousDecision;

  @XmlElementWrapper(name = "ensuingDecisions", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "ensuingDecision", namespace = CaseLawLdml.RIS_NS)
  private List<RelatedDecision> ensuingDecision;

  @XmlElementWrapper(name = "fileNumbers", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "fileNumber", namespace = CaseLawLdml.RIS_NS)
  private List<String> fileNumbers;

  @XmlElement(name = "documentType", namespace = CaseLawLdml.RIS_NS)
  private String documentType;

  @XmlElement(name = "courtLocation", namespace = CaseLawLdml.RIS_NS)
  private String courtLocation;

  @XmlElement(name = "courtType", namespace = CaseLawLdml.RIS_NS)
  private String courtType;

  @XmlElementWrapper(name = "legalForces", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "legalForce", namespace = CaseLawLdml.RIS_NS)
  private List<String> legalForce;

  @XmlElement(name = "legalEffect", namespace = CaseLawLdml.RIS_NS)
  private String legalEffect;

  @XmlElementWrapper(name = "fieldOfLaws", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "fieldOfLaw", namespace = CaseLawLdml.RIS_NS)
  private List<String> fieldOfLaw;

  @XmlElement(name = "yearOfDispute", namespace = CaseLawLdml.RIS_NS)
  private String yearOfDispute;

  @XmlElement(name = "judicialBody", namespace = CaseLawLdml.RIS_NS)
  private String judicialBody;

  @XmlElementWrapper(name = "deviatingCourts", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "deviatingCourt", namespace = CaseLawLdml.RIS_NS)
  private List<String> deviatingCourt;

  @XmlElementWrapper(name = "deviatingDates", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "deviatingDate", namespace = CaseLawLdml.RIS_NS)
  private List<String> deviatingDate;

  @XmlElementWrapper(name = "deviatingDocumentNumbers", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "deviatingDocumentNumber", namespace = CaseLawLdml.RIS_NS)
  private List<String> deviatingDocumentNumber;

  @XmlElementWrapper(name = "deviatingEclis", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "deviatingEcli", namespace = CaseLawLdml.RIS_NS)
  private List<String> deviatingEcli;

  @XmlElementWrapper(name = "deviatingFileNumbers", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "deviatingFileNumber", namespace = CaseLawLdml.RIS_NS)
  private List<String> deviatingFileNumber;

  @XmlElement(name = "publicationStatus", namespace = CaseLawLdml.RIS_NS)
  private String publicationStatus;

  @XmlElement(name = "error", namespace = CaseLawLdml.RIS_NS)
  private Boolean error;

  @XmlElement(name = "documentationOffice", namespace = CaseLawLdml.RIS_NS)
  private String documentationOffice;

  @XmlElementWrapper(name = "procedures", namespace = CaseLawLdml.RIS_NS)
  @XmlElement(name = "procedure", namespace = CaseLawLdml.RIS_NS)
  private List<String> procedure;
}

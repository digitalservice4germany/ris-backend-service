import { RangeUnit } from "../../../../src/domain/Norm"
import { NormData } from "../fixtures"

export const newNorm: NormData = {
  jurisZipFileName: "",
  articles: [
    {
      marker: "§ 1",
      title: "Allgemeiner Anwendungsbereich",
      paragraphs: [
        {
          marker: "(1)",
          text: "Dieses Gesetz regelt Mindestziele und deren Sicherstellung bei der Beschaffung bestimmter Straßenfahrzeuge und Dienstleistungen, für die diese Straßenfahrzeuge eingesetzt werden, durch öffentliche Auftraggeber und Sektorenauftraggeber.",
        },
        {
          marker: "(2)",
          text: "Soweit in diesem Gesetz oder aufgrund dieses Gesetzes nichts anderes geregelt ist, sind die allgemeinen vergaberechtlichen Vorschriften anzuwenden.",
        },
      ],
    },
  ],
  metadataSections: {
    NORM: [
      {
        KEYWORD: ["Neues Schlagwort 1 ", "Neues Schlagwort 2"],
        DIVERGENT_DOCUMENT_NUMBER: [
          "Neue abweichende Dokumentennummer 1",
          "Neue abweichende Dokumentennummer 2",
        ],
        RIS_ABBREVIATION_INTERNATIONAL_LAW: [
          "Neue Juris-Abkürzung für völkerrechtliche Vereinbarungen 1",
          "Neue Juris-Abkürzung für völkerrechtliche Vereinbarungen 2",
        ],
        UNOFFICIAL_ABBREVIATION: [
          "Neue Nichtamtliche Buchstabenabkürzung 1",
          "Neue Nichtamtliche Buchstabenabkürzung 2",
        ],
        UNOFFICIAL_SHORT_TITLE: [
          "Neue Nichtamtliche Kurzüberschrift 1",
          "Neue Nichtamtliche Kurzüberschrift 2",
        ],
        UNOFFICIAL_LONG_TITLE: [
          "Neue Nichtamtliche Langüberschrift1",
          "Neue Nichtamtliche Langüberschrift2",
        ],
        UNOFFICIAL_REFERENCE: [
          "Neue nichtamtliche Fundstelle 1",
          "Neue nichtamtliche Fundstelle 2",
        ],
        REFERENCE_NUMBER: ["Neues Aktenzeichen 1", "Neues Aktenzeichen 2"],
        DEFINITION: ["Neue Definition 1", "Neue Definition 2"],
        AGE_OF_MAJORITY_INDICATION: [
          "Neue Volljährigkeit 1",
          "Neue Volljährigkeit 2",
        ],
        VALIDITY_RULE: [
          "Neue Gültigkeitsregelung 1",
          "Neue Gültigkeitsregelung 2",
        ],
      },
    ],
    SUBJECT_AREA: [
      { SUBJECT_FNA: ["Neue FNA 1"], SUBJECT_GESTA: ["Neue GESTA 1"] },
      { SUBJECT_FNA: ["Neue FNA 2"], SUBJECT_GESTA: ["Neue GESTA 2"] },
    ],
    LEAD: [
      {
        LEAD_JURISDICTION: ["Neues Ressort 1"],
        LEAD_UNIT: ["Neue Organisationseinheit 1"],
      },
      {
        LEAD_JURISDICTION: ["Neues Ressort 2"],
        LEAD_UNIT: ["Neue Organisationseinheit 2"],
      },
    ],
    PARTICIPATION: [
      {
        PARTICIPATION_TYPE: ["Neue Art der Mitwirkung 1"],
        PARTICIPATION_INSTITUTION: ["Neues mitwirkendes Organ 1"],
      },
      {
        PARTICIPATION_TYPE: ["Neue Art der Mitwirkung 2"],
        PARTICIPATION_INSTITUTION: ["Neues mitwirkendes Organ 2"],
      },
    ],
    CITATION_DATE: [{ DATE: ["2023-01-02"] }, { DATE: ["2023-03-02"] }],
    AGE_INDICATION: [
      {
        RANGE_START: ["1"],
        RANGE_START_UNIT: [RangeUnit.DAYS],
        RANGE_END: ["2"],
        RANGE_END_UNIT: [RangeUnit.WEEKS],
      },
      {
        RANGE_START: ["3"],
        RANGE_START_UNIT: [RangeUnit.MONTHS],
        RANGE_END: ["4"],
        RANGE_END_UNIT: [RangeUnit.YEARS],
      },
    ],
    NORM_PROVIDER: [
      {
        ENTITY: ["providerEntity"],
        DECIDING_BODY: ["providerDecidingBody"],
        RESOLUTION_MAJORITY: [false],
      },
    ],
  },
  officialLongTitle:
    "Verordnung zur Anpassung von Rechtsverordnungen an das Tierarzneimittelrecht",
  officialShortTitle: "officialShortTitle",
  officialAbbreviation: "officialAbbreviation",
  announcementDate: "2022-11-01",
  risAbbreviation: "risAbbreviation",
  documentTemplateName: "documentTemplateName",
  publicationDate: "2022-11-01",
  isExpirationDateTemp: false,
  categorizedReference: "categorizedReference",
  celexNumber: "celexNumber",
  completeCitation: "completeCitation",
  digitalAnnouncementDate: "2022-11-01",
  digitalAnnouncementArea: "digitalAnnouncementArea",
  digitalAnnouncementAreaNumber: "digitalAnnouncementAreaNumber",
  digitalAnnouncementEdition: "digitalAnnouncementEdition",
  digitalAnnouncementExplanations: "digitalAnnouncementExplanations",
  digitalAnnouncementInfo: "digitalAnnouncementInfo",
  digitalAnnouncementMedium: "digitalAnnouncementMedium",
  digitalAnnouncementYear: "digitalAnnouncementYear",
  digitalEvidenceAppendix: "digitalEvidenceAppendix",
  digitalEvidenceExternalDataNote: "digitalEvidenceExternalDataNote",
  digitalEvidenceLink: "digitalEvidenceLink",
  digitalEvidenceRelatedData: "digitalEvidenceRelatedData",
  divergentEntryIntoForceDate: "2022-11-01",
  divergentEntryIntoForceDateState: "unbestimmt (unbekannt)",
  divergentExpirationDate: "2022-11-01",
  divergentExpirationDateState: "nicht vorhanden",
  documentCategory: "documentCategory",
  documentNormCategory: "documentNormCategory",
  documentNumber: "documentNumber",
  documentStatusDate: "2022-11-01",
  documentStatusDescription: "documentStatusDescription",
  documentStatusEntryIntoForceDate: "2022-11-01",
  documentStatusProof: "documentStatusProof",
  documentStatusReference: "documentStatusReference",
  documentStatusWorkNote: "documentStatusWorkNote",
  documentTextProof: "documentTextProof",
  documentTypeName: "documentTypeName",
  entryIntoForceDate: "2022-11-01",
  entryIntoForceDateState: "unbestimmt (zukünftig)",
  euAnnouncementExplanations: "euAnnouncementExplanations",
  euAnnouncementGazette: "euAnnouncementGazette",
  euAnnouncementInfo: "euAnnouncementInfo",
  euAnnouncementNumber: "euAnnouncementNumber",
  euAnnouncementPage: "euAnnouncementPage",
  euAnnouncementSeries: "euAnnouncementSeries",
  euAnnouncementYear: "euAnnouncementYear",
  eli: "europeanLegalIdentifier",
  expirationDate: "2022-11-01",
  expirationDateState: "unbestimmt (unbekannt)",
  expirationNormCategory: "expirationNormCategory",
  otherDocumentNote: "otherDocumentNote",
  otherFootnote: "otherFootnote",
  footnoteChange: "footnoteChange",
  footnoteComment: "footnoteComment",
  footnoteDecision: "footnoteDecision",
  footnoteStateLaw: "footnoteStateLaw",
  footnoteEuLaw: "footnoteEuLaw",
  otherOfficialAnnouncement: "otherOfficialAnnouncement",
  otherStatusNote: "otherStatusNote",
  principleEntryIntoForceDate: "2022-11-01",
  principleEntryIntoForceDateState: "nicht vorhanden",
  principleExpirationDate: "2022-11-01",
  principleExpirationDateState: "unbestimmt (zukünftig)",
  printAnnouncementExplanations: "printAnnouncementExplanations",
  printAnnouncementGazette: "printAnnouncementGazette",
  printAnnouncementInfo: "printAnnouncementInfo",
  printAnnouncementNumber: "printAnnouncementNumber",
  printAnnouncementPage: "printAnnouncementPage",
  printAnnouncementYear: "printAnnouncementYear",
  reissueArticle: "reissueArticle",
  reissueDate: "2022-11-01",
  reissueNote: "reissueNote",
  reissueReference: "reissueReference",
  repealArticle: "repealArticle",
  repealDate: "2022-11-01",
  repealNote: "repealNote",
  repealReferences: "repealReferences",
  statusDate: "2022-11-01",
  statusDescription: "statusDescription",
  statusNote: "statusNote",
  statusReference: "statusReference",
  text: "text",
}

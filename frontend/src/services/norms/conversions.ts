import dayjs from "dayjs"
import timezone from "dayjs/plugin/timezone"
import utc from "dayjs/plugin/utc"
import {
  MetadatumSchema,
  MetadataSectionSchema,
  NormResponseSchema,
  FlatMetadataRequestSchema,
} from "./schemas"
import {
  groupBy,
  mapValues,
  compareOrder,
  mergeValues,
  filterEntries,
} from "./utilities"

import {
  MetadatumType,
  MetadataSectionName,
  Metadata,
  MetadataSections,
  MetadataValueType,
  Norm,
  FlatMetadata,
} from "@/domain/Norm"

function encodeString(data?: string | null): string | null {
  return data && data.length > 0 ? data : null
}

function encodeBoolean(data?: boolean | null): boolean | null {
  return data ?? null
}

// Makes the assumption that we currently get a date string in the following
// format: `2022-11-14T23:00:00.000Z`. To comply with the expected date format
// of the API, we only take the first 10 characters.
//
// TODO: Improve by working with enriched date type.
function encodeDate(data?: string | null): string | null {
  dayjs.extend(utc)
  dayjs.extend(timezone)

  return data && data.length > 0
    ? dayjs(data).tz("Europe/Berlin").format("YYYY-MM-DD")
    : null
}

type MetadataValueDecoders = {
  [Property in keyof MetadataValueType]: (
    data: MetadatumSchema["value"]
  ) => MetadataValueType[Property]
}

type MetadataValueEncoders = {
  [Property in keyof MetadataValueType]: (
    data: MetadataValueType[Property]
  ) => MetadatumSchema["value"]
}

const DECODERS: MetadataValueDecoders = {
  [MetadatumType.KEYWORD]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_LONG_TITLE]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_SHORT_TITLE]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_ABBREVIATION]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_REFERENCE]: (data: string) => data,
  [MetadatumType.DIVERGENT_DOCUMENT_NUMBER]: (data: string) => data,
  [MetadatumType.REFERENCE_NUMBER]: (data: string) => data,
  [MetadatumType.DEFINITION]: (data: string) => data,
  [MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW]: (data: string) => data,
  [MetadatumType.AGE_OF_MAJORITY_INDICATION]: (data: string) => data,
  [MetadatumType.VALIDITY_RULE]: (data: string) => data,
  [MetadatumType.LEAD_JURISDICTION]: (data: string) => data,
  [MetadatumType.LEAD_UNIT]: (data: string) => data,
  [MetadatumType.PARTICIPATION_TYPE]: (data: string) => data,
  [MetadatumType.PARTICIPATION_INSTITUTION]: (data: string) => data,
  [MetadatumType.SUBJECT_FNA]: (data: string) => data,
  [MetadatumType.SUBJECT_PREVIOUS_FNA]: (data: string) => data,
  [MetadatumType.SUBJECT_GESTA]: (data: string) => data,
  [MetadatumType.SUBJECT_BGB_3]: (data: string) => data,
}

const ENCORDERS: MetadataValueEncoders = {
  [MetadatumType.KEYWORD]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_LONG_TITLE]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_SHORT_TITLE]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_ABBREVIATION]: (data: string) => data,
  [MetadatumType.UNOFFICIAL_REFERENCE]: (data: string) => data,
  [MetadatumType.DIVERGENT_DOCUMENT_NUMBER]: (data: string) => data,
  [MetadatumType.REFERENCE_NUMBER]: (data: string) => data,
  [MetadatumType.DEFINITION]: (data: string) => data,
  [MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW]: (data: string) => data,
  [MetadatumType.AGE_OF_MAJORITY_INDICATION]: (data: string) => data,
  [MetadatumType.VALIDITY_RULE]: (data: string) => data,
  [MetadatumType.LEAD_JURISDICTION]: (data: string) => data,
  [MetadatumType.LEAD_UNIT]: (data: string) => data,
  [MetadatumType.PARTICIPATION_TYPE]: (data: string) => data,
  [MetadatumType.PARTICIPATION_INSTITUTION]: (data: string) => data,
  [MetadatumType.SUBJECT_FNA]: (data: string) => data,
  [MetadatumType.SUBJECT_PREVIOUS_FNA]: (data: string) => data,
  [MetadatumType.SUBJECT_GESTA]: (data: string) => data,
  [MetadatumType.SUBJECT_BGB_3]: (data: string) => data,
}

/**
 * Decodes a list of metadata from the API response schema to the domain format. It
 * does that by grouping and mapping them by their type, sort them by order in
 * each group and finally map them to their decoded value property based on the
 * respective datum type.
 *
 * @example
 * ```ts
 * decodeMetadata([
 *  { type: COMMENT, value: "text", order: 2 }
 *  { type: DATE, value: "20-03-2001", order: 1 }
 *  { type: COMMENT, value: "other text", order: 1 }
 * ])
 * // => { COMMENT: ["other text", "text"], DATE: ["20-03-2001"] }
 * ```
 *
 * @param metadata to decode from response schema
 * @returns grouped, mapped, sorted metadata values
 */
function decodeMetadata(metadata: MetadatumSchema[]): Metadata {
  const grouped = groupBy(metadata, (datum) => datum.type)
  return mapValues(grouped, (data) =>
    data.sort(compareOrder).map((datum) => DECODERS[datum.type](datum.value))
  )
}

/**
 * Encodes some metadata from the domain format to the API request schema. The
 * mapping keys are used as type, also used for the encoding of the values. The
 * index within a group is used as order property. Finally all groups get merged
 * back together.
 *
 * @example
 * ```ts
 * encodeMetadata({ COMMENT: ["other text", "text"], DATE: ["20-03-2001"] })
 * // =>
 * // [
 * //   { type: COMMENT, value: "other text", order: 1 },
 * //   { type: COMMENT, value: "text", order: 2 },
 * //   { type: DATE, value: "20-03-2001, order: 1 },
 * // ]
 * ```
 *
 * @param metadata to encode from response schema
 * @returns un-grouped metadata object collection
 */
function encodeMetadata(metadata: Metadata): MetadatumSchema[] {
  const encodedMapping = mapValues(metadata, (group, type) =>
    group?.map((value, index) => ({
      type,
      order: index + 1,
      // FIXME: Type system is tricky here...
      value: ENCORDERS[type](value as never),
    }))
  )

  return mergeValues(encodedMapping)
}

/**
 * Decodes a list of metadata sections from the API response schema to the
 * domain format. It does that by grouping and mapping them by their name,
 * sort them by order in each group and finally map them to their recursively
 * decoded sections and metadata.
 *
 * @example
 * ```ts
 * decodeMetadataSections([
 *  {
 *    name: RELEASE,
 *     order: 2,
 *     metadata: [
 *       { type: DATE, value: "20-03-2001", order: 1 },
 *       { type: COMMENT, value: "text", order: 1 },
 *     ],
 *     sections: [{
 *       name: INSTITUTION,
 *       order: 1,
 *       metadata: [
 *         { type: NAME, value: "House", order: 1 }
 *       ],
 *     }],
 *  },
 *  {
 *    name: RELEASE:
 *    order: 1,
 *  },
 *  {
 *    name: PROOF,
 *    order: 1,
 *    metadata: [
 *     { type: DOCUMENT, value: "example.pdf", order: 1 }
 *     { type: DOCUMENT, value: "other.txt", order: 2 }
 *    ]
 *  }
 * ])
 * // =>
 * // {
 * //   RELEASE: [
 * //     { _ },
 * //     { DATE: ["20-03-2001"], COMMENT: ["text"], INSTITUTION: [{ NAME: ["House"] }]},
 * //   ],
 * //   PROOF: [
 * //     { DOCUMENT: ["example.pdf", "other.txt"] },
 * //   ],
 * // }
 * ```
 *
 * @param sections to decode from response schema
 * @returns grouped, mapped, sorted metadata sections
 */
export function decodeMetadataSections(
  sections: MetadataSectionSchema[]
): MetadataSections {
  const grouped = groupBy(sections, (section) => section.name)
  return mapValues(grouped, (sections) =>
    sections.sort(compareOrder).map((section) => ({
      ...decodeMetadata(section.metadata ?? []),
      ...decodeMetadataSections(section.sections ?? []),
    }))
  )
}

/**
 * Encodes some metadata sections from the domain format to the API request
 * schema. The mapping keys are used as name. The keys within the sections are
 * then separated by datum and section types and names to recursively encode the
 * section. The index within a group is used as order property. Finally all
 * groups are merged back together.
 *
 * @example
 * ```ts
 * encodeMetadataSection({
 *   RELEASE: [
 *     { _ },
 *     { DATE: ["20-03-2001"], COMMENT: ["text"], INSTITUTION: [{ NAME: ["House"] }]},
 *   ],
 *   PROOF: [
 *     { DOCUMENT: ["example.pdf", "other.txt"] },
 *    ],
 * })
 * // [
 * //   {
 * //     name: RELEASE:
 * //     order: 1,
 * //   },
 * //   {
 * //     name: RELEASE,
 * //      order: 2,
 * //      metadata: [
 * //        { type: DATE, value: "20-03-2001", order: 1 },
 * //        { type: COMMENT, value: "text", order: 1 },
 * //      ],
 * //      sections: [{
 * //        name: INSTITUTION,
 * //        order: 1,
 * //        metadata: [
 * //          { type: NAME, value: "House", order: 1 }
 * //        ],
 * //      }],
 * //   },
 * //   {
 * //     name: PROOF,
 * //     order: 1,
 * //     metadata: [
 * //      { type: DOCUMENT, value: "example.pdf", order: 1 }
 * //      { type: DOCUMENT, value: "other.txt", order: 2 }
 * //     ]
 * //   }
 * // ]
 * ```
 *
 * @param sections to encode as request schema
 * @returns un-grouped metadata section object collection
 */
export function encodeMetadataSections(
  sections: MetadataSections
): MetadataSectionSchema[] | null {
  if (Object.entries(sections).length == 0) {
    return null
  }

  const encodedMapping = mapValues(sections, (group, name) =>
    group?.map((value, index) => {
      const metadata = filterEntries(
        value,
        (data, key) =>
          Object.keys(MetadatumType).includes(key) && data !== undefined
      ) as Metadata

      const childSections = filterEntries(
        value,
        (data, key) =>
          Object.keys(MetadataSectionName).includes(key) && data !== undefined
      ) as MetadataSections

      return {
        name,
        order: index + 1,
        metadata: encodeMetadata(metadata),
        sections: encodeMetadataSections(childSections),
      }
    })
  )

  return mergeValues(encodedMapping)
}

export function decodeNorm(norm: NormResponseSchema): Norm {
  const { metadataSections, ...stableData } = norm
  return {
    metadataSections: decodeMetadataSections(metadataSections ?? []),
    ...stableData,
  }
}

export function encodeFlatMetadata(
  flatMetadata: FlatMetadata
): FlatMetadataRequestSchema {
  return {
    documentTemplateName: encodeString(flatMetadata.documentTemplateName),
    ageIndicationEnd: encodeString(flatMetadata.ageIndicationEnd),
    ageIndicationStart: encodeString(flatMetadata.ageIndicationStart),
    announcementDate: encodeDate(flatMetadata.announcementDate),
    applicationScopeArea: encodeString(flatMetadata.applicationScopeArea),
    applicationScopeEndDate: encodeDate(flatMetadata.applicationScopeEndDate),
    applicationScopeStartDate: encodeDate(
      flatMetadata.applicationScopeStartDate
    ),
    categorizedReference: encodeString(flatMetadata.categorizedReference),
    celexNumber: encodeString(flatMetadata.celexNumber),
    citationDate: encodeDate(flatMetadata.citationDate),
    citationYear: encodeString(flatMetadata.citationYear),
    completeCitation: encodeString(flatMetadata.completeCitation),
    digitalAnnouncementDate: encodeDate(flatMetadata.digitalAnnouncementDate),
    digitalAnnouncementArea: encodeString(flatMetadata.digitalAnnouncementArea),
    digitalAnnouncementAreaNumber: encodeString(
      flatMetadata.digitalAnnouncementAreaNumber
    ),
    digitalAnnouncementEdition: encodeString(
      flatMetadata.digitalAnnouncementEdition
    ),
    digitalAnnouncementExplanations: encodeString(
      flatMetadata.digitalAnnouncementExplanations
    ),
    digitalAnnouncementInfo: encodeString(flatMetadata.digitalAnnouncementInfo),
    digitalAnnouncementMedium: encodeString(
      flatMetadata.digitalAnnouncementMedium
    ),
    digitalAnnouncementPage: encodeString(flatMetadata.digitalAnnouncementPage),
    digitalAnnouncementYear: encodeString(flatMetadata.digitalAnnouncementYear),
    digitalEvidenceAppendix: encodeString(flatMetadata.digitalEvidenceAppendix),
    digitalEvidenceExternalDataNote: encodeString(
      flatMetadata.digitalEvidenceExternalDataNote
    ),
    digitalEvidenceLink: encodeString(flatMetadata.digitalEvidenceLink),
    digitalEvidenceRelatedData: encodeString(
      flatMetadata.digitalEvidenceRelatedData
    ),
    divergentEntryIntoForceDate: encodeDate(
      flatMetadata.divergentEntryIntoForceDate
    ),
    divergentEntryIntoForceDateState: encodeString(
      flatMetadata.divergentEntryIntoForceDateState
    ),
    divergentExpirationDate: encodeDate(flatMetadata.divergentExpirationDate),
    divergentExpirationDateState: encodeString(
      flatMetadata.divergentExpirationDateState
    ),
    documentCategory: encodeString(flatMetadata.documentCategory),
    documentNormCategory: encodeString(flatMetadata.documentNormCategory),
    documentNumber: encodeString(flatMetadata.documentNumber),
    documentStatusDate: encodeDate(flatMetadata.documentStatusDate),
    documentStatusDescription: encodeString(
      flatMetadata.documentStatusDescription
    ),
    documentStatusEntryIntoForceDate: encodeDate(
      flatMetadata.documentStatusEntryIntoForceDate
    ),
    documentStatusProof: encodeString(flatMetadata.documentStatusProof),
    documentStatusReference: encodeString(flatMetadata.documentStatusReference),
    documentStatusWorkNote: encodeString(flatMetadata.documentStatusWorkNote),
    documentTextProof: encodeString(flatMetadata.documentTextProof),
    documentTypeName: encodeString(flatMetadata.documentTypeName),
    entryIntoForceDate: encodeDate(flatMetadata.entryIntoForceDate),
    entryIntoForceDateState: encodeString(flatMetadata.entryIntoForceDateState),
    entryIntoForceNormCategory: encodeString(
      flatMetadata.entryIntoForceNormCategory
    ),
    euAnnouncementExplanations: encodeString(
      flatMetadata.euAnnouncementExplanations
    ),
    euAnnouncementGazette: encodeString(flatMetadata.euAnnouncementGazette),
    euAnnouncementInfo: encodeString(flatMetadata.euAnnouncementInfo),
    euAnnouncementNumber: encodeString(flatMetadata.euAnnouncementNumber),
    euAnnouncementPage: encodeString(flatMetadata.euAnnouncementPage),
    euAnnouncementSeries: encodeString(flatMetadata.euAnnouncementSeries),
    euAnnouncementYear: encodeString(flatMetadata.euAnnouncementYear),
    eli: encodeString(flatMetadata.eli),
    expirationDate: encodeDate(flatMetadata.expirationDate),
    expirationDateState: encodeString(flatMetadata.expirationDateState),
    expirationNormCategory: encodeString(flatMetadata.expirationNormCategory),
    isExpirationDateTemp: encodeBoolean(flatMetadata.isExpirationDateTemp),
    officialAbbreviation: encodeString(flatMetadata.officialAbbreviation),
    officialLongTitle: encodeString(flatMetadata.officialLongTitle) ?? "",
    officialShortTitle: encodeString(flatMetadata.officialShortTitle),
    otherDocumentNote: encodeString(flatMetadata.otherDocumentNote),
    otherFootnote: encodeString(flatMetadata.otherFootnote),
    footnoteChange: encodeString(flatMetadata.footnoteChange),
    footnoteComment: encodeString(flatMetadata.footnoteComment),
    footnoteDecision: encodeString(flatMetadata.footnoteDecision),
    footnoteStateLaw: encodeString(flatMetadata.footnoteStateLaw),
    footnoteEuLaw: encodeString(flatMetadata.footnoteEuLaw),
    otherOfficialAnnouncement: encodeString(
      flatMetadata.otherOfficialAnnouncement
    ),
    otherStatusNote: encodeString(flatMetadata.otherStatusNote),
    principleEntryIntoForceDate: encodeDate(
      flatMetadata.principleEntryIntoForceDate
    ),
    principleEntryIntoForceDateState: encodeString(
      flatMetadata.principleEntryIntoForceDateState
    ),
    principleExpirationDate: encodeDate(flatMetadata.principleExpirationDate),
    principleExpirationDateState: encodeString(
      flatMetadata.principleExpirationDateState
    ),
    printAnnouncementExplanations: encodeString(
      flatMetadata.printAnnouncementExplanations
    ),
    printAnnouncementGazette: encodeString(
      flatMetadata.printAnnouncementGazette
    ),
    printAnnouncementInfo: encodeString(flatMetadata.printAnnouncementInfo),
    printAnnouncementNumber: encodeString(flatMetadata.printAnnouncementNumber),
    printAnnouncementPage: encodeString(flatMetadata.printAnnouncementPage),
    printAnnouncementYear: encodeString(flatMetadata.printAnnouncementYear),
    providerEntity: encodeString(flatMetadata.providerEntity),
    providerDecidingBody: encodeString(flatMetadata.providerDecidingBody),
    providerIsResolutionMajority: encodeBoolean(
      flatMetadata.providerIsResolutionMajority
    ),
    publicationDate: encodeDate(flatMetadata.publicationDate),
    reissueArticle: encodeString(flatMetadata.reissueArticle),
    reissueDate: encodeDate(flatMetadata.reissueDate),
    reissueNote: encodeString(flatMetadata.reissueNote),
    reissueReference: encodeString(flatMetadata.reissueReference),
    repealArticle: encodeString(flatMetadata.repealArticle),
    repealDate: encodeDate(flatMetadata.repealDate),
    repealNote: encodeString(flatMetadata.repealNote),
    repealReferences: encodeString(flatMetadata.repealReferences),
    risAbbreviation: encodeString(flatMetadata.risAbbreviation),
    statusDate: encodeDate(flatMetadata.statusDate),
    statusDescription: encodeString(flatMetadata.statusDescription),
    statusNote: encodeString(flatMetadata.statusNote),
    statusReference: encodeString(flatMetadata.statusReference),
    text: encodeString(flatMetadata.text),
  }
}

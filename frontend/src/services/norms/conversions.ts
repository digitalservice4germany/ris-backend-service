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
  NormCategory,
  UndefinedDate,
} from "@/domain/Norm"

function identity<T>(data: T): T {
  return data
}

function encodeString(data?: string | null): string | null {
  return data && data.length > 0 ? data : null
}

// Makes the assumption that we currently get a date string in the following
// format: `2022-11-14T23:00:00.000Z`. To comply with the expected date format
// of the API, we only take the first 10 characters.
//
// TODO: Improve by working with enriched date type.
function encodeDate(data?: string): string {
  dayjs.extend(utc)
  dayjs.extend(timezone)

  return data && data.length > 0
    ? dayjs.utc(data).tz(dayjs.tz.guess()).format("YYYY-MM-DD")
    : ""
}

function encodeNullDate(data?: string | null): string | null {
  return data ? encodeDate(data) : null
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

function decodeBoolean(data: string): boolean {
  return JSON.parse(data)
}

function encodeBoolean(data: boolean): string {
  return String(data)
}

function decodeNormCategory(data: string): NormCategory {
  const decodedCategory = Object.values(NormCategory).find(
    (category) => category == data
  )
  if (decodedCategory) return decodedCategory
  else throw new Error(`Unknown Norm Category: "${data}"`)
}

function decodeUndefinedDate(data: string): UndefinedDate {
  const indexOfKeyPassed = Object.keys(UndefinedDate).indexOf(data)

  const unit = Object.values(UndefinedDate)[indexOfKeyPassed]

  if (unit) {
    return unit
  } else throw new Error(`Could not decode UndefinedDate: '${data}'`)
}

function encodeUndefinedDate(data: UndefinedDate): string {
  return data
}

const DECODERS: MetadataValueDecoders = {
  [MetadatumType.KEYWORD]: identity,
  [MetadatumType.UNOFFICIAL_LONG_TITLE]: identity,
  [MetadatumType.UNOFFICIAL_SHORT_TITLE]: identity,
  [MetadatumType.UNOFFICIAL_ABBREVIATION]: identity,
  [MetadatumType.UNOFFICIAL_REFERENCE]: identity,
  [MetadatumType.DIVERGENT_DOCUMENT_NUMBER]: identity,
  [MetadatumType.REFERENCE_NUMBER]: identity,
  [MetadatumType.DEFINITION]: identity,
  [MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW]: identity,
  [MetadatumType.AGE_OF_MAJORITY_INDICATION]: identity,
  [MetadatumType.VALIDITY_RULE]: identity,
  [MetadatumType.LEAD_JURISDICTION]: identity,
  [MetadatumType.LEAD_UNIT]: identity,
  [MetadatumType.PARTICIPATION_TYPE]: identity,
  [MetadatumType.PARTICIPATION_INSTITUTION]: identity,
  [MetadatumType.SUBJECT_FNA]: identity,
  [MetadatumType.SUBJECT_PREVIOUS_FNA]: identity,
  [MetadatumType.SUBJECT_GESTA]: identity,
  [MetadatumType.SUBJECT_BGB_3]: identity,
  [MetadatumType.DATE]: identity,
  [MetadatumType.YEAR]: identity,
  [MetadatumType.RANGE_START]: identity,
  [MetadatumType.RANGE_END]: identity,
  [MetadatumType.ANNOUNCEMENT_MEDIUM]: identity,
  [MetadatumType.ANNOUNCEMENT_GAZETTE]: identity,
  [MetadatumType.ADDITIONAL_INFO]: identity,
  [MetadatumType.EXPLANATION]: identity,
  [MetadatumType.AREA_OF_PUBLICATION]: identity,
  [MetadatumType.NUMBER_OF_THE_PUBLICATION_IN_THE_RESPECTIVE_AREA]: identity,
  [MetadatumType.EU_GOVERNMENT_GAZETTE]: identity,
  [MetadatumType.SERIES]: identity,
  [MetadatumType.OTHER_OFFICIAL_REFERENCE]: identity,
  [MetadatumType.NUMBER]: identity,
  [MetadatumType.PAGE]: identity,
  [MetadatumType.EDITION]: identity,
  [MetadatumType.ENTITY]: identity,
  [MetadatumType.DECIDING_BODY]: identity,
  [MetadatumType.RESOLUTION_MAJORITY]: decodeBoolean,
  [MetadatumType.TYPE_NAME]: identity,
  [MetadatumType.NORM_CATEGORY]: decodeNormCategory,
  [MetadatumType.TEMPLATE_NAME]: identity,
  [MetadatumType.UNDEFINED_DATE]: decodeUndefinedDate,
  [MetadatumType.TEXT]: identity,
}

const ENCODERS: MetadataValueEncoders = {
  [MetadatumType.KEYWORD]: identity,
  [MetadatumType.UNOFFICIAL_LONG_TITLE]: identity,
  [MetadatumType.UNOFFICIAL_SHORT_TITLE]: identity,
  [MetadatumType.UNOFFICIAL_ABBREVIATION]: identity,
  [MetadatumType.UNOFFICIAL_REFERENCE]: identity,
  [MetadatumType.DIVERGENT_DOCUMENT_NUMBER]: identity,
  [MetadatumType.REFERENCE_NUMBER]: identity,
  [MetadatumType.DEFINITION]: identity,
  [MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW]: identity,
  [MetadatumType.AGE_OF_MAJORITY_INDICATION]: identity,
  [MetadatumType.VALIDITY_RULE]: identity,
  [MetadatumType.LEAD_JURISDICTION]: identity,
  [MetadatumType.LEAD_UNIT]: identity,
  [MetadatumType.PARTICIPATION_TYPE]: identity,
  [MetadatumType.PARTICIPATION_INSTITUTION]: identity,
  [MetadatumType.SUBJECT_FNA]: identity,
  [MetadatumType.SUBJECT_PREVIOUS_FNA]: identity,
  [MetadatumType.SUBJECT_GESTA]: identity,
  [MetadatumType.SUBJECT_BGB_3]: identity,
  [MetadatumType.DATE]: encodeDate,
  [MetadatumType.YEAR]: identity,
  [MetadatumType.RANGE_START]: identity,
  [MetadatumType.RANGE_END]: identity,
  [MetadatumType.ANNOUNCEMENT_MEDIUM]: identity,
  [MetadatumType.ANNOUNCEMENT_GAZETTE]: identity,
  [MetadatumType.ADDITIONAL_INFO]: identity,
  [MetadatumType.EXPLANATION]: identity,
  [MetadatumType.AREA_OF_PUBLICATION]: identity,
  [MetadatumType.NUMBER_OF_THE_PUBLICATION_IN_THE_RESPECTIVE_AREA]: identity,
  [MetadatumType.EU_GOVERNMENT_GAZETTE]: identity,
  [MetadatumType.SERIES]: identity,
  [MetadatumType.OTHER_OFFICIAL_REFERENCE]: identity,
  [MetadatumType.NUMBER]: identity,
  [MetadatumType.PAGE]: identity,
  [MetadatumType.EDITION]: identity,
  [MetadatumType.ENTITY]: identity,
  [MetadatumType.DECIDING_BODY]: identity,
  [MetadatumType.RESOLUTION_MAJORITY]: encodeBoolean,
  [MetadatumType.TYPE_NAME]: identity,
  [MetadatumType.NORM_CATEGORY]: identity,
  [MetadatumType.TEMPLATE_NAME]: identity,
  [MetadatumType.UNDEFINED_DATE]: encodeUndefinedDate,
  [MetadatumType.TEXT]: identity,
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
function encodeMetadata(metadata: Metadata): MetadatumSchema[] | null {
  if (Object.entries(metadata).length == 0) {
    return null
  }

  const encodedMapping = mapValues(metadata, (group, type) =>
    group?.map((value, index) => ({
      type,
      order: index + 1,
      // FIXME: Type system is tricky here...
      value: ENCODERS[type](value as never),
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

  const encodedSections = mergeValues(encodedMapping)
  const nonEmptySections = encodedSections.filter(
    (section) =>
      (section.metadata && section.metadata.length > 0) ||
      (section.sections && section.sections.length > 0)
  )
  return nonEmptySections
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
    applicationScopeArea: encodeString(flatMetadata.applicationScopeArea),
    applicationScopeEndDate: encodeNullDate(
      flatMetadata.applicationScopeEndDate
    ),
    applicationScopeStartDate: encodeNullDate(
      flatMetadata.applicationScopeStartDate
    ),
    celexNumber: encodeString(flatMetadata.celexNumber),
    completeCitation: encodeString(flatMetadata.completeCitation),
    digitalEvidenceAppendix: encodeString(flatMetadata.digitalEvidenceAppendix),
    digitalEvidenceExternalDataNote: encodeString(
      flatMetadata.digitalEvidenceExternalDataNote
    ),
    digitalEvidenceLink: encodeString(flatMetadata.digitalEvidenceLink),
    digitalEvidenceRelatedData: encodeString(
      flatMetadata.digitalEvidenceRelatedData
    ),
    documentCategory: encodeString(flatMetadata.documentCategory),
    documentNumber: encodeString(flatMetadata.documentNumber),
    documentStatusDate: encodeNullDate(flatMetadata.documentStatusDate),
    documentStatusDescription: encodeString(
      flatMetadata.documentStatusDescription
    ),
    documentStatusEntryIntoForceDate: encodeNullDate(
      flatMetadata.documentStatusEntryIntoForceDate
    ),
    documentStatusProof: encodeString(flatMetadata.documentStatusProof),
    documentStatusReference: encodeString(flatMetadata.documentStatusReference),
    documentStatusWorkNote: encodeString(flatMetadata.documentStatusWorkNote),
    documentTextProof: encodeString(flatMetadata.documentTextProof),
    eli: encodeString(flatMetadata.eli),
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
    otherStatusNote: encodeString(flatMetadata.otherStatusNote),
    announcementDate: encodeNullDate(flatMetadata.announcementDate),
    publicationDate: encodeNullDate(flatMetadata.publicationDate),
    reissueArticle: encodeString(flatMetadata.reissueArticle),
    reissueDate: encodeNullDate(flatMetadata.reissueDate),
    reissueNote: encodeString(flatMetadata.reissueNote),
    reissueReference: encodeString(flatMetadata.reissueReference),
    repealArticle: encodeString(flatMetadata.repealArticle),
    repealDate: encodeNullDate(flatMetadata.repealDate),
    repealNote: encodeString(flatMetadata.repealNote),
    repealReferences: encodeString(flatMetadata.repealReferences),
    risAbbreviation: encodeString(flatMetadata.risAbbreviation),
    statusDate: encodeNullDate(flatMetadata.statusDate),
    statusDescription: encodeString(flatMetadata.statusDescription),
    statusNote: encodeString(flatMetadata.statusNote),
    statusReference: encodeString(flatMetadata.statusReference),
    text: encodeString(flatMetadata.text),
  }
}

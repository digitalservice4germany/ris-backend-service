import {
  Article,
  FlatMetadata,
  Metadata,
  MetadataSectionName,
  MetadataSections,
  MetadataValueType,
  MetadatumType,
  Norm,
  Paragraph,
} from "@/domain/Norm"
import {
  InputType,
  BaseInputAttributes,
  BaseInputField,
  TextInputAttributes,
  TextInputField,
} from "@/shared/components/input/types"

type MetadataValueGenerators = {
  [Type in keyof MetadataValueType]: () => MetadataValueType[Type]
}

const METADATA_VALUE_GENERATORS: MetadataValueGenerators = {
  [MetadatumType.KEYWORD]: generateString,
  [MetadatumType.UNOFFICIAL_LONG_TITLE]: generateString,
  [MetadatumType.UNOFFICIAL_SHORT_TITLE]: generateString,
  [MetadatumType.UNOFFICIAL_ABBREVIATION]: generateString,
  [MetadatumType.UNOFFICIAL_REFERENCE]: generateString,
  [MetadatumType.DIVERGENT_DOCUMENT_NUMBER]: generateString,
  [MetadatumType.REFERENCE_NUMBER]: generateString,
  [MetadatumType.DEFINITION]: generateString,
  [MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW]: generateString,
  [MetadatumType.AGE_OF_MAJORITY_INDICATION]: generateString,
  [MetadatumType.VALIDITY_RULE]: generateString,
  [MetadatumType.LEAD_JURISDICTION]: generateString,
  [MetadatumType.LEAD_UNIT]: generateString,
  [MetadatumType.PARTICIPATION_TYPE]: generateString,
  [MetadatumType.PARTICIPATION_INSTITUTION]: generateString,
  [MetadatumType.SUBJECT_FNA]: generateString,
  [MetadatumType.SUBJECT_PREVIOUS_FNA]: generateString,
  [MetadatumType.SUBJECT_GESTA]: generateString,
  [MetadatumType.SUBJECT_BGB_3]: generateString,
}

const ALPHABET_CHARACTERS = "abcdefghijklmnopqrstuvwxyz"
const HEXADECIMAL_CHARACTERS = "0123456789abcdef"

export function generateRandomNumber(minimum = 0, maximum = 10): number {
  return Math.floor(Math.random() * (maximum - minimum) + minimum)
}

export function generateString(options?: {
  characterSet?: string
  length?: number
  prefix?: string
}): string {
  const characterSet = options?.characterSet ?? ALPHABET_CHARACTERS
  const length = options?.length ?? 5
  let output = options?.prefix ?? ""

  for (let i = 0; i < length; i++) {
    output += characterSet.charAt(
      generateRandomNumber(0, characterSet.length - 1)
    )
  }

  return output
}

export function generateBaseInputAttributes(
  partialAttributes?: Partial<BaseInputAttributes>
): BaseInputAttributes {
  return {
    ariaLabel: generateString({ prefix: "aria-label-" }),
    ...partialAttributes,
  }
}

export function generateTextInputAttributes(
  partialAttributes?: Partial<TextInputAttributes>
): TextInputAttributes {
  return {
    ...generateBaseInputAttributes(),
    placeholder: undefined,
    ...partialAttributes,
  }
}

export function generateBaseInputField(
  partialField?: Partial<BaseInputField>
): BaseInputField {
  return {
    type: InputType.TEXT,
    name: generateString({ prefix: "name-" }),
    label: generateString({ prefix: "Label " }),
    inputAttributes: generateBaseInputAttributes(),
    ...partialField,
  }
}

export function generateTextInputField(
  partialField?: Partial<TextInputField>
): TextInputField {
  return {
    ...generateBaseInputField(),
    type: InputType.TEXT,
    inputAttributes: generateTextInputAttributes(),
    ...partialField,
  }
}
export function generateGuid(): string {
  const first = generateString({
    length: 8,
    characterSet: HEXADECIMAL_CHARACTERS,
  })
  const second = generateString({
    length: 4,
    characterSet: HEXADECIMAL_CHARACTERS,
  })
  const third = generateString({
    length: 4,
    characterSet: HEXADECIMAL_CHARACTERS,
  })
  const fourth = generateString({
    length: 4,
    characterSet: HEXADECIMAL_CHARACTERS,
  })
  const fith = generateString({
    length: 12,
    characterSet: HEXADECIMAL_CHARACTERS,
  })
  return `${first}-${second}-${third}-${fourth}-${fith}`
}

export function generateParagraph(
  partialParagraph?: Partial<Paragraph>
): Paragraph {
  return {
    guid: generateGuid(),
    marker: generateString({ prefix: "marker " }),
    text: generateString({ prefix: "text", length: 40 }),
    ...partialParagraph,
  }
}

export function generateArticle(partialArticle?: Partial<Article>): Article {
  return {
    guid: generateGuid(),
    title: generateString({ prefix: "title " }),
    marker: generateString({ prefix: "marker " }),
    paragraphs: [generateParagraph()],
    ...partialArticle,
  }
}

export function pickRandomMetadatumType(): MetadatumType {
  const options = Object.keys(MetadatumType)
  const index = generateRandomNumber(0, options.length - 1)
  return options[index] as MetadatumType
}

export function generateMetadata(partialMetadata?: Partial<Metadata>) {
  const metadata = {} as Metadata
  const metadataCount = generateRandomNumber()

  for (let i = 0; i < metadataCount; i++) {
    const type = pickRandomMetadatumType()
    const values = new Array(generateRandomNumber())
      .fill(0)
      .map(METADATA_VALUE_GENERATORS[type])
    metadata[type] = values
  }

  Object.entries(partialMetadata ?? {}).forEach(([type, values]) => {
    metadata[type as MetadatumType] = values
  })

  return metadata
}

export function pickRandomMetadataSectionName(): MetadataSectionName {
  const options = Object.keys(MetadataSectionName)
  const index = generateRandomNumber(0, options.length - 1)
  return options[index] as MetadataSectionName
}

export function generateMetadataSections(
  partialSections?: Partial<MetadataSections>
): MetadataSections {
  const sections = {} as MetadataSections

  for (let i = 0; i < generateRandomNumber(1); i++) {
    const name = pickRandomMetadataSectionName()
    // Do not use child sections here to prevent random infinite loop.
    sections[name] = new Array(generateRandomNumber())
      .fill(0)
      .map(generateMetadata)
  }

  Object.entries(partialSections ?? {}).forEach(([name, metadata]) => {
    sections[name as MetadataSectionName] = metadata
  })

  return sections
}

export function generateFlatMetadata(
  partialFlatMetadata?: Partial<FlatMetadata>
): FlatMetadata {
  return {
    documentTemplateName: generateString(),
    ageIndicationEnd: generateString(),
    ageIndicationStart: generateString(),
    announcementDate: generateString(),
    categorizedReference: generateString(),
    celexNumber: generateString(),
    citationDate: generateString(),
    completeCitation: generateString(),
    digitalAnnouncementDate: generateString(),
    digitalAnnouncementArea: generateString(),
    digitalAnnouncementAreaNumber: generateString(),
    digitalAnnouncementEdition: generateString(),
    digitalAnnouncementExplanations: generateString(),
    digitalAnnouncementInfo: generateString(),
    digitalAnnouncementMedium: generateString(),
    digitalAnnouncementPage: generateString(),
    digitalAnnouncementYear: generateString(),
    digitalEvidenceAppendix: generateString(),
    digitalEvidenceExternalDataNote: generateString(),
    digitalEvidenceLink: generateString(),
    digitalEvidenceRelatedData: generateString(),
    divergentEntryIntoForceDate: generateString(),
    divergentEntryIntoForceDateState: generateString(),
    divergentExpirationDate: generateString(),
    divergentExpirationDateState: generateString(),
    documentCategory: generateString(),
    documentNormCategory: generateString(),
    documentNumber: generateString(),
    documentStatusDate: generateString(),
    documentStatusDescription: generateString(),
    documentStatusEntryIntoForceDate: generateString(),
    documentStatusProof: generateString(),
    documentStatusReference: generateString(),
    documentStatusWorkNote: generateString(),
    documentTextProof: generateString(),
    documentTypeName: generateString(),
    entryIntoForceDate: generateString(),
    entryIntoForceDateState: generateString(),
    euAnnouncementExplanations: generateString(),
    euAnnouncementGazette: generateString(),
    euAnnouncementInfo: generateString(),
    euAnnouncementNumber: generateString(),
    euAnnouncementPage: generateString(),
    euAnnouncementSeries: generateString(),
    euAnnouncementYear: generateString(),
    eli: generateString(),
    expirationDate: generateString(),
    expirationDateState: generateString(),
    expirationNormCategory: generateString(),
    isExpirationDateTemp: false,
    officialAbbreviation: generateString(),
    officialLongTitle: generateString(),
    officialShortTitle: generateString(),
    otherDocumentNote: generateString(),
    otherFootnote: generateString(),
    footnoteChange: generateString(),
    footnoteComment: generateString(),
    footnoteDecision: generateString(),
    footnoteStateLaw: generateString(),
    footnoteEuLaw: generateString(),
    otherOfficialAnnouncement: generateString(),
    otherStatusNote: generateString(),
    principleEntryIntoForceDate: generateString(),
    principleEntryIntoForceDateState: generateString(),
    principleExpirationDate: generateString(),
    principleExpirationDateState: generateString(),
    printAnnouncementExplanations: generateString(),
    printAnnouncementGazette: generateString(),
    printAnnouncementInfo: generateString(),
    printAnnouncementNumber: generateString(),
    printAnnouncementPage: generateString(),
    printAnnouncementYear: generateString(),
    providerEntity: generateString(),
    providerDecidingBody: generateString(),
    providerIsResolutionMajority: false,
    publicationDate: generateString(),
    reissueArticle: generateString(),
    reissueDate: generateString(),
    reissueNote: generateString(),
    reissueReference: generateString(),
    repealArticle: generateString(),
    repealDate: generateString(),
    repealNote: generateString(),
    repealReferences: generateString(),
    risAbbreviation: generateString(),
    statusDate: generateString(),
    statusDescription: generateString(),
    statusNote: generateString(),
    statusReference: generateString(),
    text: generateString(),
    ...partialFlatMetadata,
  }
}

export function generateNorm(partialNorm?: Partial<Norm>): Norm {
  return {
    guid: generateGuid(),
    articles: [generateArticle()],
    metadataSections: generateMetadataSections(),
    ...generateFlatMetadata(),
    ...partialNorm,
  }
}

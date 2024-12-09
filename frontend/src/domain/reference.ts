import { DocumentType } from "./documentUnit"
import EditableListItem from "./editableListItem"
import RelatedDocumentation from "./relatedDocumentation"
import LegalPeriodical from "@/domain/legalPeriodical"

export default class Reference implements EditableListItem {
  id?: string
  citation?: string
  referenceSupplement?: string
  footnote?: string
  legalPeriodical?: LegalPeriodical
  legalPeriodicalRawValue?: string
  documentationUnit?: RelatedDocumentation
  documentType?: DocumentType
  author?: string
  referenceType: "caselaw" | "literature" = "caselaw"

  static readonly requiredFields = [
    "legalPeriodical",
    "citation",
    "referenceSupplement",
  ] as const

  static readonly requiredFieldsForDocunit = [
    "legalPeriodical",
    "citation",
  ] as const

  static readonly requiredLiteratureFields = [
    "legalPeriodical",
    "citation",
    "documentType",
    "author",
  ] as const

  static readonly fields = [
    "legalPeriodical",
    "citation",
    "referenceSupplement",
    "documentationUnit",
    "author",
    "documentType",
  ] as const

  constructor(data: Partial<Reference> = {}) {
    Object.assign(this, data)

    if (this.documentationUnit) {
      this.documentationUnit = new RelatedDocumentation({
        ...data.documentationUnit,
      })
    }
    if (this.id == undefined) {
      this.id = crypto.randomUUID()
    }
  }

  /**
   * Returns true if documentation unit is created by a reference as a part of edition.
   */
  getIsDocumentationUnitCreatedByReference(): boolean {
    return this.documentationUnit?.createdByReference === this.id
  }

  get renderDecision(): string {
    return [
      this.author ? `${this.author},` : "",
      this.legalPeriodical?.abbreviation ?? this.legalPeriodicalRawValue,
      this.citation,
      this.referenceSupplement ? ` (${this.referenceSupplement})` : "",
      this.documentType ? ` (${this.documentType.jurisShortcut})` : "",
    ]
      .filter(Boolean)
      .join(" ")
  }

  get hasMissingRequiredFields(): boolean {
    return this.missingRequiredFields.length > 0
  }

  get missingRequiredFields() {
    return Reference.requiredFields.filter((field) =>
      this.fieldIsEmpty(this[field]),
    )
  }

  get hasMissingRequiredFieldsForDocunit(): boolean {
    return this.missingRequiredFieldsForDocunit.length > 0
  }

  get missingRequiredFieldsForDocunit() {
    return Reference.requiredFieldsForDocunit.filter((field) =>
      this.fieldIsEmpty(this[field]),
    )
  }

  get hasMissingRequiredLiteratureFields(): boolean {
    return this.missingRequiredLiteratureFields.length > 0
  }

  get missingRequiredLiteratureFields() {
    return Reference.requiredLiteratureFields.filter((field) =>
      this.fieldIsEmpty(this[field]),
    )
  }

  equals(entry: Reference): boolean {
    return this.id === entry.id
  }

  get isEmpty(): boolean {
    let isEmpty = true

    Reference.fields.map((key) => {
      if (!this.fieldIsEmpty(this[key])) {
        isEmpty = false
      }
    })
    return isEmpty
  }

  fieldIsEmpty(value: Reference[(typeof Reference.fields)[number]]) {
    return value === undefined || !value || Object.keys(value).length === 0
  }

  get hasForeignSource(): boolean {
    return true
  }
}

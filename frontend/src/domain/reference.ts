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

  static readonly requiredFields = ["legalPeriodical", "citation"] as const

  static readonly fields = [
    "legalPeriodical",
    "citation",
    "referenceSupplement",
    "documentationUnit",
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

  get renderDecision(): string {
    return [
      this.legalPeriodical?.abbreviation ?? this.legalPeriodicalRawValue,
      this.citation,
      this.referenceSupplement ? ` (${this.referenceSupplement})` : "",
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

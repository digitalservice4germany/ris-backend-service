import EditableListItem from "./editableListItem"
import { NormAbbreviation } from "./normAbbreviation"
import SingleNorm from "./singleNorm"

export default class NormReference implements EditableListItem {
  public normAbbreviation?: NormAbbreviation
  public singleNorms?: SingleNorm[]
  public normAbbreviationRawValue?: string
  public hasForeignSource: boolean = false

  static readonly requiredFields = ["normAbbreviation"] as const
  static readonly fields = [
    "normAbbreviation",
    "normAbbreviationRawValue",
  ] as const

  constructor(data: Partial<NormReference> = {}) {
    Object.assign(this, data)
  }

  get hasAmbiguousNormReference(): boolean {
    return !this.normAbbreviation && !!this.normAbbreviationRawValue
  }

  get renderDecision(): string {
    let result: string[]
    if (this.normAbbreviation?.abbreviation) {
      result = [`${this.normAbbreviation?.abbreviation}`]
    } else if (this.normAbbreviationRawValue) {
      result = [`${this.normAbbreviationRawValue}`]
    } else {
      result = []
    }
    return [...result].join(", ")
  }

  get isEmpty(): boolean {
    let isEmpty = true

    NormReference.fields.map((key) => {
      if (!this.fieldIsEmpty(this[key])) {
        isEmpty = false
      }
    })
    return isEmpty
  }

  get hasMissingFieldsInLegalForce() {
    if (this.singleNorms) {
      return (
        this.singleNorms.filter((singleNorm) => {
          return singleNorm.legalForce?.hasMissingRequiredFields
        }).length > 0
      )
    }
    return false
  }

  private fieldIsEmpty(
    value: NormReference[(typeof NormReference.fields)[number]],
  ) {
    return value === undefined || !value || Object.keys(value).length === 0
  }
}

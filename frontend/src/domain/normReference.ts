import EditableListItem from "./editableListItem"
import { NormAbbreviation } from "./normAbbreviation"
import SingleNorm from "./singleNorm"

export default class NormReference implements EditableListItem {
  public normAbbreviation?: NormAbbreviation
  public singleNorms?: SingleNorm[]
  public normAbbreviationRawValue?: string
  public hasForeignSource: boolean = false
  // public uuid?: string

  static readonly requiredFields = ["normAbbreviation"] as const
  static readonly fields = [
    "normAbbreviation",
    "normAbbreviationRawValue",
  ] as const

  constructor(data: Partial<NormReference> = {}) {
    Object.assign(this, data)
  }

  get id() {
    return this.normAbbreviation
      ? this.normAbbreviation.id
      : this.normAbbreviationRawValue
  }

  get hasAmbiguousNormReference(): boolean {
    return !this.normAbbreviation && !!this.normAbbreviationRawValue
  }

  equals(entry: NormReference): boolean {
    if (entry.isEmpty) return true
    let isEquals = false
    if (this.normAbbreviation) {
      isEquals = entry.normAbbreviation
        ? this.normAbbreviation?.abbreviation ==
          entry.normAbbreviation.abbreviation
        : false
    } else if (this.normAbbreviationRawValue) {
      isEquals = this.normAbbreviationRawValue == entry.normAbbreviationRawValue
    }
    return isEquals
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

  /**
   * Checks if there are any single norms with missing required fields in their legal force.
   *
   * @returns `true` if any single norm has missing required fields in its legal force; otherwise, `false`.
   */
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

  /**
   * Returns a boolean value, if a given normEntry has single norms and respectively renders the norms' summary in one
   * compact line or in a sublist, below the norm abbreviation.
   */
  get hasSingleNorms() {
    if (this.singleNorms) {
      return this.singleNorms?.length > 0 && !this.singleNorms[0].isEmpty
    } else return false
  }

  private fieldIsEmpty(
    value: NormReference[(typeof NormReference.fields)[number]],
  ) {
    return value === undefined || !value || Object.keys(value).length === 0
  }
}

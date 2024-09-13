import Reference from "./reference"
import LegalPeriodical from "@/domain/legalPeriodical"

export default class LegalPeriodicalEdition {
  static readonly fields = [
    "name",
    "prefix",
    "suffix",
    "legalPeriodical",
  ] as const

  static readonly requiredFields = ["name", "legalPeriodical"] as const

  id?: string
  name?: string
  prefix?: string
  suffix?: string
  legalPeriodical?: LegalPeriodical
  references?: Reference[]
  createdAt?: Date

  constructor(data: Partial<LegalPeriodicalEdition> = {}) {
    Object.assign(this, data)

    this.legalPeriodical = new LegalPeriodical({ ...this.legalPeriodical })
    this.references = this.references
      ? this.references.map((reference) => new Reference({ ...reference }))
      : []

    this.id = this.id ?? crypto.randomUUID()
  }

  /**
   * Gets the list of missing required fields.
   * @type {string[]}
   * @readonly
   */
  get missingRequiredFields() {
    return LegalPeriodicalEdition.requiredFields.filter((field) =>
      this.fieldIsEmpty(this[field]),
    )
  }

  private fieldIsEmpty(
    value: LegalPeriodicalEdition[(typeof LegalPeriodicalEdition.fields)[number]],
  ): boolean {
    return !value
  }

  get isEmpty(): boolean {
    const emptyFields = LegalPeriodicalEdition.fields.filter((field) =>
      this.fieldIsEmpty(this[field]),
    )
    return emptyFields.length !== LegalPeriodicalEdition.fields.length
  }
}

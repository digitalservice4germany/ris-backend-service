import dayjs from "dayjs"
import { CitationStyle } from "./citationStyle"
import EditableListItem from "./editableListItem"
import LinkedDocumentUnit from "./linkedDocumentUnit"

export default class ActiveCitation
  extends LinkedDocumentUnit
  implements EditableListItem
{
  public citationStyle?: CitationStyle

  static requiredFields = [
    "citationStyle",
    "fileNumber",
    "court",
    "decisionDate",
  ] as const

  constructor(data: Partial<ActiveCitation> = {}) {
    super()
    Object.assign(this, data)
  }

  get renderDecision(): string {
    return [
      ...(this.citationStyle?.label ? [this.citationStyle.label] : []),
      ...(this.court?.label ? [`${this.court?.label}`] : []),
      ...(this.decisionDate
        ? [dayjs(this.decisionDate).format("DD.MM.YYYY")]
        : []),
      ...(this.fileNumber ? [this.fileNumber] : []),
      ...(this.documentType ? [this.documentType.label] : []),
      ...(this.documentNumber && this.isReadOnly ? [this.documentNumber] : []),
    ].join(", ")
  }

  get hasMissingRequiredFields(): boolean {
    return this.missingRequiredFields.length > 0
  }

  get missingRequiredFields(): string[] {
    return ActiveCitation.requiredFields.filter((field) =>
      this.requiredFieldIsEmpty(this[field]),
    )
  }

  private requiredFieldIsEmpty(
    value: ActiveCitation[(typeof ActiveCitation.requiredFields)[number]],
  ): boolean {
    if (value === undefined || !value || value === null) {
      return true
    }

    return false
  }
}

export const activeCitationLabels: { [name: string]: string } = {
  citationStyle: "Art der Zitierung",
  fileNumber: "Aktenzeichen",
  court: "Gericht",
  decisionDate: "Entscheidungsdatum",
}

import dayjs from "dayjs"
import { Court, DocumentType } from "./documentUnit"
import {
  Label,
  PublicationState,
  PublicationStatus,
} from "@/domain/publicationStatus"

export default class RelatedDocumentation {
  public uuid?: string
  public documentNumber?: string
  public status?: PublicationStatus
  public deviatingFileNumber?: string
  public court?: Court
  public decisionDate?: string
  public fileNumber?: string
  public documentType?: DocumentType
  public referenceFound?: boolean

  get hasForeignSource(): boolean {
    return this.documentNumber != null && !!this.referenceFound
  }

  constructor(data: Partial<RelatedDocumentation> = {}) {
    Object.assign(this, data)
  }

  public isLinkedWith<Type extends RelatedDocumentation>(
    localDecisions: Type[] | undefined,
  ): boolean {
    if (!localDecisions) return false

    return localDecisions.some(
      (localDecision) => localDecision.documentNumber == this.documentNumber,
    )
  }

  public static getStatusLabel(status: PublicationStatus) {
    if (!status) return ""

    switch (status.publicationStatus) {
      case PublicationState.PUBLISHED:
        return Label.PUBLISHED
      case PublicationState.UNPUBLISHED:
        return Label.UNPUBLISHED
      case PublicationState.PUBLISHING:
        return Label.PUBLISHING
      case PublicationState.DUPLICATED:
        return Label.DUPLICATED
      case PublicationState.LOCKED:
        return Label.LOCKED
      case PublicationState.DELETING:
        return Label.DELETING
      default:
        return ""
    }
  }

  get renderDecision(): string {
    return [
      ...(this.court ? [`${this.court?.label}`] : []),
      ...(this.decisionDate
        ? [dayjs(this.decisionDate).format("DD.MM.YYYY")]
        : []),
      ...(this.fileNumber ? [this.fileNumber] : []),
      ...(this.documentType?.label ? [this.documentType.label] : []),
      ...(this.status
        ? [RelatedDocumentation.getStatusLabel(this.status)]
        : []),
    ].join(", ")
  }
}

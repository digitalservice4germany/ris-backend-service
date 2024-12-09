import DocumentUnit, { Court, DocumentType } from "./documentUnit"
import DocumentationOffice from "@/domain/documentationOffice"

export default class DocumentUnitListEntry {
  public id?: string
  uuid?: string
  documentNumber?: string
  decisionDate?: string
  lastPublicationDateTime?: string
  scheduledPublicationDateTime?: string
  status?: NonNullable<DocumentUnit["status"]>
  fileNumber?: string
  documentType?: DocumentType
  court?: Court
  appraisalBody?: string
  hasHeadnoteOrPrinciple?: boolean
  hasAttachments?: boolean
  note?: string
  isDeletable?: boolean
  isEditable?: boolean
  source?: string
  creatingDocumentationOffice?: DocumentationOffice

  constructor(data: Partial<DocumentUnitListEntry> = {}) {
    Object.assign(this, data)
  }
}

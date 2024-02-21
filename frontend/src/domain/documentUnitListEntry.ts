import DocumentUnit, { Court, DocumentType } from "./documentUnit"

export default class DocumentUnitListEntry {
  public id?: string
  uuid?: string
  documentNumber?: string
  decisionDate?: string
  status?: NonNullable<DocumentUnit["status"]>
  fileNumber?: string
  fileName?: string
  documentType?: DocumentType
  court?: Court
  appraisalBody?: string

  constructor(data: Partial<DocumentUnitListEntry> = {}) {
    Object.assign(this, data)
  }
}

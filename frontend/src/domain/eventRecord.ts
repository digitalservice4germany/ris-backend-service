export enum EventRecordType {
  HANDOVER = "HANDOVER",
  HANDOVER_REPORT = "HANDOVER_REPORT",
  MIGRATION = "MIGRATION",
}

export class MailAttachment {
  public fileName?: string
  public fileContent?: string
}

export default class EventRecord {
  public type?: EventRecordType
  public date?: string
  public content?: string // for reports
  public xml?: string // for delta migrations
  public attachments?: MailAttachment[] // for handovers
  public statusMessages?: string[]
  public success?: boolean
  public receiverAddress?: string
  public mailSubject?: string
  public fileName?: string // for edition preview
}

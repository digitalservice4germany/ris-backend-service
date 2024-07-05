export enum PublicationHistoryRecordType {
  PUBLICATION = "PUBLICATION",
  PUBLICATION_REPORT = "PUBLICATION_REPORT",
  MIGRATION = "MIGRATION",
}

export default class PublicationHistoryRecord {
  public type?: PublicationHistoryRecordType // readonly status?: "PUBLISHED" | "UNPUBLISHED"
  public date?: string
  public content?: string
  public xml?: string
  public statusMessages?: string[]
  public statusCode?: string
  public receiverAddress?: string
  public mailSubject?: string
}

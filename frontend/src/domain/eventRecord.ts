import DateUtil from "@/utils/dateUtil"

class MailAttachment {
  constructor(
    public fileName?: string,
    public fileContent?: string,
  ) {}
}

enum HandoverEntityType {
  DOCUMENTATION_UNIT,
  EDITION,
}

export default interface EventRecord {
  type: EventRecordType
  getDate(): string
  getContent(): string
  setContent(content: string): void
}

export enum EventRecordType {
  HANDOVER = "HANDOVER",
  HANDOVER_REPORT = "HANDOVER_REPORT",
  MIGRATION = "MIGRATION",
}

export class HandoverMail implements EventRecord {
  entityId?: string
  entityType?: HandoverEntityType
  receiverAddress?: string
  mailSubject?: string
  attachments: MailAttachment[] = []
  success?: boolean
  statusMessages?: string[]
  date?: string
  issuerAddress?: string
  type: EventRecordType = EventRecordType.HANDOVER

  constructor(data: Partial<HandoverMail> = {}) {
    Object.assign(this, data)
  }

  getType(): EventRecordType {
    return EventRecordType.HANDOVER
  }

  getDate(): string {
    return DateUtil.formatDateTime(this.date ?? "")
  }

  getContent(): string {
    return this.attachments?.[0].fileContent ?? ""
  }

  setContent(content: string): void {
    this.attachments[0].fileContent = content
  }
}

export class HandoverReport implements EventRecord {
  entityId?: string
  content?: string
  date?: string
  type: EventRecordType = EventRecordType.HANDOVER_REPORT

  constructor(data: Partial<HandoverReport> = {}) {
    Object.assign(this, data)
  }

  getDate(): string {
    return DateUtil.formatDateTime(this.date ?? "")
  }

  getContent(): string {
    return this.content ?? ""
  }

  setContent(content: string): void {
    this.content = content
  }
}

export class DeltaMigration implements EventRecord {
  xml?: string
  date?: string
  type: EventRecordType = EventRecordType.MIGRATION

  constructor(data: Partial<DeltaMigration> = {}) {
    Object.assign(this, data)
  }

  getDate(): string {
    return DateUtil.formatDateTime(this.date ?? "")
  }

  getContent(): string {
    return this.xml ?? ""
  }

  setContent(content: string): void {
    this.xml = content
  }
}

export class Preview {
  xml?: string
  creationDate?: string
  success?: boolean
  statusMessages?: string[]
  fileName?: string

  constructor(data: Partial<Preview> = {}) {
    Object.assign(this, data)
  }

  getDate(): string {
    return DateUtil.formatDateTime(this.creationDate ?? "")
  }

  getContent(): string {
    return this.xml ?? ""
  }

  setContent(content: string): void {
    this.xml = content
  }
}

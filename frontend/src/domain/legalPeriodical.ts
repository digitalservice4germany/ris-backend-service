export default class LegalPeriodical {
  uuid?: string
  title?: string
  subtitle?: string
  abbreviation?: string
  primaryReference?: boolean
  citationStyle?: string

  constructor(data: Partial<LegalPeriodical> = {}) {
    Object.assign(this, data)
  }
}

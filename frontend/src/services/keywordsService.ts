import httpClient, { ServiceResponse } from "./httpClient"

interface KeywordService {
  getKeywords(uuid: string): Promise<ServiceResponse<string[]>>
  addKeyword(uuid: string, keyword: string): Promise<ServiceResponse<string[]>>
  deleteKeyword(
    uuid: string,
    keyword: string,
  ): Promise<ServiceResponse<string[]>>
}

const service: KeywordService = {
  async getKeywords(uuid: string) {
    const response = await httpClient.get<string[]>(
      `caselaw/documentunits/${uuid}/contentrelatedindexing/keywords`,
    )
    if (response.status >= 300) {
      response.error = {
        title: `Schlagwörter konnten nicht geladen werden.`,
      }
    }
    return response
  },
  async addKeyword(uuid: string, keyword: string) {
    const encodedkeyword = encodeURIComponent(keyword)
    const encodedString = `caselaw/documentunits/${uuid}/contentrelatedindexing/keywords/${encodedkeyword}`

    const response = await httpClient.put<string, string[]>(encodedString)
    if (response.status >= 300) {
      response.error = {
        title: `Schlagwort ${keyword} konnte nicht hinzugefügt werden`,
      }
    }
    return response
  },
  async deleteKeyword(uuid: string, keyword: string) {
    const encodedkeyword = encodeURIComponent(keyword)
    const encodedString = `caselaw/documentunits/${uuid}/contentrelatedindexing/keywords/${encodedkeyword}`

    const response = await httpClient.delete<string[]>(encodedString)
    if (response.status >= 300) {
      response.error = {
        title: `Schlagwort ${keyword} konnte nicht entfernt werden`,
      }
    }
    return response
  },
}

export default service

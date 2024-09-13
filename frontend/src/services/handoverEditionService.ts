import httpClient, { ServiceResponse } from "./httpClient"
import EventRecord from "@/domain/eventRecord"
import errorMessages from "@/i18n/errors.json"

interface HandoverEditionService {
  handoverEdition(editionId: string): Promise<ServiceResponse<EventRecord>>
  getEventLog(editionId: string): Promise<ServiceResponse<EventRecord[]>>
  getPreview(editionId: string): Promise<ServiceResponse<EventRecord[]>>
}

const service: HandoverEditionService = {
  async handoverEdition(editionId: string) {
    const response = await httpClient.put<string, EventRecord>(
      `caselaw/legalperiodicaledition/${editionId}/handover`,
      {
        headers: { "Content-Type": "text/plain" },
      },
    )

    if (response.status >= 300 || !response.data?.success) {
      let description = "Die Ausgabe kann nicht übergeben werden."
      if (
        response.data?.statusMessages &&
        response.data.statusMessages.length > 0
      ) {
        description += '<ul class="list-disc">'
      }
      response.data?.statusMessages?.forEach(
        (value) =>
          (description +=
            '<li class="ds-body-02-reg font-bold ml-[1rem] list-item">' +
            value +
            "</li>"),
      )
      if (
        response.data?.statusMessages &&
        response.data.statusMessages.length > 0
      ) {
        description += "</ul>"
      }

      response.error = {
        title: errorMessages.EDITION_HANDOVER_FAILED.title,
        description,
      }
    }

    return response
  },

  async getEventLog(editionId?: string) {
    const response = await httpClient.get<EventRecord[]>(
      `caselaw/legalperiodicaledition/${editionId}/handover`,
    )

    response.error =
      response.status >= 300
        ? {
            title: errorMessages.EDITION_LOADING_HANDOVER_FAILED.title,
            description:
              errorMessages.EDITION_LOADING_HANDOVER_FAILED.description,
          }
        : undefined

    return response
  },

  async getPreview(editionId?: string) {
    const response = await httpClient.get<EventRecord[]>(
      `caselaw/legalperiodicaledition/${editionId}/preview-xml`,
    )

    if (
      response.status >= 300 ||
      !Array.isArray(response.data) ||
      response.data.filter((record) => !record.success).length > 0
    ) {
      response.error = {
        title: errorMessages.EDITION_LOADING_XML_PREVIEW.title,
        description:
          (Array.isArray(response.data) &&
            response.data
              ?.filter((record) => !record.success)
              .flatMap((record) => record.statusMessages)
              .join(", ")) ||
          errorMessages.EDITION_LOADING_XML_PREVIEW.description,
      }
    }

    return response
  },
}

export default service

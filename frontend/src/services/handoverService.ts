import httpClient, { ServiceResponse } from "./httpClient"
import EventRecord from "@/domain/eventRecord"
import errorMessages from "@/i18n/errors.json"

interface HandoverService {
  handoverDocument(
    documentUnitUuid: string,
  ): Promise<ServiceResponse<EventRecord>>
  getEventLog(documentUnitUuid: string): Promise<ServiceResponse<EventRecord[]>>
  getPreview(documentUnitUuid: string): Promise<ServiceResponse<EventRecord>>
}

const service: HandoverService = {
  async handoverDocument(documentUnitUuid: string) {
    const response = await httpClient.put<string, EventRecord>(
      `caselaw/documentunits/${documentUnitUuid}/handover`,
      {
        headers: { "Content-Type": "text/plain" },
      },
    )

    if (response.status >= 300 || !response.data?.success) {
      let description = "Die Dokumentationseinheit kann nicht übergeben werden."
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
        title: errorMessages.DOCUMENT_UNIT_HANDOVER_FAILED.title,
        description,
      }
    }

    return response
  },

  async getEventLog(documentUnitUuid: string) {
    const response = await httpClient.get<EventRecord[]>(
      `caselaw/documentunits/${documentUnitUuid}/handover`,
    )

    response.error =
      response.status >= 300
        ? {
            title: errorMessages.DOCUMENT_UNIT_LOADING_HANDOVER_FAILED.title,
            description:
              errorMessages.DOCUMENT_UNIT_LOADING_HANDOVER_FAILED.description,
          }
        : undefined

    return response
  },

  async getPreview(documentUnitUuid: string) {
    const response = await httpClient.get<EventRecord>(
      `caselaw/documentunits/${documentUnitUuid}/preview-xml`,
    )

    if (response.status >= 300 || !response.data?.success) {
      response.error = {
        title: errorMessages.DOCUMENT_UNIT_LOADING_XML_PREVIEW.title,
        description:
          response.data?.statusMessages &&
          response.data.statusMessages.length > 0
            ? response.data?.statusMessages
            : errorMessages.DOCUMENT_UNIT_LOADING_XML_PREVIEW.description,
      }
    }

    return response
  },
}

export default service

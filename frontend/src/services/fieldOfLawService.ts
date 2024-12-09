import httpClient, { ServiceResponse } from "./httpClient"
import { Page } from "@/components/Pagination.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import errorMessages from "@/i18n/errors.json"

interface FieldOfLawService {
  getSelectedFieldsOfLaw(uuid: string): Promise<ServiceResponse<FieldOfLaw[]>>

  addFieldOfLaw(
    uuid: string,
    identifier: string,
  ): Promise<ServiceResponse<FieldOfLaw[]>>

  removeFieldOfLaw(
    uuid: string,
    identifier: string,
  ): Promise<ServiceResponse<FieldOfLaw[]>>

  getChildrenOf(identifier: string): Promise<ServiceResponse<FieldOfLaw[]>>

  getTreeForIdentifier(identifier: string): Promise<ServiceResponse<FieldOfLaw>>

  searchForFieldsOfLaw(
    page: number,
    size: number,
    query?: string,
    identifier?: string,
    norm?: string,
  ): Promise<ServiceResponse<Page<FieldOfLaw>>>
}

const service: FieldOfLawService = {
  async getSelectedFieldsOfLaw(uuid: string) {
    const response = await httpClient.get<FieldOfLaw[]>(
      `caselaw/documentunits/${uuid}/contentrelatedindexing/fieldsoflaw`,
    )
    if (response.status >= 300) {
      response.error = {
        title:
          errorMessages.DOCUMENT_UNIT_FIELDS_OF_LAW_COULD_NOT_BE_LOADED.title.replace(
            "${uuid}",
            uuid,
          ),
      }
    }
    return response
  },
  async addFieldOfLaw(uuid: string, identifier: string) {
    const response = await httpClient.put<undefined, FieldOfLaw[]>(
      `caselaw/documentunits/${uuid}/contentrelatedindexing/fieldsoflaw/${identifier}`,
    )
    if (response.status >= 300) {
      response.error = {
        title:
          errorMessages.DOCUMENT_UNIT_FIELDS_OF_LAW_COULD_NOT_BE_ADDED.title
            .replace("${identifier}", identifier)
            .replace("${uuid}", uuid),
      }
    }
    return response
  },
  async removeFieldOfLaw(uuid: string, identifier: string) {
    const response = await httpClient.delete<FieldOfLaw[]>(
      `caselaw/documentunits/${uuid}/contentrelatedindexing/fieldsoflaw/${identifier}`,
    )
    if (response.status >= 300) {
      response.error = {
        title:
          errorMessages.DOCUMENT_UNIT_FIELDS_OF_LAW_COULD_NOT_BE_DELETED.title
            .replace("${identifier}", identifier)
            .replace("${uuid}", uuid),
      }
    }
    return response
  },
  async getChildrenOf(identifier: string) {
    const response = await httpClient.get<FieldOfLaw[]>(
      `caselaw/fieldsoflaw/${identifier}/children`,
    )
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.FIELDS_OF_LAW_COULD_NOT_BE_LOADED.title.replace(
          "${identifier}",
          identifier,
        ),
      }
    }
    return response
  },
  async getTreeForIdentifier(identifier: string) {
    const response = await httpClient.get<FieldOfLaw>(
      `caselaw/fieldsoflaw/${identifier}/tree`,
    )
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.FIELD_OF_LAW_COULD_NOT_BE_LOADED.title,
      }
    }
    return response
  },
  async searchForFieldsOfLaw(
    page: number,
    size: number,
    query?: string,
    identifier?: string,
    norm?: string,
  ) {
    const response = await httpClient.get<Page<FieldOfLaw>>(
      `caselaw/fieldsoflaw?pg=${page}&sz=${size}`,
      {
        params: {
          q: query ?? "",
          identifier: identifier ?? "",
          norm: norm ?? "",
        },
      },
    )
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.FIELD_OF_LAW_SEARCH_FAILED.title,
      }
    }
    return response
  },
}

export default service

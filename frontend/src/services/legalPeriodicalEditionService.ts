import { UUID } from "crypto"
import httpClient, { ResponseError, ServiceResponse } from "./httpClient"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import errorMessages from "@/i18n/errors.json"

interface LegalPeriodicalEditionService {
  get(uuid: UUID): Promise<ServiceResponse<LegalPeriodicalEdition>>

  getAllByLegalPeriodicalId(
    legalPeriodicalId: string,
  ): Promise<ServiceResponse<LegalPeriodicalEdition[]>>

  save(
    legalPeriodicalEdition: LegalPeriodicalEdition,
  ): Promise<ServiceResponse<LegalPeriodicalEdition>>

  delete(editionId: string): Promise<ServiceResponse<unknown>>
}

const service: LegalPeriodicalEditionService = {
  async get(
    legalPeriodicalId: string,
  ): Promise<ServiceResponse<LegalPeriodicalEdition>> {
    const response = await httpClient.get<LegalPeriodicalEdition>(
      `caselaw/legalperiodicaledition/${legalPeriodicalId}`,
    )

    if (response.status >= 300) {
      response.error = {
        title: errorMessages.LEGAL_PERIODICAL_EDITION_COULD_NOT_BE_LOADED.title,
      }
    }
    return response
  },

  async getAllByLegalPeriodicalId(
    legalPeriodicalId: string,
  ): Promise<ServiceResponse<LegalPeriodicalEdition[]>> {
    const response = await httpClient.get<LegalPeriodicalEdition[]>(
      `caselaw/legalperiodicaledition`,
      {
        params: {
          legal_periodical_id: legalPeriodicalId,
        },
      },
    )
    if (response.status >= 300) {
      response.error = {
        title:
          errorMessages.LEGAL_PERIODICAL_EDITIONS_COULD_NOT_BE_LOADED.title,
      }
    }

    return response
  },

  async save(legalPeriodicalEdition: LegalPeriodicalEdition) {
    const response = await httpClient.put<
      LegalPeriodicalEdition,
      LegalPeriodicalEdition
    >(
      `caselaw/legalperiodicaledition`,
      {
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
      },
      legalPeriodicalEdition,
    )
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.LEGAL_PERIODICAL_EDITION_COULD_NOT_BE_SAVED.title,
      } as ResponseError
    }
    return response
  },

  async delete(editionId: string) {
    const response = await httpClient.delete(
      `caselaw/legalperiodicaledition/${editionId}`,
    )
    if (response.status >= 300) {
      response.error = {
        title:
          errorMessages.LEGAL_PERIODICAL_EDITION_COULD_NOT_BE_DELETED.title,
      } as ResponseError
    }
    return response
  },
}

export default service

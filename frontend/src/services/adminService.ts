import httpClient, { ServiceResponse } from "./httpClient"
import { Env } from "@/domain/env"
import errorMessages from "@/i18n/errors.json"

interface AdminService {
  getEnv(): Promise<ServiceResponse<Env>>
  getAccountManagementUrl(): Promise<ServiceResponse<string>>
}

const service: AdminService = {
  async getEnv() {
    const response = await httpClient.get<Env>("admin/env")
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.ENV_COULD_NOT_BE_LOADED.title,
      }
    }
    return response
  },
  async getAccountManagementUrl() {
    const response = await httpClient.get<string>("admin/accountManagementUrl")
    if (response.status >= 300) {
      response.error = {
        title: "Account management could not be loaded.",
      }
    }
    return response
  },
}

export default service

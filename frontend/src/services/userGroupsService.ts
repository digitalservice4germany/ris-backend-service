import httpClient, { ServiceResponse } from "./httpClient"
import { UserGroup } from "@/domain/userGroup"
import errorMessages from "@/i18n/errors.json"

interface UserGroupService {
  get(): Promise<ServiceResponse<UserGroup[]>>
}

const service: UserGroupService = {
  async get() {
    const response = await httpClient.get<UserGroup[]>(`caselaw/user-group`)
    if (response.status >= 300) {
      response.error = {
        title: errorMessages.SERVER_ERROR.title,
      }
    }
    return response
  },
}

export default service

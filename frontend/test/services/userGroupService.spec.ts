import { UserGroup } from "@/domain/userGroup"
import errorMessages from "@/i18n/errors.json"
import httpClient, { ServiceResponse } from "@/services/httpClient"
import service from "@/services/userGroupsService"

vi.mock("@/services/httpClient")

describe("userGroupService", () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it("should fetch user groups", async () => {
    const userGroups = [
      {
        id: "userGroupId",
        userGroupPathName: "/DS/Extern",
      },
    ]

    const httpClientGet = vi
      .mocked(httpClient)
      .get.mockResolvedValueOnce({ status: 200, data: userGroups })

    const result = (await service.get()).data as UserGroup[]

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result).toEqual(userGroups)
  })

  it("should return error on status >= 300", async () => {
    const httpClientGet = vi
      .mocked(httpClient)
      .get.mockResolvedValueOnce({ status: 400, data: {} })

    const result = (await service.get()) as ServiceResponse<UserGroup[]>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.error?.title).toEqual(errorMessages.SERVER_ERROR.title)
  })
})

import errorMessages from "@/i18n/errors.json"
import httpClient, { ServiceResponse } from "@/services/httpClient"
import service from "@/services/procedureService"

vi.mock("@/services/httpClient")

describe("procedureService", () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it("should assign user group", async () => {
    const procedureId = "procedureId"
    const userGroupId = "userGroupId"

    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 200, data: "response" })

    const result = (await service.assignUserGroup(
      procedureId,
      userGroupId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(200)
    expect(result.data).toEqual("response")
  })

  it("should return error on status >= 300 when assign", async () => {
    const procedureId = "procedureId"
    const userGroupId = "userGroupId"

    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 300, data: "response" })

    const result = (await service.assignUserGroup(
      procedureId,
      userGroupId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(300)
    expect(result.error?.title).toEqual(
      errorMessages.PROCEDURE_COULD_NOT_BE_ASSIGNED.title,
    )
  })

  it("should return not allowed error on status 403 when assign", async () => {
    const procedureId = "procedureId"
    const userGroupId = "userGroupId"

    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 403, data: "response" })

    const result = (await service.assignUserGroup(
      procedureId,
      userGroupId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(403)
    expect(result.error?.title).toEqual(
      errorMessages.NOT_ALLOWED.title +
        ". " +
        errorMessages.PROCEDURE_COULD_NOT_BE_ASSIGNED.title,
    )
  })

  it("should unassign user group", async () => {
    const procedureId = "procedureId"

    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 200, data: "response" })

    const result = (await service.unassignUserGroup(
      procedureId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(200)
    expect(result.data).toEqual("response")
  })

  it("should return error on status >= 300 when unassign", async () => {
    const procedureId = "procedureId"

    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 300, data: "response" })

    const result = (await service.unassignUserGroup(
      procedureId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(300)
    expect(result.error?.title).toEqual(
      errorMessages.PROCEDURE_COULD_NOT_BE_UNASSIGNED.title,
    )
  })

  it("should return not allowed error on status 403 when unassign", async () => {
    const procedureId = "procedureId"
    const httpClientGet = vi
      .mocked(httpClient)
      .put.mockResolvedValueOnce({ status: 403, data: "response" })

    const result = (await service.unassignUserGroup(
      procedureId,
    )) as ServiceResponse<string>

    expect(httpClientGet).toHaveBeenCalledOnce()
    expect(result.status).toEqual(403)
    expect(result.error?.title).toEqual(
      errorMessages.NOT_ALLOWED.title +
        ". " +
        errorMessages.PROCEDURE_COULD_NOT_BE_UNASSIGNED.title,
    )
  })
})

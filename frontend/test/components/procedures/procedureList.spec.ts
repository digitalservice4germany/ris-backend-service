import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { UseFetchReturn } from "@vueuse/core"
import { afterEach, expect, vi } from "vitest"
import { ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import { Page } from "@/components/Pagination.vue"
import ProcedureList from "@/components/procedures/ProcedureList.vue"
import { Procedure } from "@/domain/documentUnit"
import featureToggleService from "@/services/featureToggleService"
import service from "@/services/procedureService"
import userGroupsService from "@/services/userGroupsService"

vi.mock("@/services/procedureService")
vi.mock("@/services/authService")
vi.mock("@/services/userGroupsService")

let isInternalUser = false
vi.mock("@/composables/useInternalUser", () => {
  return {
    useInternalUser: () => isInternalUser,
  }
})

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes: [
    {
      path: "",
      name: "index",
      component: {},
    },
  ],
})

async function renderComponent(options?: { procedures: Procedure[][] }) {
  const proceduresError = ref()
  const abortProcedures = vi.fn()
  const canAbort = ref(false)
  const isFetchingProcedures = ref(false)
  const proceduresPagesResponse = ref<Page<Procedure>>({
    content: options?.procedures[0] ?? [
      {
        id: "abc",
        label: "testProcedure",
        documentationUnitCount: 2,
        createdAt: "foo",
      },
    ],
    size: 1,
    number: 0,
    numberOfElements: 200,
    first: true,
    last: false,
    empty: false,
  })
  const executeGetProcedures = vi.fn().mockImplementation(
    () =>
      (proceduresPagesResponse.value = {
        content: options?.procedures[1] ?? [
          {
            label: "testProcedure",
            documentationUnitCount: 2,
            createdAt: "foo",
          },
        ],
        size: 1,
        number: 1,
        numberOfElements: 200,
        first: true,
        last: false,
        empty: false,
      }),
  )
  const mockedGetProcedures = vi.mocked(service.get).mockReturnValueOnce({
    error: proceduresError,
    isFetching: isFetchingProcedures,
    execute: executeGetProcedures,
    canAbort,
    abort: abortProcedures,
    data: proceduresPagesResponse,
  } as unknown as UseFetchReturn<Page<Procedure>>)

  const mockedGetDocumentUnits = vi
    .mocked(service.getDocumentUnits)
    .mockResolvedValue({
      status: 200,
      data: [{ documentNumber: "testABC" }],
    })

  const mockedGetUserGroups = vi
    .mocked(userGroupsService.get)
    .mockResolvedValue({
      status: 200,
      data: [
        {
          id: "userGroupId1",
          userGroupPathName: "DS/Extern/Agentur1",
        },
        {
          id: "userGroupId2",
          userGroupPathName: "DS/Extern/Agentur2",
        },
      ],
    })

  const mockedAssignProcedure = vi
    .mocked(service.assignUserGroup)
    .mockResolvedValue({
      status: 200,
      data: ["Success Response"],
    })

  return {
    ...render(ProcedureList, {
      global: {
        stubs: { routerLink: { template: "<a><slot /></a>" } },
        plugins: [router, createTestingPinia()],
      },
    }),

    user: userEvent.setup(),
    mockedGetProcedures,
    mockedGetDocumentUnits,
    mockedGetUserGroups,
    mockedAssignProcedure,
    proceduresPagesResponse,
    executeGetProcedures,
  }
}

describe("ProcedureList", () => {
  beforeEach(() => {
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it("fetches docUnits once from BE if expanded", async () => {
    const { mockedGetProcedures, mockedGetDocumentUnits, user } =
      await renderComponent()
    expect(mockedGetProcedures).toHaveBeenCalledOnce()
    expect(mockedGetDocumentUnits).not.toHaveBeenCalled()

    await user.click(await screen.findByTestId("icons-open-close"))
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()

    // do not fetch again
    await user.click(await screen.findByTestId("icons-open-close"))
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()
  })

  it("does not fetch docUnits if unecessary", async () => {
    const { mockedGetDocumentUnits, user } = await renderComponent({
      procedures: [
        [
          {
            id: "1",
            label: "foo",
            documentationUnitCount: 0,
            createdAt: "2023-09-18T19:57:01.826083Z",
          },
        ],
      ],
    })

    expect(mockedGetDocumentUnits).not.toHaveBeenCalled()

    await user.click(await screen.findByText(/foo/))
    await user.click(await screen.findByTestId("icons-open-close"))
    expect(mockedGetDocumentUnits).not.toHaveBeenCalledOnce()
  })

  it("searches on entry", async () => {
    const queryReplaceSpy = vi.spyOn(router, "replace")
    const { user, executeGetProcedures } = await renderComponent({
      procedures: [
        [
          {
            id: "1",
            label: "foo",
            documentationUnitCount: 0,
            createdAt: "2023-09-18T19:57:01.826083Z",
          },
          {
            id: "2",
            label: "bar",
            documentationUnitCount: 0,
            createdAt: "2023-09-18T19:57:01.826083Z",
          },
        ],
      ],
    })

    await user.type(await screen.findByLabelText("Nach Vorgängen suchen"), "b")

    // Query is updated immediately
    expect(queryReplaceSpy).toHaveBeenCalledOnce()
    expect(queryReplaceSpy).toHaveBeenCalledWith(
      expect.objectContaining({ query: { q: "b" } }),
    )
    expect(executeGetProcedures).not.toHaveBeenCalledOnce()

    await new Promise((resolve) => setTimeout(resolve, 500))

    // Request to server is debounced
    expect(executeGetProcedures).toHaveBeenCalledOnce()
  })

  it("resets currently expanded procedures on search", async () => {
    const { mockedGetDocumentUnits, user } = await renderComponent()

    expect(mockedGetDocumentUnits).not.toHaveBeenCalled()

    await user.click(await screen.findByTestId("icons-open-close"))
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()
    await user.type(await screen.findByLabelText("Nach Vorgängen suchen"), "b")
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()
  })

  it("resets currently expanded procedures on page change", async () => {
    const { user } = await renderComponent()

    expect(screen.queryByText("testABC")).not.toBeInTheDocument()

    // Expand procedure to load doc units
    await user.click(await screen.findByTestId("icons-open-close"))

    expect(screen.getByText("testABC")).toBeInTheDocument()
    expect(screen.getByText("Dokumentnummer")).toBeInTheDocument()

    // Go to next page
    await user.click(await screen.findByLabelText("nächste Ergebnisse"))

    expect(screen.queryByText("testABC")).not.toBeInTheDocument()
    expect(screen.queryByText("Dokumentnummer")).not.toBeInTheDocument()
  })

  it("show increment page count when going to next page", async () => {
    const { user } = await renderComponent()

    expect(screen.getByText("Seite 1:")).toBeInTheDocument()

    // Go to next page
    await user.click(await screen.findByLabelText("nächste Ergebnisse"))

    expect(screen.getByText("Seite 2:")).toBeInTheDocument()
  })

  it("should fetch userGroups onBeforeMounted", async () => {
    const { mockedGetUserGroups } = await renderComponent()
    expect(mockedGetUserGroups).toHaveBeenCalledOnce()
  })

  it("should list all user groups and default option in dropdown", async () => {
    isInternalUser = true
    const { mockedGetProcedures } = await renderComponent()
    expect(mockedGetProcedures).toHaveBeenCalledOnce()

    const dropdown = await screen.findByLabelText("dropdown input")
    expect(dropdown).toBeEnabled()

    const options = screen.getAllByRole("option")
    expect(options.length).toBe(3)
    expect(options[0]).toHaveTextContent("Agentur1")
    expect(options[1]).toHaveTextContent("Agentur2")
    expect(options[2]).toHaveTextContent("Nicht zugewiesen")
  })

  it("should hide dropdown when user is external", async () => {
    isInternalUser = false
    const { mockedGetProcedures } = await renderComponent()
    expect(mockedGetProcedures).toHaveBeenCalledOnce()

    expect(
      screen.queryByText("Es wurden noch keine Vorgänge angelegt."),
    ).not.toBeInTheDocument()

    expect(screen.queryByLabelText("dropdown input")).not.toBeInTheDocument()
  })
})

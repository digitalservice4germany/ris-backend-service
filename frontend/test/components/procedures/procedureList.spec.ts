import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { afterEach, beforeEach, expect, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import ProcedureList from "@/components/procedures/ProcedureList.vue"
import useQuery from "@/composables/useQueryFromRoute"
import { Procedure } from "@/domain/documentUnit"
import featureToggleService from "@/services/featureToggleService"
import service from "@/services/procedureService"
import userGroupsService from "@/services/userGroupsService"

vi.mock("@/services/procedureService")
vi.mock("@/services/authService")
vi.mock("@/services/userGroupsService")

const mocks = vi.hoisted(() => ({
  mockedPushQuery: vi.fn(),
}))

let isInternalUser = false
vi.mock("@/composables/useInternalUser", () => {
  return {
    useInternalUser: () => isInternalUser,
  }
})

vi.mock("@/composables/useQueryFromRoute", async () => {
  const actual = (
    await vi.importActual<{ default: typeof useQuery }>(
      "@/composables/useQueryFromRoute",
    )
  ).default
  return {
    default: () => {
      return {
        ...actual(),
        pushQueryToRoute: mocks.mockedPushQuery,
      }
    },
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
  const mockedGetProcedures = vi
    .mocked(service.get)
    .mockResolvedValueOnce({
      status: 200,
      data: {
        content: options?.procedures[0] ?? [
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
      },
    })
    .mockResolvedValueOnce({
      status: 200,
      data: {
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
      },
    })

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
    // eslint-disable-next-line testing-library/await-async-events
    user: userEvent.setup(),
    mockedGetProcedures,
    mockedGetDocumentUnits,
    mockedGetUserGroups,
    mockedAssignProcedure,
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
    const { user, mockedGetProcedures } = await renderComponent({
      procedures: [
        [
          {
            label: "foo",
            documentationUnitCount: 0,
            createdAt: "2023-09-18T19:57:01.826083Z",
          },
          {
            label: "bar",
            documentationUnitCount: 0,
            createdAt: "2023-09-18T19:57:01.826083Z",
          },
        ],
      ],
    })

    await user.type(await screen.findByLabelText("Nach Vorgängen suchen"), "b")
    expect(mockedGetProcedures).toHaveBeenCalledWith(10, 0, "b")

    expect(mocks.mockedPushQuery).not.toHaveBeenCalled()
  })

  it("debounces route updates on search", async () => {
    const { user } = await renderComponent()
    await user.type(await screen.findByLabelText("Nach Vorgängen suchen"), "b")

    expect(mocks.mockedPushQuery).not.toHaveBeenCalled()

    await new Promise((resolve) => {
      setTimeout(resolve, 500)
    })

    expect(mocks.mockedPushQuery).toHaveBeenCalledWith({ q: "b" })
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
    const { mockedGetDocumentUnits, user } = await renderComponent()

    expect(mockedGetDocumentUnits).not.toHaveBeenCalled()

    await user.click(await screen.findByTestId("icons-open-close"))
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()
    await user.click(await screen.findByLabelText("nächste Ergebnisse"))
    expect(mockedGetDocumentUnits).toHaveBeenCalledOnce()
  })

  it("should fetch userGroups onMounted", async () => {
    const { mockedGetUserGroups } = await renderComponent()
    expect(mockedGetUserGroups).toHaveBeenCalledOnce()
  })

  it("should list all user groups and default option in dropdown", async () => {
    isInternalUser = true
    const { mockedGetProcedures } = await renderComponent()
    expect(mockedGetProcedures).toHaveBeenCalledOnce()

    expect(
      await screen.findByText("Es wurden noch keine Vorgänge angelegt."),
    ).not.toBeVisible()

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
      await screen.findByText("Es wurden noch keine Vorgänge angelegt."),
    ).not.toBeVisible()

    expect(screen.queryByLabelText("dropdown input")).not.toBeInTheDocument()
  })

  it("should enable dropdown when user is internal", async () => {
    isInternalUser = true
    const { mockedGetProcedures } = await renderComponent()
    expect(mockedGetProcedures).toHaveBeenCalledOnce()

    expect(
      await screen.findByText("Es wurden noch keine Vorgänge angelegt."),
    ).not.toBeVisible()

    const dropdown = await screen.findByLabelText("dropdown input")
    expect(dropdown).toBeEnabled()
  })
})

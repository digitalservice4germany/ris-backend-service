import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import EditionId from "@/routes/caselaw/periodical-evaluation/[editionId].vue"
import featureToggleService from "@/services/featureToggleService"
import LegalPeriodicalEditionService from "@/services/legalPeriodicalEditionService"

const editionUuid = crypto.randomUUID()

async function renderComponent() {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: "/",
        name: "home",
        component: {},
      },
      {
        path: "/caselaw/periodical-evaluation/:editionId",
        name: "caselaw-periodical-evaluation-editionId",
        component: EditionId,
      },
      {
        path: "/caselaw/periodical-evaluation/:editionId/edition",
        name: "caselaw-periodical-evaluation-editionId-edition",
        component: {
          template: "<div data-testid='edition'>Ausgabe</div>",
        },
      },
      {
        path: "/caselaw/periodical-evaluation/:editionId/references",
        name: "caselaw-periodical-evaluation-editionId-references",
        component: {
          template: "<div data-testid='references'>Fundstellen</div>",
        },
      },
      {
        path: "/caselaw/periodical-evaluation/:editionId/handover",
        name: "caselaw-periodical-evaluation-editionId-handover",
        component: {
          template: "<div data-testid='handover'>Übergabe</div>",
        },
      },
    ],
  })

  // Mock the route with a specific uuid before rendering
  await router.push({
    name: "caselaw-periodical-evaluation-editionId",
    params: { editionId: editionUuid },
  })

  // Wait for the router to be ready
  return router.isReady().then(() => ({
    user,
    ...render(EditionId, {
      global: {
        plugins: [
          router,
          [
            createTestingPinia({
              stubActions: false,
            }),
          ],
        ],
      },
    }),
  }))
}

describe("Edition Id Route", () => {
  beforeEach(() => {
    vi.spyOn(LegalPeriodicalEditionService, "save").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: new LegalPeriodicalEdition({
          id: editionUuid,
          legalPeriodical: { abbreviation: "BDZ" },
          name: "name",
        }),
      }),
    )
    vi.spyOn(LegalPeriodicalEditionService, "get").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: new LegalPeriodicalEdition({
          id: editionUuid,
          legalPeriodical: { abbreviation: "BDZ" },
          name: "name",
        }),
      }),
    )
    vi.spyOn(
      LegalPeriodicalEditionService,
      "getAllByLegalPeriodicalId",
    ).mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: [
          new LegalPeriodicalEdition({
            id: editionUuid,
            legalPeriodical: { abbreviation: "BDZ" },
            name: "name",
          }),
        ],
      }),
    )
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  test("renders legal periodical and edition name in title", async () => {
    await renderComponent()
    // await router.push({ name: 'caselaw-periodical-evaluation-editionId-edition', params: { editionId: editionUuid } })
    const titleElements = screen.getAllByText("Periodikumsauswertung")
    const separatorElements = screen.getAllByText("|")
    const editionNames = screen.getAllByText("BDZ name")

    expect(titleElements.length).toBeGreaterThan(1)
    expect(titleElements[0]).toHaveClass("font-bold")

    expect(separatorElements.length).toBeGreaterThan(1)
    expect(separatorElements[0]).toHaveClass("m-4")

    expect(editionNames.length).toBeGreaterThan(1)
    expect(editionNames[0]).not.toHaveClass("font-bold")
  })
})

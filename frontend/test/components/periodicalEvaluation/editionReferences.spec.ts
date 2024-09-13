import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import EditionReferences from "@/components/periodical-evaluation/PeriodicalReferences.vue"
import LegalPeriodical from "@/domain/legalPeriodical"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import { ServiceResponse } from "@/services/httpClient"
import service from "@/services/legalPeriodicalEditionService"
import testRoutes from "~/test-helper/routes"

const editionUUid = crypto.randomUUID()

async function renderComponent() {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: testRoutes,
  })

  // Mock the route with a specific uuid before rendering
  await router.push({
    name: "caselaw-periodical-evaluation-editionId-references",
    params: { editionId: editionUUid },
  })

  const legalPeriodical: LegalPeriodical = {
    uuid: "1",
    abbreviation: "BDZ",
    citationStyle: "2024, Heft 1",
  }

  // Wait for the router to be ready
  return router.isReady().then(() => ({
    user,
    ...render(EditionReferences, {
      global: {
        plugins: [
          router,
          [
            createTestingPinia({
              initialState: {
                editionStore: new LegalPeriodicalEdition({
                  id: editionUUid,
                  legalPeriodical: legalPeriodical,
                  name: "name",
                  prefix: "präfix",
                  suffix: "suffix",
                  references: [],
                }),
              },
              stubActions: false,
            }),
          ],
        ],
      },
    }),
  }))
}

describe("Legal periodical edition evaluation", () => {
  beforeEach(async () => {
    const legalPeriodical: LegalPeriodical = {
      uuid: "1",
      abbreviation: "BDZ",
      citationStyle: "2024, Heft 1",
    }
    vi.spyOn(service, "get").mockImplementation(
      (): Promise<ServiceResponse<LegalPeriodicalEdition>> =>
        Promise.resolve({
          status: 200,
          data: new LegalPeriodicalEdition({
            id: editionUUid,
            legalPeriodical: legalPeriodical,
            name: "name",
            prefix: "präfix",
            suffix: "suffix",
            references: [],
          }),
        }),
    )
    vi.spyOn(service, "save").mockImplementation(
      (): Promise<ServiceResponse<LegalPeriodicalEdition>> =>
        Promise.resolve({
          status: 200,
          data: new LegalPeriodicalEdition({
            id: editionUUid,
            legalPeriodical: legalPeriodical,
            name: "name",
            prefix: "präfix",
            suffix: "suffix",
            references: [],
          }),
        }),
    )
  })

  test("renders legal periodical and edition name in title", async () => {
    await renderComponent()
    expect(
      screen.getByLabelText("Zitatstelle Präfix", { exact: true }),
    ).toHaveValue("präfix")
    expect(screen.getByText("Zitierbeispiel: 2024, Heft 1")).toBeInTheDocument()

    expect(
      screen.getByLabelText("Gericht", { exact: true }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText("Aktenzeichen", { exact: true }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText("Entscheidungsdatum", { exact: true }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText("Dokumenttyp", { exact: true }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText("Zitatstelle *", { exact: true }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText("Klammernzusatz", { exact: true }),
    ).toBeInTheDocument()
  })
})

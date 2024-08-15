import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import EditionEvaluation from "@/components/legalperiodical/EditionEvaluation.vue"
import { LegalPeriodical } from "@/domain/reference"
import service from "@/services/legalPeriodicalEditionService"

function renderComponent() {
  // eslint-disable-next-line testing-library/await-async-events
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: "/caselaw/documentUnit/new",
        name: "new",
        component: {},
      },
      {
        path: "/",
        name: "home",
        component: {},
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/categories",
        name: "caselaw-documentUnit-documentNumber-categories",
        component: {},
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/preview",
        name: "caselaw-documentUnit-documentNumber-preview",
        component: {},
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/files",
        name: "caselaw-documentUnit-documentNumber-files",
        component: {},
      },
      {
        path: "/caselaw/legal-periodical-editions/:uuid",
        name: "caselaw-legal-periodical-editions-uuid",
        component: {},
      },
    ],
  })
  return {
    user,
    ...render(EditionEvaluation, {
      global: { plugins: [router] },
    }),
  }
}

describe("Legal periodical edition list", () => {
  beforeEach(() => {
    const legalPeriodical: LegalPeriodical = {
      legalPeriodicalAbbreviation: "BDZ",
    }
    vi.spyOn(service, "get").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: {
          id: crypto.randomUUID(),
          legalPeriodical: legalPeriodical,
          name: "name",
          prefix: "präfix",
          suffix: "suffix",
          references: [],
        },
      }),
    )
  })

  test("renders correctly", async () => {
    renderComponent()
    expect(screen.getByText("Periodikaauswertung")).toBeVisible()
    // todo
  })
})

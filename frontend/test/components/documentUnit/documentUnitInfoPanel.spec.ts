import { createTestingPinia } from "@pinia/testing"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitInfoPanel from "@/components/DocumentUnitInfoPanel.vue"
import DocumentUnit, { CoreData } from "@/domain/documentUnit"
import routes from "~/test-helper/routes"

function renderComponent(options?: { heading?: string; coreData?: CoreData }) {
  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })
  return {
    ...render(DocumentUnitInfoPanel, {
      props: { heading: options?.heading ?? "" },
      global: {
        plugins: [
          router,
          createTestingPinia({
            initialState: {
              docunitStore: {
                documentUnit: new DocumentUnit("foo", {
                  documentNumber: "1234567891234",
                  coreData: options?.coreData ?? {
                    court: {
                      type: "AG",
                      location: "Test",
                      label: "AG Test",
                    },
                  },
                }),
              },
            },
          }),
        ],
      },
    }),
  }
}

describe("documentUnit InfoPanel", () => {
  it("renders heading if given", async () => {
    renderComponent({ heading: "test heading" })

    screen.getAllByText("test heading")
  })

  it("renders all given property infos in correct order", async () => {
    const coreData = {
      decisionDate: "2024-01-01",
      fileNumbers: ["AZ123"],
      court: {
        type: "AG",
        location: "Test",
        label: "AG Test",
      },
    }
    renderComponent({ coreData: coreData })

    expect(
      await screen.findByText("AG Test, AZ123, 01.01.2024"),
    ).toBeInTheDocument()
  })

  it("omits incomplete coredata fields from rendering", async () => {
    renderComponent()

    expect(await screen.findByText("AG Test")).toBeInTheDocument()
  })
})

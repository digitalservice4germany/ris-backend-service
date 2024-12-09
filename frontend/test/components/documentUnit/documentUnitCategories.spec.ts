import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitCategories from "@/components/DocumentUnitCategories.vue"
import DocumentUnit from "@/domain/documentUnit"
import routes from "~/test-helper/routes"

function renderComponent() {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })
  return {
    user,
    ...render(DocumentUnitCategories, {
      global: {
        plugins: [
          createTestingPinia({
            initialState: {
              session: { user: { roles: ["Internal"] } },
              docunitStore: {
                documentUnit: new DocumentUnit("foo", {
                  documentNumber: "1234567891234",
                }),
              },
            },
          }),
          [router],
        ],
      },
    }),
  }
}

describe("Document Unit Categories", () => {
  test("renders all categories", async () => {
    renderComponent()

    expect(screen.getByRole("heading", { name: "Stammdaten" })).toBeVisible()

    expect(
      screen.getByRole("heading", { name: "Rechtszug" }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole("heading", { name: "Inhaltliche Erschließung" }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole("heading", { name: "Kurz- & Langtexte" }),
    ).toBeInTheDocument()
  })
})

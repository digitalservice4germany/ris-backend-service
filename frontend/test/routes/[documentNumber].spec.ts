import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createHead } from "@unhead/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnit from "@/domain/documentUnit"
import categories from "@/routes/caselaw/documentUnit/[documentNumber]/categories.vue"
import DocumentNumber from "@/routes/caselaw/documentUnit/[documentNumber].vue"
import documentUnitService from "@/services/documentUnitService"
import featureToggleService from "@/services/featureToggleService"
import { ServiceResponse } from "@/services/httpClient"

function renderComponent() {
  const user = userEvent.setup()
  const head = createHead()

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
        component: categories,
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/attachments",
        name: "caselaw-documentUnit-documentNumber-attachments",
        component: {
          template: "<div data-testid='attachments'>Attachments</div>",
        },
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/references",
        name: "caselaw-documentUnit-documentNumber-references",
        component: {
          template:
            "<div data-testid='references'>References<input aria-label=\"Periodikum\"/></div>",
        },
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/handover",
        name: "caselaw-documentUnit-documentNumber-handover",
        component: {
          template: "<div data-testid='handover'>Handover</div>",
        },
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/preview",
        name: "caselaw-documentUnit-documentNumber-preview",
        component: {
          template: "<div data-testid='preview'>Preview</div>",
        },
      },
    ],
  })
  return {
    user,
    router,
    ...render(DocumentNumber, {
      props: {
        documentNumber: "1234567891234",
      },
      global: {
        plugins: [
          head,
          router,
          [
            createTestingPinia({
              initialState: {
                session: { user: { roles: ["Internal"] } },
                docunitStore: {
                  documentUnit: new DocumentUnit("foo", {
                    documentNumber: "1234567891234",
                    coreData: {
                      court: {
                        type: "AG",
                        location: "Test",
                        label: "AG Test",
                      },
                    },
                  }),
                },
              },
              stubActions: false,
            }),
          ],
        ],
      },
    }),
  }
}

describe("Document Number Route", () => {
  beforeEach(() => {
    vi.spyOn(documentUnitService, "update").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: { documentationUnitVersion: 1, patch: [], errorPaths: [] },
      }),
    )
    vi.spyOn(documentUnitService, "getByDocumentNumber").mockImplementation(
      () =>
        Promise.resolve({
          status: 200,
          data: new DocumentUnit("foo", {
            documentNumber: "1234567891234",
          }),
        }),
    )
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  describe("Conditional rendering", () => {
    test("should render categories with side panels and header", async () => {
      const { router, container } = renderComponent()

      await router.push({
        path: "/caselaw/documentUnit/1234567891234/categories",
      })

      expect(screen.getByTestId("side-toggle-navigation")).toBeInTheDocument()

      // Header with save button
      expect(screen.getByTestId("document-unit-info-panel")).toBeInTheDocument()
      expect(
        screen.getByRole("button", { name: "Speichern Button" }),
      ).toBeInTheDocument()

      // ExtraContentSidePanel
      expect(
        screen.getByRole("button", { name: "Seitenpanel öffnen" }),
      ).toBeInTheDocument()

      // Main route is rendered
      expect(container.querySelector("#coreData")).toBeInTheDocument()
    })

    test("should render documents with side panels and header", async () => {
      const { router } = renderComponent()

      await router.push({
        path: "/caselaw/documentUnit/1234567891234/attachments",
      })

      expect(screen.getByTestId("side-toggle-navigation")).toBeInTheDocument()

      // Header with save button
      expect(screen.getByTestId("document-unit-info-panel")).toBeInTheDocument()

      expect(
        screen.queryByRole("button", { name: "Speichern Button" }),
      ).toBeInTheDocument()

      // ExtraContentSidePanel
      expect(
        screen.getByRole("button", { name: "Seitenpanel öffnen" }),
      ).toBeInTheDocument()

      // Main route is rendered
      expect(screen.getByTestId("attachments")).toBeInTheDocument()
    })

    test("should render handover with only nav side panel and header", async () => {
      const { router } = renderComponent()

      await router.push({
        path: "/caselaw/documentUnit/1234567891234/handover",
      })

      expect(screen.getByTestId("side-toggle-navigation")).toBeInTheDocument()

      // Header without save button
      expect(screen.getByTestId("document-unit-info-panel")).toBeInTheDocument()
      expect(
        screen.queryByRole("button", { name: "Speichern Button" }),
      ).not.toBeInTheDocument()

      // ExtraContentSidePanel
      expect(
        screen.queryByRole("button", { name: "Seitenpanel öffnen" }),
      ).not.toBeInTheDocument()

      // Main route is rendered
      expect(screen.getByTestId("handover")).toBeInTheDocument()
    })

    test("should render preview without side panels and header", async () => {
      const { router } = renderComponent()
      await router.push({ path: "/caselaw/documentUnit/1234567891234/preview" })

      expect(
        screen.queryByTestId("side-toggle-navigation"),
      ).not.toBeInTheDocument()

      // Header with save button
      expect(
        screen.queryByTestId("document-unit-info-panel"),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole("button", { name: "Speichern Button" }),
      ).not.toBeInTheDocument()

      // ExtraContentSidePanel
      expect(
        screen.queryByRole("button", { name: "Seitenpanel öffnen" }),
      ).not.toBeInTheDocument()

      // Main route is rendered
      expect(screen.getByTestId("preview")).toBeInTheDocument()
    })

    test("should render error page without document", async () => {
      vi.spyOn(documentUnitService, "getByDocumentNumber").mockImplementation(
        () =>
          Promise.resolve({
            status: 404,
            data: undefined,
            error: { title: "Backend_Error_Title" },
          } as ServiceResponse<DocumentUnit>),
      )

      const { router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/categories",
      })

      // Navigation
      expect(
        screen.queryByTestId("side-toggle-navigation"),
      ).not.toBeInTheDocument()

      // Header with save button
      expect(
        screen.queryByTestId("document-unit-info-panel"),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole("button", { name: "Speichern Button" }),
      ).not.toBeInTheDocument()

      // ExtraContentSidePanel
      expect(
        screen.queryByRole("button", { name: "Seitenpanel öffnen" }),
      ).not.toBeInTheDocument()

      // Error page is rendered
      expect(screen.getByText("Backend_Error_Title")).toBeInTheDocument()
    })
  })

  describe("Shortcuts", () => {
    it('detects "n" keypress and opens notes', async () => {
      const { user, router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/references?showAttachmentPanel=false",
      })

      expect(screen.getByLabelText("Notiz Eingabefeld")).not.toBeVisible()
      await user.keyboard("n")
      expect(screen.getByLabelText("Notiz Eingabefeld")).toBeVisible()
    })

    it('detects "v" keypress and opens preview', async () => {
      const { user, router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/references?showAttachmentPanel=false",
      })

      expect(screen.queryByTestId("preview")).not.toBeInTheDocument()
      await user.keyboard("v")
      expect(screen.getByTestId("preview")).toBeInTheDocument()
    })

    it('detects "d" keypress and opens documents', async () => {
      const { user, router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/references?showAttachmentPanel=false",
      })

      expect(
        screen.queryByTestId(
          "Wenn eine Datei hochgeladen ist, können Sie die Datei hier sehen.",
        ),
      ).not.toBeInTheDocument()
      await user.keyboard("d")
      expect(
        screen.getByText(
          "Wenn eine Datei hochgeladen ist, können Sie die Datei hier sehen.",
        ),
      ).toBeVisible()
    })

    it('detects ">" keypress and opens both panels', async () => {
      const { user, router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/references?showAttachmentPanel=false",
      })

      expect(
        screen.queryByLabelText("Seitenpanel schließen"),
      ).not.toBeInTheDocument()
      await user.keyboard("<")
      expect(screen.getByLabelText("Seitenpanel schließen")).toBeVisible()
      await user.keyboard("<")
      expect(
        screen.queryByLabelText("Seitenpanel schließen"),
      ).not.toBeInTheDocument()
    })

    it('does not detect ">" when input is focused', async () => {
      const { user, router } = renderComponent()
      await router.push({
        path: "/caselaw/documentUnit/1234567891234/references?showAttachmentPanel=false",
      })

      expect(
        screen.queryByLabelText("Seitenpanel schließen"),
      ).not.toBeInTheDocument()
      screen.getByLabelText("Periodikum").focus()
      expect(screen.getByLabelText("Periodikum")).toHaveFocus()
      await user.keyboard("<")
      expect(
        screen.queryByLabelText("Seitenpanel schließen"),
      ).not.toBeInTheDocument()
    })
  })
})

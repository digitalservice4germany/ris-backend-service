import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitHandover from "@/components/DocumentUnitHandover.vue"
import DocumentUnit from "@/domain/documentUnit"
import { HandoverMail, HandoverReport, Preview } from "@/domain/eventRecord"
import documentUnitService from "@/services/documentUnitService"
import handoverDocumentationUnitService from "@/services/handoverDocumentationUnitService"
import routes from "~/test-helper/routes"

function renderComponent() {
  const user = userEvent.setup()
  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })

  return {
    user,
    ...render(DocumentUnitHandover, {
      global: {
        plugins: [
          createTestingPinia({
            initialState: {
              docunitStore: {
                documentUnit: new DocumentUnit("foo", {
                  documentNumber: "1234567891234",
                  coreData: {
                    fileNumbers: ["123"],
                    court: {
                      label: "foo",
                    },
                    decisionDate: "2024-01-01",
                    documentType: { jurisShortcut: "", label: "" },
                    legalEffect: "Ja",
                  },
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
describe("Document Unit Handover", () => {
  vi.spyOn(handoverDocumentationUnitService, "getEventLog").mockImplementation(
    () =>
      Promise.resolve({
        status: 200,
        data: [new HandoverReport()],
      }),
  )
  test("renders successfully", async () => {
    renderComponent()

    expect(
      screen.getByRole("heading", { name: "Überprüfung der Daten ..." }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("heading", { name: "Übergabe an jDV" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("heading", { name: "Letzte Ereignisse" }),
    ).toBeInTheDocument()
  })

  test("renders error", async () => {
    vi.spyOn(
      handoverDocumentationUnitService,
      "getEventLog",
    ).mockImplementation(() =>
      Promise.resolve({
        status: 300,
        error: {
          title: "Leider ist ein Fehler aufgetreten",
          description: "",
        },
      }),
    )
    renderComponent()

    expect(
      screen.getByRole("heading", { name: "Überprüfung der Daten ..." }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("heading", { name: "Übergabe an jDV" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText("Leider ist ein Fehler aufgetreten"),
    ).toBeInTheDocument()
  })

  test.skip("renders handover result", async () => {
    vi.spyOn(
      handoverDocumentationUnitService,
      "getEventLog",
    ).mockImplementation(() =>
      Promise.resolve({
        status: 300,
        data: [new HandoverReport()],
      }),
    )

    vi.spyOn(
      handoverDocumentationUnitService,
      "handoverDocument",
    ).mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: new HandoverMail({ success: true }),
      }),
    )
    vi.spyOn(handoverDocumentationUnitService, "getPreview").mockImplementation(
      () =>
        Promise.resolve({
          status: 200,
          data: new Preview({
            xml: "<xml>all good</xml>",
            success: true,
          }),
        }),
    )
    vi.spyOn(documentUnitService, "getByDocumentNumber").mockImplementation(
      () =>
        Promise.resolve({
          status: 200,
          data: new DocumentUnit("foo", {
            documentNumber: "1234567891234",
            coreData: {
              court: {
                type: "AG",
                location: "Test",
                label: "AG Test",
              },
            },
            shortTexts: {},
            longTexts: {},
            previousDecisions: undefined,
            ensuingDecisions: undefined,
            contentRelatedIndexing: {},
          }),
        }),
    )
    const { user } = renderComponent()

    expect(
      screen.getByRole("heading", { name: "Überprüfung der Daten ..." }),
    ).toBeInTheDocument()
    expect(
      await screen.findByLabelText("Dokumentationseinheit an jDV übergeben"),
    ).toBeInTheDocument()

    await user.click(
      screen.getByLabelText("Dokumentationseinheit an jDV übergeben"),
    )

    expect(
      await screen.findByLabelText("Erfolg der jDV Übergabe"),
    ).toBeInTheDocument()
  })
})

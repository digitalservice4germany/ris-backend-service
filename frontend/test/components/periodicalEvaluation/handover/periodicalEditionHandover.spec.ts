import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen, waitFor } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import PeriodicalEditionHandover from "@/components/periodical-evaluation/handover/PeriodicalEditionHandover.vue"
import {
  EventRecordType,
  HandoverMail,
  HandoverReport,
  Preview,
} from "@/domain/eventRecord"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import Reference from "@/domain/reference"
import featureToggleService from "@/services/featureToggleService"
import handoverEditionService from "@/services/handoverEditionService"
import routes from "~/test-helper/routes"

// Define a default edition for most tests
const defaultEdition = new LegalPeriodicalEdition({
  id: "123",
  legalPeriodical: {
    uuid: "456",
    title: "foo",
  },
  references: [new Reference({ id: "789", citation: "AB, 3" })],
})

function renderComponent(storeState = {}) {
  const user = userEvent.setup()
  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })

  return {
    user,
    ...render(PeriodicalEditionHandover, {
      global: {
        plugins: [
          createTestingPinia({
            initialState: {
              editionStore: {
                edition: defaultEdition,
                ...storeState,
              },
            },
          }),
          [router],
        ],
      },
    }),
  }
}
describe("Edition Handover", () => {
  beforeEach(() => {
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })

    vi.spyOn(handoverEditionService, "getPreview").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: [
          new Preview({
            xml: "<xml>all good</xml>",
            success: true,
          }),
        ],
      }),
    )
    vi.spyOn(handoverEditionService, "getEventLog").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: [
          new HandoverReport({
            date: "2024-10-11T07:19:10.049027Z",
            content:
              '<?xml version="1.0"?>\n<!DOCTYPE juris-r SYSTEM "juris-r.dtd">\n<xml>content</xml>',
          }),
        ],
      }),
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  test("if edition not loaded in store, loading screen renders", async () => {
    renderComponent({ edition: null })

    expect(
      screen.getByRole("heading", { name: "Ausgabe wird geladen ..." }),
    ).toBeInTheDocument()
  })

  test("renders with loaded edition", async () => {
    renderComponent()
    expect(
      await screen.findByRole("heading", { name: "Übergabe an jDV" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("heading", { name: "Letzte Ereignisse" }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
      `Letzte Ereignisse Diese Ausgabe wurde bisher nicht an die jDV übergeben`,
    )
  })

  test("gets eventlog successfully on initial load", async () => {
    renderComponent()

    await waitFor(() => {
      expect(featureToggleService.isEnabled).toHaveBeenCalledWith(
        "neuris.evaluation-handover",
      )
    })

    await waitFor(() => {
      expect(handoverEditionService.getEventLog).toHaveBeenCalledTimes(1)
    })
  })

  test("gets eventlog with error on initial load", async () => {
    vi.spyOn(handoverEditionService, "getEventLog").mockImplementation(() =>
      Promise.resolve({
        status: 300,
        error: {
          title: "Leider ist ein Fehler aufgetreten",
          description: "",
        },
      }),
    )
    renderComponent()

    await waitFor(() => {
      expect(featureToggleService.isEnabled).toHaveBeenCalledWith(
        "neuris.evaluation-handover",
      )
    })

    await waitFor(() => {
      expect(handoverEditionService.getEventLog).toHaveBeenCalledTimes(1)
    })

    expect(
      await screen.findByText("Leider ist ein Fehler aufgetreten"),
    ).toBeInTheDocument()
  })

  test("gets preview successfully on initial load", async () => {
    renderComponent()

    await waitFor(() => {
      expect(featureToggleService.isEnabled).toHaveBeenCalledWith(
        "neuris.evaluation-handover",
      )
    })

    await waitFor(() => {
      expect(handoverEditionService.getPreview).toHaveBeenCalledTimes(1)
    })
  })

  test("gets preview with error on initial load", async () => {
    vi.spyOn(handoverEditionService, "getPreview").mockImplementation(() =>
      Promise.resolve({
        status: 300,
        error: {
          title: "Fehler beim Laden der XML Vorschau",
          description: "",
        },
      }),
    )
    renderComponent()

    await waitFor(() => {
      expect(featureToggleService.isEnabled).toHaveBeenCalledWith(
        "neuris.evaluation-handover",
      )
    })

    expect(
      await screen.findByText("Fehler beim Laden der XML Vorschau"),
    ).toBeInTheDocument()
  })

  test("on press 'Fundstellen der Ausgabe an jDV übergeben' hands over successfully", async () => {
    vi.spyOn(handoverEditionService, "handoverEdition").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: new HandoverMail({
          type: EventRecordType.HANDOVER,
          attachments: [
            {
              fileContent:
                '<?xml version="1.0"?>\n<!DOCTYPE juris-r SYSTEM "juris-r.dtd">\n<xml>content</xml>',
              fileName: "file.xml",
            },
          ],
          statusMessages: ["success"],
          success: true,
          issuerAddress: "issuer address",
          receiverAddress: "receiver address",
          mailSubject: "mail subject",
          date: "2024-10-11T07:19:10.049027Z",
        }),
      }),
    )
    const { user } = renderComponent()
    const handoverButton = await screen.findByRole("button", {
      name: "Fundstellen der Ausgabe an jDV übergeben",
    })
    await user.click(handoverButton)

    expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
      `Letzte EreignisseXml Email Abgabe - 11.10.2024 um 07:19 UhrÜBERE-Mail an: receiver address Betreff: mail subjectALSfile.xml1<?xml version="1.0"?>2<!DOCTYPE juris-r SYSTEM "juris-r.dtd">3<xml>content</xml>Juris Protokoll - 11.10.2024 um 07:19 Uhr`,
    )
  })

  test("on press 'Fundstellen der Ausgabe an jDV übergeben' hands over with error", async () => {
    vi.spyOn(handoverEditionService, "handoverEdition").mockImplementation(() =>
      Promise.resolve({
        status: 300,
        error: {
          title: "Leider ist ein Fehler aufgetreten",
        },
      }),
    )
    const { user } = renderComponent()

    const handoverButton = await screen.findByRole("button", {
      name: "Fundstellen der Ausgabe an jDV übergeben",
    })
    await user.click(handoverButton)
    await waitFor(() => {
      expect(handoverEditionService.handoverEdition).toHaveBeenCalledTimes(1)
    })
    expect(
      screen.queryByLabelText("Erfolg der jDV Übergabe"),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText("Fehler bei jDV Übergabe")).toHaveTextContent(
      `Leider ist ein Fehler aufgetreten`,
    )
  })
})

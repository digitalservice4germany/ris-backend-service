import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, fireEvent, screen, waitFor } from "@testing-library/vue"
import { Stubs } from "@vue/test-utils/dist/types"
import { createRouter, createWebHistory } from "vue-router"
import HandoverEditionView from "@/components/HandoverEditionView.vue"
import { EventRecordType } from "@/domain/eventRecord"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import Reference from "@/domain/reference"
import featureToggleService from "@/services/featureToggleService"
import handoverService from "@/services/handoverEditionService"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/caselaw/periodical-evaluation/:editionId/handover",
      name: "caselaw-periodical-evaluation-editionId-handover",
      component: {},
    },
    {
      path: "/",
      name: "caselaw",
      component: {},
    },
  ],
})

function renderComponent(
  options: {
    props?: unknown
    stubs?: Stubs
    edition?: LegalPeriodicalEdition
  } = {},
) {
  const user = userEvent.setup()

  return {
    user,
    ...render(HandoverEditionView, {
      props: options.props ?? {},
      global: {
        plugins: [
          [router],
          [
            createTestingPinia({
              initialState: {
                editionStore: {
                  edition:
                    options.edition ??
                    new LegalPeriodicalEdition({
                      id: "123",
                      legalPeriodical: { uuid: "456", title: "foo" },
                      references: [
                        new Reference({ id: "789", citation: "AB, 3" }),
                      ],
                    }),
                },
              },
            }),
          ],
        ],
        stubs: options.stubs ?? undefined,
      },
    }),
  }
}

describe("HandoverEditionView:", () => {
  beforeEach(() => {
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  vi.spyOn(handoverService, "getPreview").mockImplementation(() =>
    Promise.resolve({
      status: 200,
      data: [
        {
          xml: "<xml>all good</xml>",
          success: true,
        },
      ],
    }),
  )

  describe("renders preview", () => {
    it("render preview error", async () => {
      renderComponent({
        props: {
          errorMessage: {
            title: "preview error",
            description: "error message description",
          },
        },
      })
      expect(await screen.findByText("preview error")).toBeInTheDocument()
    })

    describe("on press 'Fundstellen der Ausgabe an jDV übergeben'", () => {
      it("hands over successfully", async () => {
        const { emitted } = renderComponent()
        const handoverButton = await screen.findByRole("button", {
          name: "Fundstellen der Ausgabe an jDV übergeben",
        })
        await fireEvent.click(handoverButton)

        expect(emitted().handoverEdition).toBeTruthy()
      })

      it("renders error modal from backend", async () => {
        renderComponent({
          props: {
            handoverResult: {
              xml: "xml",
              statusMessages: ["error message 1", "error message 2"],
              success: false,
              receiverAddress: "receiver address",
              mailSubject: "mail subject",
              date: undefined,
            },
            errorMessage: {
              title: "error message title",
              description: "error message description",
            },
          },
        })

        expect(
          screen.queryByLabelText("Erfolg der jDV Übergabe"),
        ).not.toBeInTheDocument()
        expect(
          screen.getByLabelText("Fehler bei jDV Übergabe"),
        ).toHaveTextContent(`error message titleerror message description`)
      })

      it("renders error modal from frontend", async () => {
        renderComponent({ edition: new LegalPeriodicalEdition() })

        const handoverButton = await screen.findByRole("button", {
          name: "Fundstellen der Ausgabe an jDV übergeben",
        })
        await fireEvent.click(handoverButton)

        expect(
          screen.queryByLabelText("Erfolg der jDV Übergabe"),
        ).not.toBeInTheDocument()
        expect(
          screen.getByLabelText("Fehler bei jDV Übergabe"),
        ).toHaveTextContent(
          `Es sind noch keine Fundstellen vermerkt.Die Ausgabe kann nicht übergeben werden.`,
        )
      })
    })

    describe("last handed over xml", () => {
      it("with earlier handed over document unit", async () => {
        renderComponent({
          props: {
            eventLog: [
              {
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
                receiverAddress: "receiver address",
                mailSubject: "mail subject",
                date: "01.02.2000",
              },
            ],
          },
        })
        // TODO multiple
        expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
          `Letzte EreignisseXml Email Abgabe - 01.02.2000ÜBERE-Mail an: receiver address Betreff: mail subjectALSfile.xml1<?xml version="1.0"?>2<!DOCTYPE juris-r SYSTEM "juris-r.dtd">3<xml>content</xml>`,
        )
      })

      it("without earlier handed over document unit", async () => {
        renderComponent()
        expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
          `Letzte Ereignisse Diese Ausgabe wurde bisher nicht an die jDV übergeben`,
        )
      })
    })

    it("with stubbing", async () => {
      const { container } = renderComponent({
        props: {
          eventLog: [
            {
              type: EventRecordType.HANDOVER,
              attachments: [
                { fileContent: "xml content", fileName: "file.xml" },
              ],
              statusMessages: ["success"],
              success: true,
              receiverAddress: "receiver address",
              mailSubject: "mail subject",
              date: "01.02.2000",
            },
          ],
        },
        stubs: {
          CodeSnippet: {
            template: '<div data-testid="code-snippet"/>',
          },
        },
      })

      await waitFor(() => {
        expect(container).toHaveTextContent(
          `Übergabe an jDVPlausibilitätsprüfungAlle Pflichtfelder sind korrekt ausgefülltXML VorschauFundstellen der Ausgabe an jDV übergebenLetzte EreignisseXml Email Abgabe - 01.02.2000ÜBERE-Mail an: receiver address Betreff: mail subjectALS`,
        )
      })

      const codeSnippet = screen.queryByTestId("code-snippet")

      expect(codeSnippet).toBeInTheDocument()
      expect(codeSnippet?.title).toBe("file.xml")
      expect(codeSnippet).toHaveAttribute("XML")
      expect(codeSnippet?.getAttribute("xml")).toBe("xml content")
    })
  })
})

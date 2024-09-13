import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, fireEvent, screen } from "@testing-library/vue"
import { Stubs } from "@vue/test-utils/dist/types"
import { createRouter, createWebHistory } from "vue-router"
import HandoverView from "@/components/HandoverView.vue"
import DocumentUnit from "@/domain/documentUnit"
import { EventRecordType } from "@/domain/eventRecord"
import LegalForce from "@/domain/legalForce"
import NormReference from "@/domain/normReference"
import SingleNorm from "@/domain/singleNorm"
import handoverService from "@/services/handoverService"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/caselaw/documentUnit/:documentNumber/categories",
      name: "caselaw-documentUnit-documentNumber-categories",
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
    documentUnit?: DocumentUnit
    stubs?: Stubs
  } = {},
) {
  const user = userEvent.setup()

  return {
    user,
    ...render(HandoverView, {
      props: options.props ?? {},
      global: {
        plugins: [
          [router],
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit:
                    options.documentUnit ??
                    new DocumentUnit("123", {
                      documentNumber: "foo",
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

describe("HandoverView:", () => {
  vi.spyOn(handoverService, "getPreview").mockImplementation(() =>
    Promise.resolve({
      status: 200,
      data: {
        xml: "<xml>all good</xml>",
        success: true,
      },
    }),
  )

  describe("renders plausibility check", () => {
    it("with all required fields filled", async () => {
      renderComponent({
        documentUnit: new DocumentUnit("123", {
          documentNumber: "foo",
          coreData: {
            fileNumbers: ["foo"],
            court: {
              type: "type",
              location: "location",
              label: "label",
            },
            decisionDate: "2022-02-01",
            legalEffect: "legalEffect",
            documentType: {
              jurisShortcut: "ca",
              label: "category",
            },
          },
        }),
      })

      expect(
        screen.getByText("Alle Pflichtfelder sind korrekt ausgefüllt"),
      ).toBeInTheDocument()
      expect(
        screen.queryByText(
          "Die folgenden Rubriken-Pflichtfelder sind nicht befüllt:",
        ),
      ).not.toBeInTheDocument()
      expect(await screen.findByText("XML Vorschau")).toBeInTheDocument()
    })

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

    it("with required fields missing", async () => {
      renderComponent({
        documentUnit: new DocumentUnit("123", {
          documentNumber: "foo",
          contentRelatedIndexing: {
            norms: [
              new NormReference({
                normAbbreviationRawValue: "ABC",
                singleNorms: [
                  new SingleNorm({
                    singleNorm: "§ 1",
                    legalForce: new LegalForce(),
                  }),
                ],
              }),
            ],
          },
        }),
      })
      expect(
        await screen.findByText(
          "Die folgenden Rubriken-Pflichtfelder sind nicht befüllt:",
        ),
      ).toBeInTheDocument()

      expect(screen.getByText("Aktenzeichen")).toBeInTheDocument()
      expect(screen.getByText("Gericht")).toBeInTheDocument()
      expect(screen.getByText("Entscheidungsdatum")).toBeInTheDocument()
      expect(screen.getByText("Rechtskraft")).toBeInTheDocument()
      expect(screen.getByText("Dokumenttyp")).toBeInTheDocument()
      expect(screen.getByText("Normen")).toBeInTheDocument()
      expect(screen.getByText("ABC")).toBeInTheDocument()
      expect(screen.getByText("Gesetzeskraft")).toBeInTheDocument()

      expect(screen.queryByText("XML Vorschau")).not.toBeInTheDocument()
    })

    it("should show error message with invalid outline", async () => {
      renderComponent({
        documentUnit: new DocumentUnit("123", {
          documentNumber: "foo",
          coreData: {
            fileNumbers: ["foo"],
            court: {
              type: "type",
              location: "location",
              label: "label",
            },
            decisionDate: "2022-02-01",
            legalEffect: "legalEffect",
            documentType: {
              jurisShortcut: "ca",
              label: "category",
            },
          },
          texts: {
            outline: "Outline",
            otherHeadnote: "Other Headnote",
          },
        }),
      })
      expect(
        await screen.findByText(
          'Die Rubriken "Gliederung" und "Sonstiger Orientierungssatz" sind befüllt. Es darf nur eine der beiden Rubriken befüllt sein.',
        ),
      ).toBeInTheDocument()

      expect(
        await screen.findByLabelText("Rubriken bearbeiten"),
      ).toBeInTheDocument()
      expect(screen.queryByText("XML Vorschau")).not.toBeInTheDocument()
    })

    it("'Rubriken bearbeiten' button links back to categories", async () => {
      render(HandoverView, {
        global: {
          plugins: [router],
        },
      })
      expect(
        await screen.findByLabelText("Rubriken bearbeiten"),
      ).toBeInTheDocument()

      await userEvent.click(screen.getByLabelText("Rubriken bearbeiten"))
      expect(router.currentRoute.value.name).toBe(
        "caselaw-documentUnit-documentNumber-categories",
      )
    })
  })

  describe("on press 'Dokumentationseinheit an jDV übergeben'", () => {
    it("hands over successfully", async () => {
      const { emitted } = renderComponent({
        documentUnit: new DocumentUnit("123", {
          documentNumber: "foo",
          coreData: {
            fileNumbers: ["foo"],
            court: {
              type: "type",
              location: "location",
              label: "label",
            },
            decisionDate: "2022-02-01",
            legalEffect: "legalEffect",
            documentType: {
              jurisShortcut: "ca",
              label: "category",
            },
          },
        }),
      })
      const handoverButton = screen.getByRole("button", {
        name: "Dokumentationseinheit an jDV übergeben",
      })
      await fireEvent.click(handoverButton)

      expect(emitted().handoverDocument).toBeTruthy()
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
      renderComponent()

      const handoverButton = screen.getByRole("button", {
        name: "Dokumentationseinheit an jDV übergeben",
      })
      await fireEvent.click(handoverButton)

      expect(
        screen.queryByLabelText("Erfolg der jDV Übergabe"),
      ).not.toBeInTheDocument()
      expect(
        screen.getByLabelText("Fehler bei jDV Übergabe"),
      ).toHaveTextContent(
        `Es sind noch nicht alle Pflichtfelder befüllt.Die Dokumentationseinheit kann nicht übergeben werden.`,
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
      expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
        `Letzte EreignisseXml Email Abgabe - 01.02.2000ÜBERE-Mail an: receiver address Betreff: mail subjectALSXML1<?xml version="1.0"?>2<!DOCTYPE juris-r SYSTEM "juris-r.dtd">3<xml>content</xml>`,
      )
    })

    it("without earlier handed over document unit", async () => {
      renderComponent()
      expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
        `Letzte Ereignisse Diese Dokumentationseinheit wurde bisher nicht an die jDV übergeben`,
      )
    })
  })

  it("with preview stubbing", async () => {
    renderComponent({
      documentUnit: new DocumentUnit("123", {
        coreData: {
          fileNumbers: ["foo"],
          court: { type: "type", location: "location", label: "label" },
          decisionDate: "2022-02-01",
          legalEffect: "legalEffect",
          documentType: {
            jurisShortcut: "ca",
            label: "category",
          },
        },
      }),
      stubs: {
        CodeSnippet: {
          template: '<div data-testid="code-snippet"/>',
        },
      },
    })

    expect(await screen.findByText("XML Vorschau")).toBeInTheDocument()

    await fireEvent.click(screen.getByText("XML Vorschau"))
    const codeSnippet = screen.queryByTestId("code-snippet")

    expect(codeSnippet).toBeInTheDocument()
    expect(codeSnippet).toHaveAttribute("XML")
    expect(codeSnippet?.getAttribute("xml")).toBe("<xml>all good</xml>")
  })

  it("with stubbing", () => {
    const { container } = renderComponent({
      props: {
        eventLog: [
          {
            type: EventRecordType.HANDOVER,
            attachments: [{ fileContent: "xml content", fileName: "file.xml" }],
            statusMessages: ["success"],
            success: true,
            receiverAddress: "receiver address",
            mailSubject: "mail subject",
            date: "01.02.2000",
          },
        ],
      },
      documentUnit: new DocumentUnit("123", {
        coreData: {
          fileNumbers: ["foo"],
          court: { type: "type", location: "location", label: "label" },
          decisionDate: "2022-02-01",
          legalEffect: "legalEffect",
          documentType: {
            jurisShortcut: "ca",
            label: "category",
          },
        },
      }),
      stubs: {
        CodeSnippet: {
          template: '<div data-testid="code-snippet"/>',
        },
      },
    })

    expect(container).toHaveTextContent(
      `Übergabe an jDVPlausibilitätsprüfungAlle Pflichtfelder sind korrekt ausgefülltDokumentationseinheit an jDV übergebenLetzte EreignisseXml Email Abgabe - 01.02.2000ÜBERE-Mail an: receiver address Betreff: mail subjectALS`,
    )

    const codeSnippet = screen.queryByTestId("code-snippet")

    expect(codeSnippet).toBeInTheDocument()
    expect(codeSnippet?.title).toBe("XML")
    expect(codeSnippet).toHaveAttribute("XML")
    expect(codeSnippet?.getAttribute("xml")).toBe("xml content")
  })
})

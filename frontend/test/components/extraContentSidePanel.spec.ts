import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { describe } from "vitest"
import { createRouter, createWebHistory, Router } from "vue-router"
import ExtraContentSidePanel from "@/components/ExtraContentSidePanel.vue"
import Attachment from "@/domain/attachment"
import DocumentUnit from "@/domain/documentUnit"
import Reference from "@/domain/reference"

let router: Router

function renderComponent(
  options: {
    note?: string
    attachments?: Attachment[]
    references?: Reference[]
  } = {},
) {
  const user = userEvent.setup()
  return {
    user,
    ...render(ExtraContentSidePanel, {
      global: {
        plugins: [
          [router],
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit: new DocumentUnit("foo", {
                    documentNumber: "1234567891234",
                    note: options.note ?? "",
                    attachments: options.attachments ?? [],
                  }),
                },
              },
            }),
          ],
        ],
      },
    }),
  }
}

function mockAttachment(name: string = ""): Attachment {
  return {
    name: name,
    format: "",
    s3path: "123",
    uploadTimestamp: "",
  }
}

describe("ExtraContentSidePanel", () => {
  beforeEach(() => {
    router = createRouter({
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
            template: "<div data-testid='references'>References</div>",
          },
        },
        {
          path: "/caselaw/documentUnit/:documentNumber/handover",
          name: "caselaw-documentUnit-documentNumber-handover",
          component: {},
        },
        {
          path: "/caselaw/documentUnit/:documentNumber/preview",
          name: "caselaw-documentUnit-documentNumber-preview",
          component: {},
        },
      ],
    })
  })

  describe("Open/close the panel", () => {
    const testCases = [
      {
        hasNote: false,
        hasAttachment: false,
        queryParam: undefined,
        expectedIsOpen: false,
      },
      {
        hasNote: false,
        hasAttachment: true,
        queryParam: undefined,
        expectedIsOpen: true,
      },
      {
        hasNote: true,
        hasAttachment: false,
        queryParam: undefined,
        expectedIsOpen: true,
      },
      {
        hasNote: false,
        hasAttachment: false,
        queryParam: "true",
        expectedIsOpen: true,
      },
      {
        hasNote: true,
        hasAttachment: false,
        queryParam: "false",
        expectedIsOpen: false,
      },
      {
        hasNote: false,
        hasAttachment: true,
        queryParam: "false",
        expectedIsOpen: false,
      },
      {
        hasNote: true,
        hasAttachment: true,
        queryParam: "false",
        expectedIsOpen: false,
      },
    ]
    testCases.forEach(
      ({ hasNote, hasAttachment, queryParam, expectedIsOpen }) =>
        test(`panel inititally ${expectedIsOpen ? "opened" : "closed"} ${hasNote ? "with" : "without"} note ${hasAttachment ? "with" : "without"} attachment with query param ${queryParam}`, async () => {
          await router.push({
            path: "",
            query: { showAttachmentPanel: queryParam },
          })

          renderComponent({
            note: hasNote ? "note" : "",
            attachments: hasAttachment ? [mockAttachment()] : [],
          })

          await screen.findByLabelText(
            expectedIsOpen ? "Seitenpanel schließen" : "Seitenpanel öffnen",
          )

          if (expectedIsOpen) {
            expect(screen.getByLabelText("Notiz anzeigen")).toBeVisible()
            expect(screen.getByLabelText("Dokumente anzeigen")).toBeVisible()
          } else {
            expect(screen.getByLabelText("Notiz anzeigen")).not.toBeVisible()
            expect(
              screen.getByLabelText("Dokumente anzeigen"),
            ).not.toBeVisible()
          }
        }),
    )
  })

  test("toggle panel open and closed", async () => {
    renderComponent()

    expect(await screen.findByLabelText("Seitenpanel öffnen")).toBeVisible()
    screen.getByLabelText("Seitenpanel öffnen").click()
    expect(await screen.findByLabelText("Seitenpanel schließen")).toBeVisible()
    screen.getByLabelText("Seitenpanel schließen").click()
    expect(await screen.findByLabelText("Seitenpanel öffnen")).toBeVisible()
  })

  describe("Select panel content", () => {
    test("initially open note without note and no attachments", async () => {
      renderComponent()
      screen.getByLabelText("Seitenpanel öffnen").click()

      expect(await screen.findByLabelText("Notiz Eingabefeld")).toBeVisible()
    })

    test("initially open note with note and no attachments", async () => {
      renderComponent({ note: "some note" })

      expect(await screen.findByDisplayValue("some note")).toBeVisible()
    })

    test("initially open attachments without note and with attachment", async () => {
      renderComponent({ attachments: [mockAttachment("foo.docx")] })

      await screen.findByLabelText("Seitenpanel schließen")
      expect(screen.queryByLabelText("Notiz")).not.toBeInTheDocument()
    })

    test("initially open attachments without note and with multiple attachments", async () => {
      renderComponent({
        attachments: [mockAttachment("foo.docx"), mockAttachment("bar.docx")],
      })

      expect(await screen.findByText("foo.docx")).toBeVisible()
    })

    test("initially open note with note and with attachments", async () => {
      renderComponent({
        note: "some note",
        attachments: [mockAttachment("foo.docx")],
      })

      expect(await screen.findByDisplayValue("some note")).toBeVisible()
    })

    test("cycle through multiple attachments", async () => {
      renderComponent({
        attachments: [mockAttachment("foo.docx"), mockAttachment("bar.docx")],
      })

      expect(await screen.findByText("foo.docx")).toBeVisible()
      screen.getByLabelText("Nächstes Dokument anzeigen").click()
      expect(await screen.findByText("bar.docx")).toBeVisible()
      screen.getByLabelText("Nächstes Dokument anzeigen").click()
      expect(await screen.findByText("foo.docx")).toBeVisible()
    })

    test("switch between note and attachments without attachment", async () => {
      renderComponent({
        note: "some note",
        attachments: [],
      })

      expect(await screen.findByDisplayValue("some note")).toBeVisible()
      screen.getByLabelText("Dokumente anzeigen").click()
      expect(
        await screen.findByText(
          "Wenn eine Datei hochgeladen ist, können Sie die Datei hier sehen.",
        ),
      ).toBeVisible()

      screen.getByLabelText("Vorschau anzeigen").click()
      expect(await screen.findByTestId("preview")).toBeVisible()
      expect(
        await screen.findByLabelText("Vorschau in neuem Tab öffnen"),
      ).toBeVisible()

      screen.getByLabelText("Notiz anzeigen").click()
      expect(await screen.findByDisplayValue("some note")).toBeVisible()
    })
  })
})

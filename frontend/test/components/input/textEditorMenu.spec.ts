/* eslint-disable testing-library/no-node-access */
import { userEvent } from "@testing-library/user-event"
import { render, screen, waitFor } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import TextEditor from "@/components/input/TextEditor.vue"
import { mockDocumentForProsemirror } from "~/test-helper/prosemirror-document-mock"

mockDocumentForProsemirror()
vi.mock("@/composables/useInternalUser", () => {
  return {
    useInternalUser: () => true,
  }
})

describe("text editor toolbar", async () => {
  const renderComponent = async () => {
    userEvent.setup()
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Gründe",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()
  }

  // eslint-disable-next-line @typescript-eslint/no-require-imports
  global.ResizeObserver = require("resize-observer-polyfill")
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: "/",
        name: "home",
        component: {},
      },
      {
        path: "/caselaw/documentUnit/:documentNumber/categories#coreData",
        name: "caselaw-documentUnit-documentNumber-categories#coreData",
        component: {},
      },
    ],
  })

  describe("keyboard navigation", () => {
    test("shift tab in text editor should focus first button in menu", async () => {
      await renderComponent()
      const editorField = screen.getByTestId("Gründe")

      await userEvent.click(editorField.firstElementChild!)
      expect(editorField.firstElementChild).toHaveFocus()
      await userEvent.tab({ shift: true })
      const firstButton = screen.getByLabelText("Erweitern")
      expect(firstButton).toHaveFocus()
    })

    test("arrow right should move focus to next button until last button", async () => {
      await renderComponent()
      const editorField = screen.getByTestId("Gründe")

      await userEvent.click(editorField.firstElementChild!)
      expect(editorField.firstElementChild).toHaveFocus()
      await userEvent.tab({ shift: true })
      const firstButton = screen.getByLabelText("Erweitern")
      expect(firstButton).toHaveFocus()

      await userEvent.keyboard("{ArrowRight}")
      const secondButton = screen.getByLabelText("Nicht-druckbare Zeichen")
      expect(secondButton).toHaveFocus()

      // navigate to last button (arrow right 25 times)
      await userEvent.keyboard("{ArrowRight>25/}")
      const lastButton = screen.getByLabelText("Wiederherstellen")
      expect(lastButton).toHaveFocus()

      // go one step further to the right --> should do nothing
      await userEvent.keyboard("{ArrowRight}")
      expect(lastButton).toHaveFocus()

      // navigate to the left --> should go to previous button
      await userEvent.keyboard("{ArrowLeft}")
      const secondLastButton = screen.getByLabelText("Rückgängig machen")
      expect(secondLastButton).toHaveFocus()
    })

    test("arrow left should leave focus on first button", async () => {
      await renderComponent()
      const editorField = screen.getByTestId("Gründe")

      await userEvent.click(editorField.firstElementChild!)
      expect(editorField.firstElementChild).toHaveFocus()
      await userEvent.tab({ shift: true })
      const firstButton = screen.getByLabelText("Erweitern")
      expect(firstButton).toHaveFocus()

      // When the first button is focused, ArrowLeft does not move focus
      await userEvent.keyboard("{ArrowLeft>5}")
      expect(firstButton).toHaveFocus()

      // From the first button you can move immediately to the next one
      await userEvent.keyboard("{ArrowRight}")
      const secondButton = screen.getByLabelText("Nicht-druckbare Zeichen")
      expect(secondButton).toHaveFocus()

      await userEvent.keyboard("{ArrowLeft}")
      expect(firstButton).toHaveFocus()
    })

    test("enter should jump back to the editor input", async () => {
      await renderComponent()
      const editorField = screen.getByTestId("Gründe")

      await userEvent.click(editorField.firstElementChild!)
      await userEvent.tab({ shift: true })
      await userEvent.keyboard("{ArrowRight}")
      await userEvent.keyboard("{ArrowRight}")
      const thirdButton = screen.getByLabelText("Fett")
      expect(thirdButton).toHaveFocus()

      // When clicking enter on a text edit button, the focus moves to the editor
      await userEvent.keyboard("{Enter}")
      await waitFor(() => expect(editorField.firstElementChild).toHaveFocus(), {
        timeout: 100,
      })
    })

    test("tab into the editor should skip the menu tool bar", async () => {
      await renderComponent()
      const editorField = screen.getByTestId("Gründe")

      // Add external input field to be focused first
      const inputField = editorField.ownerDocument.createElement("input")
      editorField.ownerDocument.body.prepend(inputField)
      await userEvent.click(inputField)

      // Tab skips the menu toolbar and focuses the editor content directly
      await userEvent.tab()
      expect(editorField.firstElementChild).toHaveFocus()

      // ProseMirror needs to be focused explicitly as it does not trigger button enabling otherwise in the unit test
      await userEvent.click(editorField.firstElementChild!)

      // Tab back focuses the toolbar buttons
      await userEvent.tab({ shift: true })
      const firstButton = screen.getByLabelText("Erweitern")
      expect(firstButton).toHaveFocus()
    })
  })
})

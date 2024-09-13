/* eslint-disable testing-library/no-node-access */
import { render, screen, fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import TextEditor from "@/components/input/TextEditor.vue"

describe("text editor", async () => {
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

  test("renders text editor with default props", async () => {
    render(TextEditor, {
      props: {},
      global: { plugins: [router] },
    })

    expect(screen.getAllByTestId("Editor Feld").length).toBe(1)
  })

  test("renders text editor with props", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Test Editor Feld",
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    expect(screen.getByText("Test Value")).toBeInTheDocument()
    expect(screen.getByTestId("Test Editor Feld")).toBeInTheDocument()
  })

  test.each([
    ["max", "h-full"],
    ["big", "h-320"],
    ["medium", "h-160"],
    ["small", "h-96"],
    [undefined, "h-160"],
  ] as const)("renders %s field with correct class", async (a, expected) => {
    render(TextEditor, {
      props: { fieldSize: a },
      global: { plugins: [router] },
    })

    expect(await screen.findByTestId("Editor Feld")).toHaveClass(expected)
  })

  test("enable buttons on focus", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Test Editor Feld",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Test Editor Feld")

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(
      screen.getByLabelText("Test Editor Feld Button Leiste"),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("fullview")).toBeEnabled()
    expect(screen.getByLabelText("undo")).toBeEnabled()
    expect(screen.getByLabelText("redo")).toBeEnabled()
  })

  test("disable buttons on blur", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Test Editor Feld",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Test Editor Feld")

    if (editorField.firstElementChild !== null) {
      await fireEvent.blur(editorField.firstElementChild)
    }

    expect(
      screen.getByLabelText("Test Editor Feld Button Leiste"),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("fullview")).toBeDisabled()
    expect(screen.getByLabelText("undo")).toBeDisabled()
    expect(screen.getByLabelText("redo")).toBeDisabled()
  })

  /*
   * The purpose of this test is to ensure that all expected buttons are
   * rendered. Having this test helps us to ensure that we do not accidentally
   * remove any of them.
   * Unfortunately is the logic of the button bar very complex and dependents on
   * the current width of the editor element. Thereby it is very hard, or rather
   * impossible to test this end-to-end as it depends on the surrounding layout.
   * Thereby this is rather an integration test, as the button list is
   * a configuration of the component. The logic of the button bar and how it
   * collapses for certain widths, is a logic for itself that gets tested
   * separetly.
   * The test should be continuosly improved to very that all buttons exist.
   */
  it("shows all necessary editor buttons", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Test Editor Feld",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Test Editor Feld")

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(screen.getByLabelText("fullview")).toBeInTheDocument()
    expect(screen.getByLabelText("invisible-characters")).toBeInTheDocument()
    expect(screen.getByLabelText("bold")).toBeInTheDocument()
    expect(screen.getByLabelText("italic")).toBeInTheDocument()
    expect(screen.getByLabelText("underline")).toBeInTheDocument()
    expect(screen.getByLabelText("strike")).toBeInTheDocument()
    expect(screen.getByLabelText("superscript")).toBeInTheDocument()
    expect(screen.getByLabelText("subscript")).toBeInTheDocument()
    expect(screen.getByLabelText("left")).toBeInTheDocument()
    expect(screen.getByLabelText("center")).toBeInTheDocument()
    expect(screen.getByLabelText("right")).toBeInTheDocument()
    expect(screen.getByLabelText("bulletList")).toBeInTheDocument()
    expect(screen.getByLabelText("orderedList")).toBeInTheDocument()
    expect(screen.getByLabelText("outdent")).toBeInTheDocument()
    expect(screen.getByLabelText("indent")).toBeInTheDocument()
    expect(screen.getByLabelText("blockquote")).toBeInTheDocument()
    expect(screen.getByLabelText("deleteBorderNumber")).toBeInTheDocument()
    expect(screen.getByLabelText("undo")).toBeInTheDocument()
    expect(screen.getByLabelText("redo")).toBeInTheDocument()
  })
})

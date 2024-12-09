/* eslint-disable testing-library/no-node-access */
import { render, screen, fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import TextEditor from "@/components/input/TextEditor.vue"
import { longTextLabels } from "@/domain/documentUnit"

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
        ariaLabel: "Gründe",
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    expect(screen.getByText("Test Value")).toBeInTheDocument()
    expect(screen.getByTestId("Gründe")).toBeInTheDocument()
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
        ariaLabel: "Gründe",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Gründe")

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(screen.getByLabelText("Gründe Button Leiste")).toBeInTheDocument()
    expect(screen.getByLabelText("Erweitern")).toBeEnabled()
    expect(screen.getByLabelText("Rückgängig machen")).toBeEnabled()
    expect(screen.getByLabelText("Wiederherstellen")).toBeEnabled()
  })

  test("disable buttons on blur", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Gründe",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Gründe")

    if (editorField.firstElementChild !== null) {
      await fireEvent.blur(editorField.firstElementChild)
    }

    expect(screen.getByLabelText("Gründe Button Leiste")).toBeInTheDocument()
    expect(screen.getByLabelText("Erweitern")).toBeDisabled()
    expect(screen.getByLabelText("Rückgängig machen")).toBeDisabled()
    expect(screen.getByLabelText("Wiederherstellen")).toBeDisabled()
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
        ariaLabel: "Gründe",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Gründe")

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(screen.getByLabelText("Erweitern")).toBeInTheDocument()
    expect(screen.getByLabelText("Nicht-druckbare Zeichen")).toBeInTheDocument()
    expect(screen.getByLabelText("Fett")).toBeInTheDocument()
    expect(screen.getByLabelText("Kursiv")).toBeInTheDocument()
    expect(screen.getByLabelText("Unterstrichen")).toBeInTheDocument()
    expect(screen.getByLabelText("Durchgestrichen")).toBeInTheDocument()
    expect(screen.getByLabelText("Hochgestellt")).toBeInTheDocument()
    expect(screen.getByLabelText("Tiefgestellt")).toBeInTheDocument()
    expect(screen.getByLabelText("Linksbündig")).toBeInTheDocument()
    expect(screen.getByLabelText("Zentriert")).toBeInTheDocument()
    expect(screen.getByLabelText("Rechtsbündig")).toBeInTheDocument()
    expect(screen.getByLabelText("Aufzählungsliste")).toBeInTheDocument()
    expect(screen.getByLabelText("Nummerierte Liste")).toBeInTheDocument()
    expect(screen.getByLabelText("Einzug verringern")).toBeInTheDocument()
    expect(screen.getByLabelText("Einzug vergrößern")).toBeInTheDocument()
    expect(
      screen.getByLabelText("Tabelle", { exact: true }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Zitat einfügen")).toBeInTheDocument()
    expect(
      screen.getByLabelText("Randnummern neu erstellen"),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Randnummern entfernen")).toBeInTheDocument()
    expect(screen.getByLabelText("Rückgängig machen")).toBeInTheDocument()
    expect(screen.getByLabelText("Wiederherstellen")).toBeInTheDocument()
  })

  it("shows all table buttons after menu is expanded", async () => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: "Gründe",
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId("Gründe")

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(
      screen.getByLabelText("Tabelle", { exact: true }),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText("Tabelle einfügen")).not.toBeInTheDocument()
    expect(screen.queryByLabelText("Tabelle löschen")).not.toBeInTheDocument()
    expect(
      screen.queryByLabelText("Spalte rechts einfügen"),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByLabelText("Zeile darunter einfügen"),
    ).not.toBeInTheDocument()
    expect(screen.queryByLabelText("Spalte löschen")).not.toBeInTheDocument()
    expect(screen.queryByLabelText("Zeile löschen")).not.toBeInTheDocument()

    await fireEvent.click(screen.getByLabelText("Tabelle", { exact: true }))

    expect(screen.getByLabelText("Tabelle einfügen")).toBeInTheDocument()
    expect(screen.getByLabelText("Tabelle löschen")).toBeInTheDocument()
    expect(screen.getByLabelText("Spalte rechts einfügen")).toBeInTheDocument()
    expect(screen.getByLabelText("Zeile darunter einfügen")).toBeInTheDocument()
    expect(screen.getByLabelText("Spalte löschen")).toBeInTheDocument()
    expect(screen.getByLabelText("Zeile löschen")).toBeInTheDocument()
  })

  it.each([
    longTextLabels.tenor,
    longTextLabels.participatingJudges,
    longTextLabels.outline,
  ])("hides add border number button for category %s", async (category) => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: category,
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId(category!)

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(
      screen.queryByText("Randnummern neu erstellen"),
    ).not.toBeInTheDocument()
  })

  it.each([
    longTextLabels.reasons,
    longTextLabels.caseFacts,
    longTextLabels.decisionReasons,
    longTextLabels.dissentingOpinion,
    longTextLabels.otherLongText,
  ])("shows add border number button for category %s", async (category) => {
    render(TextEditor, {
      props: {
        value: "Test Value",
        ariaLabel: category,
        editable: true,
      },
      global: { plugins: [router] },
    })

    await flushPromises()

    const editorField = screen.getByTestId(category!)

    if (editorField.firstElementChild !== null) {
      await fireEvent.focus(editorField.firstElementChild)
    }

    expect(
      screen.getByLabelText("Randnummern neu erstellen"),
    ).toBeInTheDocument()
  })
})

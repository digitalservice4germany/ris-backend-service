import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import FieldOfLawExpandableContainer from "@/components/field-of-law/FieldOfLawExpandableContainer.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"

function renderComponent(
  fieldsOfLaw: FieldOfLaw[],
  isResetButtonVisible: boolean = false,
) {
  const user = userEvent.setup()
  return {
    user,
    ...render(FieldOfLawExpandableContainer, {
      props: { fieldsOfLaw, isResetButtonVisible },
    }),
  }
}

function generateFieldOfLaw(): FieldOfLaw {
  return {
    identifier: "AB-01",
    text: "Text for AB links to CD-01",
    linkedFields: ["CD-01"],
    norms: [],
    children: [],
    hasChildren: false,
  }
}

describe("FieldsOfLawExpandableContainer", () => {
  it("renders component with empty sachgebiet list", () => {
    renderComponent([])

    expect(
      screen.getByRole("button", { name: "Sachgebiete" }),
    ).toBeInTheDocument()
  })

  it("renders component with non empty sachgebiet list", () => {
    renderComponent([generateFieldOfLaw()])

    expect(
      screen.getByRole("button", { name: "Weitere Angabe" }),
    ).toBeInTheDocument()
  })

  it("on button click expands and collapses input, emits 'editingDone' on collapse", async () => {
    const { emitted, user } = renderComponent([])
    const scrollIntoViewMock = vi.fn()
    window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
    await user.click(screen.getByRole("button", { name: "Sachgebiete" }))
    expect(screen.getByRole("button", { name: "Fertig" })).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Fertig" }))

    expect(
      screen.getByRole("button", { name: "Sachgebiete" }),
    ).toBeInTheDocument()

    expect(emitted()["editingDone"]).toBeTruthy()
    expect(scrollIntoViewMock).toHaveBeenCalledTimes(1)
  })

  it("emits 'node:clicked'", async () => {
    const { emitted, user } = renderComponent([generateFieldOfLaw()])

    await user.click(
      screen.getByRole("button", {
        name: "AB-01 Text for AB links to CD-01 im Sachgebietsbaum anzeigen",
      }),
    )

    expect(emitted()["node:clicked"]).toBeTruthy()
  })

  it("emits 'node:remove'", async () => {
    const { emitted, user } = renderComponent([generateFieldOfLaw()])

    await user.click(
      screen.getByRole("button", {
        name: "AB-01 Text for AB links to CD-01 aus Liste entfernen",
      }),
    )

    expect(emitted()["node:remove"]).toBeTruthy()
  })

  it("on radio button click emits 'inputMethodSelected'", async () => {
    const { emitted, user } = renderComponent([generateFieldOfLaw()])

    await user.click(screen.getByRole("button", { name: "Weitere Angabe" }))
    await user.click(screen.getByLabelText("Suche"))

    expect(emitted()["inputMethodSelected"]).toBeTruthy()
  })

  it("on radio button label click emits 'inputMethodSelected'", async () => {
    const { emitted, user } = renderComponent([generateFieldOfLaw()])

    await user.click(screen.getByRole("button", { name: "Weitere Angabe" }))
    await user.click(screen.getByLabelText("Sachgebietsuche auswählen"))

    expect(emitted()["inputMethodSelected"]).toBeTruthy()
  })

  it("on reset search button label click emits 'resetSearch'", async () => {
    const { emitted, user } = renderComponent([generateFieldOfLaw()], true)

    await user.click(screen.getByRole("button", { name: "Weitere Angabe" }))
    await user.click(screen.getByLabelText("Sachgebietsuche auswählen"))
    await user.click(screen.getByRole("button", { name: "Suche zurücksetzen" }))

    expect(emitted()["resetSearch"]).toBeTruthy()
  })
})

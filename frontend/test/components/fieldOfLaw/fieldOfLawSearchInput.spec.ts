import { userEvent } from "@testing-library/user-event"
import { fireEvent, render, screen } from "@testing-library/vue"
import FieldOfLawSearchInput from "@/components/field-of-law/FieldOfLawSearchInput.vue"

function renderComponent(
  identifier?: string,
  description?: string,
  norm?: string,
) {
  const props = {
    identifier,
    description,
    norm,
  }

  const user = userEvent.setup()

  return { user, ...render(FieldOfLawSearchInput, { props }) }
}

describe("FieldOfLawSearchInput", () => {
  it("render input fields", () => {
    renderComponent(undefined, undefined, undefined)

    expect(screen.getByText("Sachgebiet")).toBeInTheDocument()
    expect(screen.getByText("Bezeichnung")).toBeInTheDocument()
    expect(screen.getByText("Norm")).toBeInTheDocument()
  })

  it("render input fields with given text", () => {
    renderComponent("AR-01-01", "Kurzarbeit", "BGB")

    expect(screen.getByText("Sachgebiet")).toBeInTheDocument()
    expect(screen.getByText("Bezeichnung")).toBeInTheDocument()
    expect(screen.getByText("Norm")).toBeInTheDocument()

    expect(screen.getByLabelText("Sachgebietskürzel")).toHaveValue("AR-01-01")
    expect(screen.getByLabelText("Sachgebietsbezeichnung")).toHaveValue(
      "Kurzarbeit",
    )
    expect(screen.getByLabelText("Sachgebietsnorm")).toHaveValue("BGB")
  })

  it("click on search button emit 'search'", async () => {
    const { emitted } = renderComponent(undefined, undefined, undefined)

    await fireEvent.click(screen.getByLabelText("Sachgebietssuche ausführen"))

    expect(emitted()["search"]).toBeTruthy()
  })

  it("type in identifier input field should emit 'update:identifier'", async () => {
    const { emitted, user } = renderComponent(undefined, undefined, undefined)

    await user.type(screen.getByLabelText("Sachgebietskürzel"), "AR-02-02")

    expect(emitted()["update:identifier"]).toBeTruthy()
  })

  it("type in description input field should emit 'update:identifier'", async () => {
    const { emitted, user } = renderComponent(undefined, undefined, undefined)

    await user.type(screen.getByLabelText("Sachgebietsbezeichnung"), "kurz")

    expect(emitted()["update:description"]).toBeTruthy()
  })

  it("type in norm input field should emit 'update:norm'", async () => {
    const { emitted, user } = renderComponent(undefined, undefined, undefined)

    await user.type(screen.getByLabelText("Sachgebietsnorm"), "BGB")

    expect(emitted()["update:norm"]).toBeTruthy()
  })
})

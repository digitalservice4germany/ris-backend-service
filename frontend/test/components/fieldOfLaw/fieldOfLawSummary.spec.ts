import { fireEvent, render, RenderResult, screen } from "@testing-library/vue"
import FieldOfLawSummary from "@/components/field-of-law/FieldOfLawSummary.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"

function renderComponent(fieldsOfLaw: FieldOfLaw[]): RenderResult {
  return render(FieldOfLawSummary, { props: { fieldsOfLaw } })
}

function generateFieldOfLaw(): FieldOfLaw {
  return {
    identifier: "ST-01-02-03",
    text: "Steuerrecht 1-2-3",
    norms: [],
    children: [],
    hasChildren: false,
  }
}

describe("FieldOfLawSummary", () => {
  it("render one entry", () => {
    renderComponent([generateFieldOfLaw()])

    expect(screen.getByText("ST-01-02-03")).toBeInTheDocument()
    expect(screen.getByText("Steuerrecht 1-2-3")).toBeInTheDocument()
    expect(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 im Sachgebietsbaum anzeigen",
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 aus Liste entfernen",
      ),
    ).toBeInTheDocument()
  })

  it("click on 'Löschen' emit 'node:remove'", async () => {
    const { emitted } = renderComponent([generateFieldOfLaw()])

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 aus Liste entfernen",
      ),
    )

    expect(emitted()["node:remove"]).toBeTruthy()
  })

  it("click on 'Auswahl im Sachgebietsbaum anzeigen' emit 'node:clicked", async () => {
    const { emitted } = renderComponent([generateFieldOfLaw()])

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 im Sachgebietsbaum anzeigen",
      ),
    )
    expect(emitted()["node:clicked"]).toBeTruthy()
  })
})

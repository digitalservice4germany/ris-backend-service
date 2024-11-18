import { fireEvent, render, RenderResult, screen } from "@testing-library/vue"
import FieldOfLawSummary from "@/components/field-of-law/FieldOfLawSummary.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"

function renderComponent(data: FieldOfLaw[]): RenderResult {
  const props = {
    data,
  }

  return render(FieldOfLawSummary, { props })
}

describe("FieldOfLawSummary", () => {
  it("render one entry", () => {
    renderComponent([
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: false,
      },
    ])

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
    const { emitted } = renderComponent([
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: false,
      },
    ])

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 aus Liste entfernen",
      ),
    )

    expect(emitted()["node:remove"]).toBeTruthy()
  })

  it("click on 'Auswahl im Sachgebietsbaum' emit 'node:select", async () => {
    const { emitted } = renderComponent([
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: false,
      },
    ])

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 im Sachgebietsbaum anzeigen",
      ),
    )
    expect(emitted()["node:select"]).toBeTruthy()
  })
})

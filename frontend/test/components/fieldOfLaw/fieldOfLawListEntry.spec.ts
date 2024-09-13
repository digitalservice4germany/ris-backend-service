import { fireEvent, render, RenderResult, screen } from "@testing-library/vue"
import FieldOfLawListEntry from "@/components/FieldOfLawListEntry.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"

function renderComponent(
  fieldOfLaw: FieldOfLaw,
  showBin: boolean,
): RenderResult {
  const props = {
    fieldOfLaw,
    showBin,
  }

  return render(FieldOfLawListEntry, { props })
}

describe("FieldOfLawListEntry", () => {
  it("render entry", () => {
    renderComponent(
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: true,
      },
      true,
    )

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
    const { emitted } = renderComponent(
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: true,
      },
      true,
    )

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 aus Liste entfernen",
      ),
    )

    expect(emitted()["node:remove"]).toBeTruthy()
  })

  it("click on 'Auswahl im Sachgebietsbaum' emit 'node:select", async () => {
    const { emitted } = renderComponent(
      {
        identifier: "ST-01-02-03",
        text: "Steuerrecht 1-2-3",
        norms: [],
        children: [],
        hasChildren: true,
      },
      false,
    )

    await fireEvent.click(
      screen.getByLabelText(
        "ST-01-02-03 Steuerrecht 1-2-3 im Sachgebietsbaum anzeigen",
      ),
    )

    expect(emitted()["node:select"]).toBeTruthy()
  })
})

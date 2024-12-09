import { render, screen } from "@testing-library/vue"
import { previewLayoutInjectionKey } from "@/components/preview/constants"
import PreviewCoreData from "@/components/preview/PreviewCoreData.vue"
import { CoreData } from "@/domain/documentUnit"

function renderComponent(coreData: CoreData) {
  return render(PreviewCoreData, {
    props: {
      coreData: coreData,
    },
    global: {
      provide: {
        [previewLayoutInjectionKey as symbol]: "wide",
      },
    },
  })
}

describe("preview core data", () => {
  test("renders all core data fields", async () => {
    renderComponent({
      court: {
        label: "BVerfG",
      },
      leadingDecisionNormReferences: ["NSW WEG $ 14 (BGH-intern)"],
      deviatingCourts: ["BGH"],
      fileNumbers: ["abc-123"],
      deviatingFileNumbers: ["cde-456"],
      decisionDate: "2023-12-12",
      deviatingDecisionDates: ["2022-12-12"],
      appraisalBody: "1 Senat",
      documentType: {
        jurisShortcut: "Bes",
        label: "Beschluss",
      },
      ecli: "ecli123",
      deviatingEclis: ["eclu123"],
      procedure: {
        label: "vorgang-1",
        documentationUnitCount: 1,
        createdAt: "2022-12-12",
      },
      previousProcedures: ["vorgang-0"],
      legalEffect: "Ja",
      region: "gfs",
      yearsOfDispute: ["2023"],
    })

    expect(await screen.findByText("Gericht")).toBeInTheDocument()
    expect(await screen.findByText("Fehlerhaftes Gericht")).toBeInTheDocument()
    expect(await screen.findByText("Aktenzeichen")).toBeInTheDocument()
    expect(
      await screen.findByText("Abweichendes Aktenzeichen"),
    ).toBeInTheDocument()
    expect(await screen.findByText("Entscheidungsdatum")).toBeInTheDocument()
    expect(
      await screen.findByText("Abweichendes Entscheidungsdatum"),
    ).toBeInTheDocument()
    expect(await screen.findByText("Spruchkörper")).toBeInTheDocument()
    expect(await screen.findByText("Dokumenttyp")).toBeInTheDocument()
    expect(await screen.findByText("ECLI")).toBeInTheDocument()
    expect(await screen.findByText("Abweichender ECLI")).toBeInTheDocument()
    expect(await screen.findByText("Vorgang")).toBeInTheDocument()
    expect(await screen.findByText("Vorgangshistorie")).toBeInTheDocument()
    expect(await screen.findByText("Rechtskraft")).toBeInTheDocument()
    expect(await screen.findByText("Region")).toBeInTheDocument()
    expect(await screen.findByText("BGH Nachschlagewerk")).toBeInTheDocument()
    expect(await screen.findByText("Streitjahr")).toBeInTheDocument()
  })

  test("do not render empty list", async () => {
    renderComponent({
      deviatingCourts: [],
      fileNumbers: [],
      deviatingFileNumbers: [],
      deviatingDecisionDates: [],
      deviatingEclis: [],
      previousProcedures: [],
      leadingDecisionNormReferences: [],
      yearsOfDispute: [],
    })

    expect(screen.queryByText("Fehlerhaftes Gericht")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktenzeichen")).not.toBeInTheDocument()
    expect(
      screen.queryByText("Abweichendes Aktenzeichen"),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByText("Abweichendes Entscheidungsdatum"),
    ).not.toBeInTheDocument()
    expect(screen.queryByText("Abweichender ECLI")).not.toBeInTheDocument()
    expect(screen.queryByText("Vorgangshistorie")).not.toBeInTheDocument()
    expect(screen.queryByText("BGH Nachschlagewerk")).not.toBeInTheDocument()
    expect(screen.queryByText("Streitjahr")).not.toBeInTheDocument()
  })

  it.each([
    [
      "Gericht",
      { court: { label: "BVerfG" } },
      [
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
        "BGH Nachschlagewerk",
      ],
    ],
    [
      "Fehlerhaftes Gericht",
      { deviatingCourts: ["AG Foo"] },
      [
        "Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
        "BGH Nachschlagewerk",
      ],
    ],
    [
      "Aktenzeichen",
      { fileNumbers: ["abc123"] },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Abweichendes Aktenzeichen",
      { deviatingFileNumbers: ["def456"] },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Entscheidungsdatum",
      { decisionDate: "2024-12-12" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Abweichendes Entscheidungsdatum",
      { deviatingDecisionDates: ["2014-12-12"] },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Spruchkörper",
      { appraisalBody: "Senat" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Dokumenttyp",
      { documentType: { jurisShortcut: "Bes", label: "Beschluss" } },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "ECLI",
      { ecli: "ECLI:123:456:ABC" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Abweichender ECLI",
      { deviatingEclis: ["ECLI:123:456:ABC"] },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Vorgang",
      {
        procedure: {
          label: "vorgang2",
          documentationUnitCount: 1,
          createdAt: "2023-12-24",
        },
      },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgangshistorie",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Vorgangshistorie",
      { previousProcedures: ["vorgang1"] },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Rechtskraft",
        "Region",
      ],
    ],
    [
      "Rechtskraft",
      { legalEffect: "Ja" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Region",
      ],
    ],
    [
      "Region",
      { region: "BRD" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
      ],
    ],
    [
      "Region",
      { region: "BRD" },
      [
        "Gericht",
        "Fehlerhaftes Gericht",
        "Aktenzeichen",
        "Abweichendes Aktenzeichen",
        "Entscheidungsdatum",
        "Abweichendes Entscheidungsdatum",
        "Spruchkörper",
        "Dokumenttyp",
        "ECLI",
        "Abweichender ECLI",
        "Vorgang",
        "Vorgangshistorie",
        "Rechtskraft",
      ],
    ],
  ])(
    `renders only %s`,
    async (expected: string, coreData: CoreData, notExpected: string[]) => {
      renderComponent(coreData)
      expect(await screen.findByText(expected)).toBeInTheDocument()
      notExpected.forEach((it) => {
        expect(screen.queryByText(it)).not.toBeInTheDocument()
      })
    },
  )
})

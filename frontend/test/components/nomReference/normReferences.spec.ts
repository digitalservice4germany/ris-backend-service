import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { describe } from "vitest"
import { ComboboxItem } from "@/components/input/types"
import NormReferences from "@/components/NormReferences.vue"
import DocumentUnit from "@/domain/documentUnit"
import LegalForce from "@/domain/legalForce"
import { NormAbbreviation } from "@/domain/normAbbreviation"
import NormReference from "@/domain/normReference"
import SingleNorm from "@/domain/singleNorm"
import comboboxItemService from "@/services/comboboxItemService"
import documentUnitService from "@/services/documentUnitService"

function renderComponent(normReferences?: NormReference[]) {
  const user = userEvent.setup()
  return {
    user,
    ...render(NormReferences, {
      global: {
        plugins: [
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit: new DocumentUnit("123", {
                    documentNumber: "foo",
                    contentRelatedIndexing: {
                      norms: normReferences ?? undefined,
                    },
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

function generateNormReference(options?: {
  normAbbreviation?: NormAbbreviation
  singleNorms?: SingleNorm[]
}) {
  const normReference = new NormReference({
    normAbbreviation: options?.normAbbreviation ?? { abbreviation: "ABC" },
    singleNorms: options?.singleNorms ?? [],
  })
  return normReference
}

describe("Norm references", () => {
  const normAbbreviation: NormAbbreviation = {
    abbreviation: "1000g-BefV",
  }
  const dropdownAbbreviationItems: ComboboxItem[] = [
    {
      label: normAbbreviation.abbreviation,
      value: normAbbreviation,
    },
  ]
  vi.spyOn(comboboxItemService, "getRisAbbreviations").mockImplementation(() =>
    Promise.resolve({ status: 200, data: dropdownAbbreviationItems }),
  )

  it("renders empty norm reference in edit mode, when no norm references in list", async () => {
    renderComponent()
    expect((await screen.findAllByLabelText("Listen Eintrag")).length).toBe(1)
    expect(await screen.findByLabelText("RIS-Abkürzung")).toBeInTheDocument()
  })

  it("renders norm references as list entries", () => {
    const normReferences: NormReference[] = [
      generateNormReference({
        singleNorms: [new SingleNorm({ singleNorm: "§ 123" })],
      }),
      generateNormReference({
        singleNorms: [new SingleNorm({ singleNorm: "§ 345" })],
      }),
    ]
    renderComponent(normReferences)

    expect(screen.getAllByLabelText("Listen Eintrag").length).toBe(2)
    expect(screen.queryByLabelText("RIS-Abkürzung")).not.toBeInTheDocument()
    expect(screen.getByText(/§ 123/)).toBeInTheDocument()
    expect(screen.getByText(/§ 345/)).toBeInTheDocument()
  })

  it("click on list item, opens the list entry in edit mode", async () => {
    vi.spyOn(documentUnitService, "validateSingleNorm").mockImplementation(() =>
      Promise.resolve({ status: 200, data: "Ok" }),
    )
    const { user } = renderComponent([
      generateNormReference({
        singleNorms: [new SingleNorm({ singleNorm: "§ 123" })],
      }),
    ])
    await user.click(screen.getByTestId("list-entry-0"))

    expect(screen.getByLabelText("RIS-Abkürzung")).toBeInTheDocument()
    expect(screen.getByLabelText("Einzelnorm der Norm")).toBeInTheDocument()
    expect(screen.getByLabelText("Fassungsdatum der Norm")).toBeInTheDocument()
    expect(screen.getByLabelText("Jahr der Norm")).toBeInTheDocument()
  })

  it("validates against duplicate entries in new entries", async () => {
    vi.spyOn(documentUnitService, "validateSingleNorm").mockImplementation(() =>
      Promise.resolve({ status: 200, data: "Ok" }),
    )
    const { user } = renderComponent([
      generateNormReference({
        normAbbreviation: { abbreviation: "1000g-BefV" },
      }),
    ])
    await user.click(screen.getByLabelText("Weitere Angabe"))

    const abbreviationField = screen.getByLabelText("RIS-Abkürzung")
    await user.type(abbreviationField, "1000g-BefV")
    const dropdownItems = screen.getAllByLabelText(
      "dropdown-option",
    ) as HTMLElement[]
    expect(dropdownItems[0]).toHaveTextContent("1000g-BefV")
    await user.click(dropdownItems[0])
    await screen.findByText(/RIS-Abkürzung bereits eingegeben/)
  })

  it("validates against duplicate entries in existing entries", async () => {
    vi.spyOn(documentUnitService, "validateSingleNorm").mockImplementation(() =>
      Promise.resolve({ status: 200, data: "Ok" }),
    )
    const { user } = renderComponent([
      generateNormReference({
        normAbbreviation: { abbreviation: "1000g-BefV" },
      }),
      generateNormReference(),
    ])
    await user.click(screen.getByTestId("list-entry-1"))

    const abbreviationField = screen.getByLabelText("RIS-Abkürzung")
    await user.type(abbreviationField, "1000g-BefV")
    const dropdownItems = screen.getAllByLabelText(
      "dropdown-option",
    ) as HTMLElement[]
    expect(dropdownItems[0]).toHaveTextContent("1000g-BefV")
    await user.click(dropdownItems[0])
    await screen.findByText(/RIS-Abkürzung bereits eingegeben/)
    const button = screen.getByLabelText("Norm speichern")
    await user.click(button)
    await screen.findByText(/RIS-Abkürzung bereits eingegeben/)
  })

  it("Removes duplicate entries in single norms", async () => {
    vi.spyOn(documentUnitService, "validateSingleNorm").mockImplementation(() =>
      Promise.resolve({ status: 200, data: "Ok" }),
    )
    const { user } = renderComponent([
      generateNormReference({
        normAbbreviation: {
          abbreviation: "1000g-BefV",
        },
        singleNorms: [
          new SingleNorm({
            singleNorm: "§ 345",
            dateOfRelevance: "2022",
            dateOfVersion: "01.01.2022",
          }),
        ],
      }),
    ])

    expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
      "1000g-BefV, § 345, 01.01.2022, 2022",
    )

    await user.click(screen.getByTestId("list-entry-0"))
    await user.click(screen.getByLabelText("Weitere Einzelnorm"))

    const singleNorms = await screen.findAllByLabelText("Einzelnorm der Norm")
    await user.type(singleNorms[1], "§ 345")

    const dates = await screen.findAllByLabelText("Fassungsdatum der Norm")
    await user.type(dates[1], "01.01.2022")

    const years = await screen.findAllByLabelText("Jahr der Norm")
    await user.type(years[1], "2022")

    const button = screen.getByLabelText("Norm speichern")
    await user.click(button)

    const listItems = screen.getAllByLabelText("Listen Eintrag")
    expect(listItems[0]).toHaveTextContent(
      "1000g-BefV, § 345, 01.01.2022, 2022",
    )
  })

  it("deletes norm reference", async () => {
    const { user } = renderComponent([
      generateNormReference(),
      generateNormReference({
        normAbbreviation: {
          abbreviation: "1000g-BefV",
        },
      }),
    ])

    const norms = screen.getAllByLabelText("Listen Eintrag")
    expect(norms.length).toBe(2)
    await user.click(screen.getByTestId("list-entry-0"))
    await user.click(screen.getByLabelText("Eintrag löschen"))
    expect(screen.getAllByLabelText("Listen Eintrag").length).toBe(1)
  })

  it("click on 'Weitere Angabe' adds new emptry list entry", async () => {
    const { user } = renderComponent([
      generateNormReference(),
      generateNormReference(),
    ])
    const normsRefernces = screen.getAllByLabelText("Listen Eintrag")
    expect(normsRefernces.length).toBe(2)
    const button = screen.getByLabelText("Weitere Angabe")
    await user.click(button)
    expect(screen.getAllByLabelText("Listen Eintrag").length).toBe(3)
  })

  it("render summary with one single norms", async () => {
    renderComponent([
      generateNormReference({
        normAbbreviation: {
          abbreviation: "1000g-BefV",
        },
        singleNorms: [new SingleNorm({ singleNorm: "§ 123" })],
      }),
    ])

    expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
      "1000g-BefV, § 123",
    )
  })

  it("render summary with multiple single norms", async () => {
    renderComponent([
      generateNormReference({
        normAbbreviation: {
          abbreviation: "1000g-BefV",
        },
        singleNorms: [
          new SingleNorm({ singleNorm: "§ 123" }),
          new SingleNorm({
            singleNorm: "§ 345",
            dateOfRelevance: "02-02-2022",
            dateOfVersion: "2022",
          }),
        ],
      }),
    ])

    expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
      "1000g-BefV 1000g-BefV, § 123 1000g-BefV, § 345, 01.01.2022, 02-02-2022",
    )
  })

  it("render summary with no single norms", async () => {
    renderComponent([
      generateNormReference({
        normAbbreviation: {
          abbreviation: "1000g-BefV",
        },
        singleNorms: [
          new SingleNorm({ singleNorm: "§ 123" }),
          new SingleNorm({
            singleNorm: "§ 345",
            dateOfRelevance: "02-02-2022",
            dateOfVersion: "2022",
          }),
        ],
      }),
    ])

    expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
      "1000g-BefV 1000g-BefV, § 123 1000g-BefV, § 345, 01.01.2022, 02-02-2022",
    )
  })

  it("render error badge, when norm reference is ambiguous", async () => {
    renderComponent([
      new NormReference({
        normAbbreviationRawValue: "EWGAssRBes 1/80",
      }),
    ])

    expect(screen.getByText("Mehrdeutiger Verweis")).toBeInTheDocument()
  })

  describe("legal force", () => {
    it("render summary with legal force type and region", () => {
      renderComponent([
        generateNormReference({
          normAbbreviation: {
            abbreviation: "1000g-BefV",
          },
          singleNorms: [
            new SingleNorm({
              singleNorm: "§ 345",
              dateOfRelevance: "02-02-2022",
              dateOfVersion: "2022",
              legalForce: new LegalForce({
                type: { abbreviation: "nichtig" },
                region: { code: "BB", longText: "Brandenburg" },
              }),
            }),
          ],
        }),
      ])

      expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
        "1000g-BefV, § 345, 01.01.2022, 02-02-2022 | Nichtig (Brandenburg)",
      )
    })

    it("render summary with legal force but without missing data error", () => {
      renderComponent([
        generateNormReference({
          normAbbreviation: {
            abbreviation: "1000g-BefV",
          },
          singleNorms: [
            new SingleNorm({
              singleNorm: "§ 345",
              dateOfRelevance: "02-02-2022",
              dateOfVersion: "2022",
              legalForce: new LegalForce({
                type: { abbreviation: "nichtig" },
                region: undefined,
              }),
            }),
          ],
        }),
      ])

      expect(screen.getByLabelText("Listen Eintrag")).toHaveTextContent(
        "1000g-BefV, § 345, 01.01.2022, 02-02-2022 | Nichtig Fehlende Daten",
      )
    })
  })
})

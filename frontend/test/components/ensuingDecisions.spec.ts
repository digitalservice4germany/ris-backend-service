import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import EnsuingDecisions from "@/components/EnsuingDecisions.vue"
import { ComboboxItem } from "@/components/input/types"
import DocumentUnit, { Court, DocumentType } from "@/domain/documentUnit"
import EnsuingDecision from "@/domain/ensuingDecision"
import comboboxItemService from "@/services/comboboxItemService"
import documentUnitService from "@/services/documentUnitService"
import routes from "~/test-helper/routes"

function renderComponent(ensuingDecisions?: EnsuingDecision[]) {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })

  return {
    user,
    ...render(EnsuingDecisions, {
      global: {
        plugins: [
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit: new DocumentUnit("foo", {
                    documentNumber: "1234567891234",
                    ensuingDecisions: ensuingDecisions ?? [],
                  }),
                },
              },
              stubActions: false,
            }),
          ],
          [router],
        ],
        stubs: { routerLink: { template: "<a><slot/></a>" } },
      },
    }),
  }
}

function generateEnsuingDecision(options?: {
  uuid?: string
  documentNumber?: string
  court?: Court
  decisionDate?: string
  fileNumber?: string
  documentType?: DocumentType
  referenceFound?: boolean
  pending?: boolean
  note?: string
}) {
  const ensuingDecision = new EnsuingDecision({
    uuid: options?.uuid ?? crypto.randomUUID(),
    documentNumber: options?.documentNumber ?? undefined,
    court: options?.court ?? {
      type: "type1",
      location: "location1",
      label: "label1",
    },
    decisionDate: options?.decisionDate ?? "2022-02-01",
    fileNumber: options?.fileNumber ?? "test fileNumber",
    documentType: options?.documentType ?? {
      jurisShortcut: "documentTypeShortcut1",
      label: "documentType1",
    },
    referenceFound: options?.referenceFound ?? false,
    pending: options?.pending ?? false,
    note: options?.note ?? undefined,
  })
  return ensuingDecision
}

describe("EnsuingDecisions", () => {
  vi.spyOn(
    documentUnitService,
    "searchByRelatedDocumentation",
  ).mockImplementation(() =>
    Promise.resolve({
      status: 200,
      data: {
        content: [
          new EnsuingDecision({
            uuid: "123",
            court: {
              type: "type1",
              location: "location1",
              label: "label1",
            },
            decisionDate: "2022-02-01",
            documentType: {
              jurisShortcut: "documentTypeShortcut1",
              label: "documentType1",
            },
            fileNumber: "test fileNumber1",
          }),
        ],
        size: 0,
        number: 0,
        numberOfElements: 20,
        first: true,
        last: false,
        empty: false,
      },
    }),
  )

  vi.spyOn(window, "scrollTo").mockImplementation(() => vi.fn())

  const court: Court = {
    type: "AG",
    location: "Test",
    label: "AG Test",
  }

  const documentType: DocumentType = {
    jurisShortcut: "Ant",
    label: "EuGH-Vorlage",
  }

  const dropdownCourtItems: ComboboxItem[] = [
    {
      label: court.label,
      value: court,
      additionalInformation: court.revoked,
    },
  ]

  const dropdownDocumentTypesItems: ComboboxItem[] = [
    {
      label: documentType.label,
      value: documentType,
      additionalInformation: documentType.jurisShortcut,
    },
  ]

  vi.spyOn(comboboxItemService, "getCourts").mockImplementation(() =>
    Promise.resolve({ status: 200, data: dropdownCourtItems }),
  )

  vi.spyOn(comboboxItemService, "getDocumentTypes").mockImplementation(() =>
    Promise.resolve({ status: 200, data: dropdownDocumentTypesItems }),
  )

  it("renders empty ensuing decision in edit mode, when no ensuingDecisions in list", async () => {
    renderComponent()
    expect(await screen.findByLabelText("Anhängige Entscheidung")).toBeVisible()
    expect(
      await screen.findByLabelText("Gericht Nachgehende Entscheidung"),
    ).toBeVisible()
    expect(
      await screen.findByLabelText(
        "Entscheidungsdatum Nachgehende Entscheidung",
      ),
    ).toBeVisible()
    expect(
      await screen.findByLabelText("Aktenzeichen Nachgehende Entscheidung"),
    ).toBeInTheDocument()
    expect(
      await screen.findByLabelText("Dokumenttyp Nachgehende Entscheidung"),
    ).toBeInTheDocument()
    expect(await screen.findByLabelText("Vermerk")).toBeInTheDocument()
    expect(
      await screen.findByLabelText("Nachgehende Entscheidung speichern"),
    ).toBeDisabled()
  })

  it("renders ensuing decisions as list entries", () => {
    const ensuingDecisions: EnsuingDecision[] = [
      generateEnsuingDecision({ fileNumber: "123" }),
      generateEnsuingDecision({ fileNumber: "345" }),
    ]
    renderComponent(ensuingDecisions)

    expect(
      screen.queryByLabelText("Nachgehende Entscheidung speichern"),
    ).not.toBeInTheDocument()
    expect(screen.getByText(/123/)).toBeInTheDocument()
    expect(screen.getByText(/345/)).toBeInTheDocument()
  })

  it("creates new ensuing decision manually", async () => {
    const { user } = renderComponent()
    const input = await screen.findByLabelText(
      "Aktenzeichen Nachgehende Entscheidung",
    )
    await user.type(input, "123")
    const button = await screen.findByLabelText(
      "Nachgehende Entscheidung speichern",
    )
    await user.click(button)

    expect(screen.getAllByLabelText("Listen Eintrag").length).toBe(2)
  })

  it("click on list item, opens the list entry in edit mode", async () => {
    const { user } = renderComponent([
      generateEnsuingDecision({
        fileNumber: "123",
      }),
    ])

    expect(
      screen.queryByLabelText("Nachgehende Entscheidung speichern"),
    ).not.toBeInTheDocument()

    await user.click(screen.getByTestId("list-entry-0"))

    expect(
      screen.getByLabelText("Nachgehende Entscheidung speichern"),
    ).toBeInTheDocument()
  })

  it("correctly toggles value of date known checkbox", async () => {
    const { user } = renderComponent([generateEnsuingDecision()])

    await user.click(screen.getByTestId("list-entry-0"))

    const checkbox = await screen.findByLabelText("Anhängige Entscheidung")

    expect(checkbox).not.toBeChecked()

    await user.click(checkbox)
    expect(checkbox).toBeChecked()
  })

  it("correctly updates value court input", async () => {
    const { user } = renderComponent([generateEnsuingDecision()])

    expect(screen.queryByText(/AG Test/)).not.toBeInTheDocument()

    await user.click(screen.getByTestId("list-entry-0"))

    await user.type(
      await screen.findByLabelText("Gericht Nachgehende Entscheidung"),
      "AG",
    )
    const dropdownItems = screen.getAllByLabelText("dropdown-option")
    expect(dropdownItems[0]).toHaveTextContent("AG Test")
    await user.click(dropdownItems[0])
    const button = screen.getByLabelText("Nachgehende Entscheidung speichern")
    await user.click(button)

    expect(screen.getByText(/AG Test/)).toBeVisible()
  })

  it("correctly updates value of fileNumber input", async () => {
    const { user } = renderComponent([generateEnsuingDecision()])

    expect(screen.queryByText(/new fileNumber/)).not.toBeInTheDocument()
    await user.click(screen.getByTestId("list-entry-0"))

    const fileNumberInput = await screen.findByLabelText(
      "Aktenzeichen Nachgehende Entscheidung",
    )

    await user.clear(fileNumberInput)
    await user.type(fileNumberInput, "new fileNumber")
    const button = screen.getByLabelText("Nachgehende Entscheidung speichern")
    await user.click(button)

    expect(screen.getByText(/new fileNumber/)).toBeVisible()
  })

  it("correctly updates value of decision date input", async () => {
    const { user } = renderComponent([generateEnsuingDecision()])

    expect(screen.queryByText(/02.02.2022/)).not.toBeInTheDocument()
    await user.click(screen.getByTestId("list-entry-0"))

    const fileNumberInput = await screen.findByLabelText(
      "Entscheidungsdatum Nachgehende Entscheidung",
    )

    await user.clear(fileNumberInput)
    await user.type(fileNumberInput, "02.02.2022")
    const button = screen.getByLabelText("Nachgehende Entscheidung speichern")
    await user.click(button)

    expect(screen.getByText(/02.02.2022/)).toBeVisible()
  })

  it("correctly deletes ensuing decision", async () => {
    const { user } = renderComponent([
      generateEnsuingDecision(),
      generateEnsuingDecision(),
    ])
    const ensuingDecisions = screen.getAllByLabelText("Listen Eintrag")
    expect(ensuingDecisions.length).toBe(2)

    await user.click(screen.getByTestId("list-entry-0"))
    await user.click(screen.getByLabelText("Eintrag löschen"))
    expect(screen.getAllByLabelText("Listen Eintrag").length).toBe(1)
  })

  it("renders from search added ensuing decisions as editable list item, note can be updated", async () => {
    const { user } = renderComponent([
      generateEnsuingDecision({
        documentNumber: "ABC",
        referenceFound: true,
      }),
    ])
    await user.click(screen.getByTestId("list-entry-0"))

    expect(screen.getByLabelText("Anhängige Entscheidung")).toBeDisabled()
    expect(screen.getByLabelText("Vermerk")).toBeInTheDocument()
    const saveButton = screen.getByLabelText(
      "Nachgehende Entscheidung speichern",
    )
    expect(saveButton).toBeEnabled()

    const input = screen.getByLabelText("Vermerk")
    await user.type(input, "Vermerk")

    await user.click(saveButton)
    expect(
      screen.getByText(
        /nachgehend, label1, 01.02.2022, test fileNumber, documentType1, Vermerk/,
      ),
    ).toBeInTheDocument()
  })

  it("lists search results", async () => {
    const { user } = renderComponent()

    expect(screen.queryByText(/test fileNumber/)).not.toBeInTheDocument()
    await user.click(await screen.findByLabelText("Nach Entscheidung suchen"))

    expect(screen.getAllByText(/test fileNumber/).length).toBe(1)
  })

  it("adds ensuing decision from search results", async () => {
    const { user } = renderComponent()

    await user.click(await screen.findByLabelText("Nach Entscheidung suchen"))
    await user.click(await screen.findByLabelText("Treffer übernehmen"))
    expect(screen.getAllByText(/test fileNumber/).length).toBe(1)
  })

  it("indicates that search result already added to ensuing decisions", async () => {
    const ensuingDecisions: EnsuingDecision[] = [
      generateEnsuingDecision({ uuid: "123" }),
    ]
    const { user } = renderComponent(ensuingDecisions)
    await user.click(screen.getByText(/Weitere Angabe/))
    await user.click(screen.getByLabelText("Nach Entscheidung suchen"))
    expect(screen.getByText(/Bereits hinzugefügt/)).toBeInTheDocument()
  })

  it("displays error in list and edit component when fields missing", async () => {
    const ensuingDecisions: EnsuingDecision[] = [generateEnsuingDecision()]
    const { user } = renderComponent(ensuingDecisions)
    await user.click(screen.getByTestId("list-entry-0"))

    const fileInput = await screen.findByLabelText(
      "Aktenzeichen Nachgehende Entscheidung",
    )
    await user.clear(fileInput)
    await user.click(
      screen.getByLabelText("Nachgehende Entscheidung speichern"),
    )
    expect(screen.getByText(/Fehlende Daten/)).toBeInTheDocument()
    await user.click(screen.getByTestId("list-entry-0"))
    expect(screen.getAllByText(/Pflichtfeld nicht befüllt/).length).toBe(1)
  })

  it("does not add ensuing decision with invalid date input", async () => {
    const { user } = renderComponent()

    const dateInput = await screen.findByLabelText(
      "Entscheidungsdatum Nachgehende Entscheidung",
    )
    expect(dateInput).toHaveValue("")

    await user.type(dateInput, "00.00.0231")

    await screen.findByText(/Kein valides Datum/)
    screen.getByLabelText("Nachgehende Entscheidung speichern").click()
    expect(dateInput).toBeVisible()
  })

  it("does not add ensuing decision with incomplete date input", async () => {
    const { user } = renderComponent()

    const dateInput = await screen.findByLabelText(
      "Entscheidungsdatum Nachgehende Entscheidung",
    )
    expect(dateInput).toHaveValue("")

    await user.type(dateInput, "01")
    await user.tab()

    await screen.findByText(/Unvollständiges Datum/)
    screen.getByLabelText("Nachgehende Entscheidung speichern").click()
    expect(dateInput).toBeVisible()
  })

  it("does not add ensuing decision with date in future", async () => {
    const { user } = renderComponent()

    const dateInput = await screen.findByLabelText(
      "Entscheidungsdatum Nachgehende Entscheidung",
    )
    expect(dateInput).toHaveValue("")

    await user.type(dateInput, "01.02.2090")
    await user.tab()

    await screen.findByText(/Das Datum darf nicht in der Zukunft liegen/)
    screen.getByLabelText("Nachgehende Entscheidung speichern").click()
    expect(dateInput).toBeVisible()
  })
})

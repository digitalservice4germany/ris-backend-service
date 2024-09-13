import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import { ComboboxItem } from "@/components/input/types"
import NewEdition from "@/components/periodical-evaluation/PeriodicalEdition.vue"
import LegalPeriodical from "@/domain/legalPeriodical"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import comboboxItemService from "@/services/comboboxItemService"
import service from "@/services/legalPeriodicalEditionService"
import testRoutes from "~/test-helper/routes"

const editionUUid = crypto.randomUUID()

const legalPeriodical: LegalPeriodical = {
  uuid: crypto.randomUUID(),
  abbreviation: "BDZ",
  title: "Bundesgesetzblatt",
  citationStyle: "2024, Heft 1",
}

async function renderComponent() {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: testRoutes,
  })

  // Mock the route with a specific uuid before rendering
  await router.push({
    name: "caselaw-periodical-evaluation-editionId-edition",
    params: { editionId: editionUUid },
  })

  // Wait for the router to be ready
  return router.isReady().then(() => ({
    user,
    ...render(NewEdition, {
      global: {
        plugins: [
          router,
          [
            createTestingPinia({
              initialState: {
                editionStore: {
                  edition: new LegalPeriodicalEdition({
                    id: editionUUid,
                    legalPeriodical: legalPeriodical,
                    name: "name",
                    prefix: "präfix",
                    suffix: "suffix",
                    references: [],
                  }),
                },
                stubActions: false,
              },
            }),
          ],
        ],
      },
    }),
  }))
}

describe("Legal periodical edition list", () => {
  const dropdownLegalPeriodicalItems: ComboboxItem[] = [
    {
      label: legalPeriodical.abbreviation! + " | " + legalPeriodical.title!,
      value: legalPeriodical,
    },
  ]

  vi.spyOn(comboboxItemService, "getLegalPeriodicals").mockImplementation(() =>
    Promise.resolve({ status: 200, data: dropdownLegalPeriodicalItems }),
  )

  vi.spyOn(service, "get").mockImplementation(() =>
    Promise.resolve({
      status: 200,
      data: new LegalPeriodicalEdition({
        id: editionUUid,
        legalPeriodical: legalPeriodical,
        name: "name",
        prefix: "präfix",
        suffix: "suffix",
        references: [],
      }),
    }),
  )
  test("renders correctly", async () => {
    await renderComponent()

    expect(screen.getByText("Ausgabe")).toBeVisible()
    expect(screen.getByLabelText("Periodikum")).toBeVisible()
    expect(screen.getByLabelText("Name der Ausgabe")).toBeVisible()
    expect(screen.getByLabelText("Präfix")).toBeVisible()
    expect(screen.getByLabelText("Suffix")).toBeVisible()
    expect(screen.getByText("Fortfahren")).toBeVisible()
    expect(screen.getByText("Abbrechen")).toBeVisible()
  })

  test("selecting legal periodical from combobox value for legal periodical", async () => {
    const { user } = await renderComponent()
    const periodicalField = screen.getByLabelText("Periodikum")

    await user.type(periodicalField, "BDZ")
    const dropdownItems = screen.getAllByLabelText(
      "dropdown-option",
    ) as HTMLElement[]
    expect(dropdownItems[0]).toHaveTextContent("BDZ | Bundesgesetzblatt")
    await user.click(dropdownItems[0])
    await expect(periodicalField).toHaveValue("BDZ | Bundesgesetzblatt")
  })

  test("clicking Speichern button calls service with correct values", async () => {
    const fetchSpy = vi.spyOn(service, "save").mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: new LegalPeriodicalEdition({
          id: editionUUid,
          legalPeriodical: legalPeriodical,
          name: "name",
          prefix: "präfix",
          suffix: "suffix",
          references: [],
        }),
      }),
    )
    const { user } = await renderComponent()
    const periodicalField = screen.getByLabelText("Periodikum")

    await user.type(periodicalField, "BDZ")
    const dropdownItems = screen.getAllByLabelText(
      "dropdown-option",
    ) as HTMLElement[]
    expect(dropdownItems[0]).toHaveTextContent("BDZ | Bundesgesetzblatt")
    await user.click(dropdownItems[0])
    await expect(periodicalField).toHaveValue("BDZ | Bundesgesetzblatt")

    await user.clear(screen.getByLabelText("Name der Ausgabe"))
    await user.type(screen.getByLabelText("Name der Ausgabe"), "new name")

    await user.clear(screen.getByLabelText("Präfix"))
    await user.type(screen.getByLabelText("Präfix"), "new präfix")

    await user.clear(screen.getByLabelText("Suffix"))
    await user.type(screen.getByLabelText("Suffix"), "new suffix")

    await user.click(screen.getByText("Fortfahren"))
    expect(fetchSpy).toHaveBeenCalledTimes(1)
    expect(fetchSpy).toHaveBeenCalledWith(
      new LegalPeriodicalEdition({
        id: editionUUid,
        createdAt: undefined,
        legalPeriodical: legalPeriodical,
        name: "new name",
        prefix: "new präfix",
        references: [],
        suffix: "new suffix",
      }),
    )
  })

  describe("Legal periodical validation", () => {
    test("don't call save if empty field", async () => {
      const fetchSpy = vi.spyOn(service, "save").mockImplementation(() =>
        Promise.resolve({
          status: 200,
          data: new LegalPeriodicalEdition({
            id: editionUUid,
            legalPeriodical: legalPeriodical,
            name: "name",
            prefix: "präfix",
            suffix: "suffix",
            references: [],
          }),
        }),
      )
      const { user } = await renderComponent()

      await user.clear(screen.getByLabelText("Name der Ausgabe"))
      await user.click(screen.getByLabelText("Fortfahren"))

      expect(
        screen.getAllByText("Pflichtfeld nicht befüllt").length,
        "should be shown if legal periodical empty",
      ).toBe(1)
      expect(fetchSpy).toHaveBeenCalledTimes(0)
    })

    test("save if legal periodical and (name / präfix) are not null", async () => {
      const fetchSpy = vi.spyOn(service, "save").mockImplementation(() =>
        Promise.resolve({
          status: 200,
          data: new LegalPeriodicalEdition({
            id: editionUUid,
            legalPeriodical: legalPeriodical,
            name: "name",
            prefix: "präfix",
            suffix: "suffix",
            references: [],
          }),
        }),
      )

      const { user } = await renderComponent()
      const periodicalField = screen.getByLabelText("Periodikum")

      await user.type(periodicalField, "BDZ")
      const dropdownItems = screen.getAllByLabelText(
        "dropdown-option",
      ) as HTMLElement[]
      expect(dropdownItems[0]).toHaveTextContent("BDZ | Bundesgesetzblatt")
      await user.click(dropdownItems[0])
      expect(periodicalField).toHaveValue("BDZ | Bundesgesetzblatt")

      await user.type(screen.getByLabelText("Name der Ausgabe"), "name")

      await user.click(screen.getByLabelText("Fortfahren"))

      expect(fetchSpy).toHaveBeenCalledTimes(1)
    })
  })
})

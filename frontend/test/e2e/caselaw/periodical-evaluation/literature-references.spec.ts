import { expect, Page } from "@playwright/test"
import {
  fillInput,
  navigateToPeriodicalReferences,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

/* eslint-disable playwright/no-conditional-in-test */

test.describe("Literature references", () => {
  test(
    "Switching between caselaw and literature reference type renders different inputs",
    {
      tag: "@RISDEV-5238",
    },
    async ({ page, edition }) => {
      await test.step("Caselaw reference type is preselected", async () => {
        await navigateToPeriodicalReferences(page, edition.id || "")

        await expect(page.getByLabel("Rechtsprechung Fundstelle")).toBeChecked()

        await expect(page.getByLabel("Literatur Fundstelle")).not.toBeChecked()

        await expect(page.getByLabel("Klammernzusatz")).toBeVisible()

        await expect(
          page.getByLabel("Dokumenttyp Literaturfundstelle"),
        ).toBeHidden()

        await expect(page.getByLabel("Autor Literaturfundstelle")).toBeHidden()
      })

      await test.step("Selecting literature reference type, renders different inputs", async () => {
        await page.getByLabel("Literatur Fundstelle").click()
        await expect(
          page.getByLabel("Rechtsprechung Fundstelle"),
        ).not.toBeChecked()

        await expect(page.getByLabel("Literatur Fundstelle")).toBeChecked()

        await expect(page.getByLabel("Klammernzusatz")).toBeHidden()

        await expect(
          page.getByLabel("Dokumenttyp Literaturfundstelle"),
        ).toBeVisible()

        await expect(page.getByLabel("Autor Literaturfundstelle")).toBeVisible()
      })
    },
  )

  test(
    "Literature references can be added to periodical evaluation",
    {
      tag: "@RISDEV-5240 @RISDEV-5237 @RISDEV-5454",
    },
    async ({ page, prefilledDocumentUnit, edition }) => {
      const fileNumber = prefilledDocumentUnit.coreData.fileNumbers?.[0] || ""
      await test.step("Literature references are validated for required inputs", async () => {
        await navigateToPeriodicalReferences(page, edition.id || "")
        await page.getByLabel("Literatur Fundstelle").click()
        await expect(page.getByLabel("Literatur Fundstelle")).toBeChecked()
        await fillInput(page, "Zitatstelle *", `2021, 2`)

        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await page.getByLabel("Treffer übernehmen").click()
        // check that both fields display error message
        await expect(
          page.locator("text=Pflichtfeld nicht befüllt"),
        ).toHaveCount(2)

        // Switching between radio buttons resets the validation errors
        await page.getByLabel("Rechtsprechung Fundstelle").click()
        await page.getByLabel("Literatur Fundstelle").click()
        await expect(
          page.locator("text=Pflichtfeld nicht befüllt"),
        ).toHaveCount(0)
      })

      await test.step("Save literature reference, verify that it is shown in the list", async () => {
        await fillInput(page, "Autor Literaturfundstelle", "Bilen, Ulviye")
        await fillInput(page, "Dokumenttyp Literaturfundstelle", "Ean")
        await page.getByText("Ean", { exact: true }).click()
        await waitForInputValue(
          page,
          "[aria-label='Dokumenttyp Literaturfundstelle']",
          "Anmerkung",
        )

        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await page.getByLabel("Treffer übernehmen").click()
        await expect(
          page.getByText(
            `Bilen, Ulviye, MMG 2024, 2021, 2${edition.suffix} (Ean)`,
          ),
        ).toBeVisible()
      })

      await test.step("Radio buttons should not be visible after saving", async () => {
        await page.getByTestId("list-entry-0").click()
        await expect(page.getByLabel("Rechtsprechung Fundstelle")).toBeHidden()

        await expect(page.getByLabel("Literatur Fundstelle")).toBeHidden()
      })
    },
  )

  test(
    "Literature and caselaw references are chronologically ordered",
    {
      tag: "@RISDEV-5240",
    },
    async ({ page, prefilledDocumentUnit, editionWithReferences }) => {
      const fileNumber = prefilledDocumentUnit.coreData.fileNumbers?.[0] || ""
      await test.step("Add caselaw reference to existing references", async () => {
        await navigateToPeriodicalReferences(
          page,
          editionWithReferences.id || "",
        )
        await expect(
          page.getByText(`MMG 2024, 12-22, Heft 1 (L)`, { exact: true }),
        ).toBeVisible()
        await fillInput(page, "Zitatstelle *", "300")
        await fillInput(page, "Klammernzusatz", "ST")

        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await page.getByLabel("Treffer übernehmen").click()
        await expect(
          page.getByText(`MMG 2024, 300${editionWithReferences.suffix} (ST)`),
        ).toBeVisible()
      })
      await test.step("Add literature reference to existing references", async () => {
        await page.getByLabel("Literatur Fundstelle").click()
        await expect(page.getByLabel("Literatur Fundstelle")).toBeChecked()
        await fillInput(page, "Zitatstelle *", `301-305`)
        await fillInput(page, "Autor Literaturfundstelle", "Bilen, Ulviye")
        await fillInput(page, "Dokumenttyp Literaturfundstelle", "Ean")
        await page.getByText("Ean", { exact: true }).click()
        await waitForInputValue(
          page,
          "[aria-label='Dokumenttyp Literaturfundstelle']",
          "Anmerkung",
        )

        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await page.getByLabel("Treffer übernehmen").click()
        await expect(
          page.getByText(
            `Bilen, Ulviye, MMG 2024, 301-305${editionWithReferences.suffix} (Ean)`,
          ),
        ).toBeVisible()
      })

      await test.step("Check correct order", async () => {
        await page.reload()
        await expect(
          page.getByText(
            `Bilen, Ulviye, MMG 2024, 301-305${editionWithReferences.suffix} (Ean)`,
          ),
        ).toBeVisible()
        const correctOrder = [
          "MMG 2024, 12-22, Heft 1  (L)",
          "MMG 2024, 1-11, Heft 1",
          "MMG 2024, 300, Heft 1  (ST)",
          "Bilen, Ulviye, MMG 2024, 301-305, Heft 1  (Ean)",
        ]
        const summaries = await page.getByTestId("citation-summary").all()

        for (let i = 0; i < summaries.length; i++) {
          const text = summaries[i]
          await expect(text).toHaveText(correctOrder[i])
        }
      })
    },
  )

  async function searchForDocUnitWithFileNumber(
    page: Page,
    fileNumber: string,
    date: string,
  ) {
    await fillInput(page, "Gericht", "AG Aachen")
    await page.getByText("AG Aachen", { exact: true }).click()
    await fillInput(page, "Aktenzeichen", fileNumber)
    await fillInput(page, "Entscheidungsdatum", date)
    await fillInput(page, "Dokumenttyp", "AnU")
    await page.getByText("Anerkenntnisurteil", { exact: true }).click()

    await page.getByText("Suchen").click()
  }
})

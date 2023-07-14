import { expect } from "@playwright/test"
import {
  checkIfProceedingDecisionCleared,
  fillProceedingDecisionInputs,
  navigateToCategories,
  navigateToPublication,
  toggleProceedingDecisionsSection,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"
import { generateString } from "~/test-helper/dataGenerators"

test.describe("Search proceeding decisions", () => {
  test("search for existing proceeding decision and add", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToPublication(
      page,
      prefilledDocumentUnit.documentNumber || "",
    )

    await page
      .locator("[aria-label='Dokumentationseinheit veröffentlichen']")
      .click()
    await expect(page.locator("text=Email wurde versendet")).toBeVisible()

    await expect(page.locator("text=Xml Email Abgabe -")).toBeVisible()
    await expect(page.locator("text=in Veröffentlichung")).toBeVisible()

    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()
    await toggleProceedingDecisionsSection(page)

    await fillProceedingDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await page
      .getByRole("button", { name: "Nach Entscheidungen suchen" })
      .click()

    await expect(page.getByText("Total 1 Items")).toBeVisible()

    const result = page.locator(".table-row", {
      hasText: `AG Aachen, AnU, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}`,
    })
    await expect(result).toBeVisible()
    await result.locator("[aria-label='Treffer übernehmen']").click()

    await checkIfProceedingDecisionCleared(page)

    await expect(
      page.getByText(
        `AG Aachen, AnU, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}`,
      ),
    ).toHaveCount(2)

    await expect(page.getByText("Bereits hinzugefügt")).toBeVisible()

    await page.getByText("delete_outline").click()
    await expect(page.getByText("Bereits hinzugefügt")).toBeHidden()
  })

  test("search with no results", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()
    await toggleProceedingDecisionsSection(page)

    await fillProceedingDecisionInputs(page, {
      fileNumber: generateString(),
    })

    await page
      .getByRole("button", { name: "Nach Entscheidungen suchen" })
      .click()

    await expect(
      page.getByText("Suche hat keine Treffer ergeben"),
    ).toBeVisible()
  })
})

import { AxeBuilder } from "@axe-core/playwright"
import { expect } from "@playwright/test"
import { caselawTest as test } from "../../e2e/caselaw/fixtures"

test.describe("a11y of start page (/caselaw)", () => {
  test("documentUnit list", async ({ page }) => {
    await page.goto("/")
    await expect(
      page.getByRole("button", {
        name: "Neue Dokumentationseinheit",
        exact: true,
      }),
    ).toBeVisible()
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("delete documentUnit popup", async ({ page, documentNumber }) => {
    await page.goto("/")
    await expect(
      page.getByRole("button", {
        name: "Neue Dokumentationseinheit",
        exact: true,
      }),
    ).toBeVisible()
    await page.getByLabel("Dokumentnummer Suche").fill(documentNumber)
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(
      page.locator(".table-row", {
        hasText: documentNumber,
      }),
    ).toBeVisible()
    await page
      .locator(".table-row", {
        hasText: documentNumber,
      })
      .locator("[aria-label='Dokumentationseinheit löschen']")
      .click()
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

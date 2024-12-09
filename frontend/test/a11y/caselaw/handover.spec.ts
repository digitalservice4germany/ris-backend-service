import { expect } from "@playwright/test"
import { caselawTest as test } from "../../e2e/caselaw/fixtures"
import { useAxeBuilder } from "~/a11y/caselaw/a11y.utils"
import { navigateToHandover } from "~/e2e/caselaw/e2e-utils"

test.describe("a11y of handover page (/caselaw/documentunit/{documentNumber}/handover)", () => {
  test("handover", async ({ page, documentNumber }) => {
    await navigateToHandover(page, documentNumber)
    const accessibilityScanResults = await useAxeBuilder(page).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("handover not possible", async ({ page, documentNumber }) => {
    await navigateToHandover(page, documentNumber)
    await expect(
      page.getByLabel("Dokumentationseinheit an jDV übergeben"),
    ).toBeDisabled()
    await expect(page.getByText("Email versendet.")).toBeHidden()

    const accessibilityScanResults = await useAxeBuilder(page).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

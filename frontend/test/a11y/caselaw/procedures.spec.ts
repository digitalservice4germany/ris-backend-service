import { AxeBuilder } from "@axe-core/playwright"
import { expect } from "@playwright/test"
import { navigateToProcedures } from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"

test.describe("a11y of procedures page (/caselaw/procedures)", () => {
  test("procedures list", async ({ page }) => {
    await navigateToProcedures(page)

    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

import { AxeBuilder } from "@axe-core/playwright"
import { expect } from "@playwright/test"
import {
  navigateToCategories,
  save,
  fillPreviousDecisionInputs,
} from "../../e2e/caselaw/e2e-utils"
import { caselawTest as test } from "../../e2e/caselaw/fixtures"

test.describe("a11y of categories page (/caselaw/documentunit/{documentNumber}/categories)", () => {
  test("first load", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("gericht", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    await page
      .locator("#coreData div")
      .filter({ hasText: "Gericht * Fehlerhaftes" })
      .getByLabel("Dropdown öffnen")
      .click()
    await expect(
      page.locator("[aria-label='dropdown-option'] >> nth=9"),
    ).toBeVisible()

    await expect(page.getByText("AG Aachen")).toBeVisible()
    await expect(page.getByText("AG Aalen")).toBeVisible()
    await page.locator("[aria-label='Gericht']").fill("bayern")
    await expect(page.locator("[aria-label='Gericht']")).toHaveValue("bayern")
    await expect(
      page.locator("[aria-label='dropdown-option'] >> nth=2"),
    ).toBeVisible()
    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("aktenzeichen", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").type("testone")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").type("testtwo")
    await page.keyboard.press("Enter")

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen anzeigen']")
      .click()

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen']")
      .type("testthree")

    await page.keyboard.press("Enter")

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("dokumenttyp", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page
      .locator("#coreData div")
      .filter({ hasText: "Spruchkörper Dokumenttyp" })
      .getByLabel("Dropdown öffnen")
      .click()

    await expect(
      page.locator("[aria-label='dropdown-option']"),
    ).not.toHaveCount(0)

    // type search string: 3 results for "zwischen"
    await page.locator("[aria-label='Dokumenttyp']").fill("zwischen")
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue(
      "zwischen",
    )
    await expect(page.locator("[aria-label='dropdown-option']")).toHaveCount(3)

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("ecli", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='ECLI']").type("one")

    await page.locator("[aria-label='Abweichender ECLI anzeigen']").click()

    await page.locator("[aria-label='Abweichender ECLI']").type("two")
    await page.keyboard.press("Enter")
    await page.locator("[aria-label='Abweichender ECLI']").type("three")
    await page.keyboard.press("Enter")

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("vorgang", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Vorgang']").fill("test Vorgang")
    await page.getByLabel("Vorgang", { exact: true }).press("ArrowDown")
    await page.getByLabel("dropdown-option").press("Enter")

    await save(page)

    await page.locator("[aria-label='Vorgang']").click()
    await expect(
      page.locator("[aria-label='additional-dropdown-info']"),
    ).toBeVisible()
    await expect(page.getByText("1 Dokumentationseinheiten")).toBeVisible()

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])

    // cleanup Vorgang again
    const response = await page.request.get(
      `/api/v1/caselaw/procedure?sz=10&pg=0&q=${"test Vorgang"}`,
    )
    const responseBody = await response.json()
    const uuid = responseBody.content[0].id

    const cookies = await page.context().cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const deleteResponse = await page.request.delete(
      `/api/v1/caselaw/procedure/${uuid}`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    expect(deleteResponse.ok()).toBeTruthy()
  })

  test("rechtskraft", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    await page.locator("[aria-label='Rechtskraft']").click()

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("proceeding decision", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await fillPreviousDecisionInputs(page, {
      court: "AG Aalen",
      decisionDate: "03.12.2004",
      fileNumber: "1a2b3c",
    })
    await save(page)

    await page.reload()
    await fillPreviousDecisionInputs(page, {
      decisionDate: "03.12.2004",
    })

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("text editor", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const editorField = page.locator("[data-testid='Leitsatz'] >> div")
    await editorField.click()
    await editorField.type("this is text")

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("schlagwörter", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Schlagwörter']").fill("one")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Schlagwörter']").fill("two")
    await page.keyboard.press("Enter")

    const accessibilityScanResults = await new AxeBuilder({ page })
      .disableRules(["duplicate-id-aria"])
      .analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

import { expect } from "@playwright/test"
import {
  clickCategoryButton,
  navigateToCategories,
  save,
  navigateToHandover,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

test.describe(
  "Indent text",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4117",
    },
  },
  () => {
    test("Enter text with indentation, check that correct style is applied", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const noIndentation = `<p>Abschnitt mit Einzug 0</p>`
      const singleIndentation = `<p style="margin-left: 40px!important;">Abschnitt mit Einzug 1</p>`
      const doubleIndentation = `<p style="margin-left: 80px!important;">Abschnitt mit Einzug 2</p>`
      const tripleIndentation = `<p style="margin-left: 120px!important;">Abschnitt mit Einzug 3</p>`

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page.keyboard.type("Abschnitt mit Einzug 0")
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 1")
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 2")
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 3")
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()

      // hide invisible characters
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()

      const inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML).toContain(singleIndentation)
      expect(inputFieldInnerHTML).toContain(doubleIndentation)
      expect(inputFieldInnerHTML).toContain(tripleIndentation)
      expect(inputFieldInnerHTML).toContain(noIndentation)

      await save(page)

      await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
      await expect(page.getByText("XML Vorschau")).toBeVisible()
      await page.getByText("XML Vorschau").click()

      await expect(page.getByText(noIndentation)).toBeVisible()
      await expect(page.getByText(singleIndentation)).toBeVisible()
      await expect(page.getByText(doubleIndentation)).toBeVisible()
      await expect(page.getByText(tripleIndentation)).toBeVisible()
    })

    test("Check styling for indent and outdent text editor interactions", async ({
      page,
      documentNumber,
    }) => {
      const noIndentation = `<p>Abschnitt mit Einzug</p>`
      const singleIndentation = `<p style="margin-left: 40px!important;">Abschnitt mit Einzug</p>`
      const tripleIndentation = `<p style="margin-left: 120px!important;">Abschnitt mit Einzug</p>`

      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()
      await page.keyboard.type("Abschnitt mit Einzug")

      let inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(noIndentation)

      await inputField.click()
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(singleIndentation)

      await inputField.click()
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()
      await page
        .locator(`[aria-label='Einzug vergrößern']:not([disabled])`)
        .click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(tripleIndentation)

      await inputField.click()
      await page
        .locator(`[aria-label='Einzug verringern']:not([disabled])`)
        .click()
      await page
        .locator(`[aria-label='Einzug verringern']:not([disabled])`)
        .click()
      await page
        .locator(`[aria-label='Einzug verringern']:not([disabled])`)
        .click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(noIndentation)
    })
  },
)

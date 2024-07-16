import { expect } from "@playwright/test"
import { navigateToCategories } from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"
import { navigateToHandover } from "~/e2e/caselaw/e2e-utils"

test.describe(
  "Indent text",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4119",
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

      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page.keyboard.type("Abschnitt mit Einzug 0")
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 1")
      await page.getByLabel("indent").click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 2")
      await page.getByLabel("indent").click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Abschnitt mit Einzug 3")
      await page.getByLabel("indent").click()

      // hide invisible characters
      await page.getByLabel("invisible-characters").click()

      const inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML.includes(singleIndentation)).toBeTruthy()
      expect(inputFieldInnerHTML.includes(doubleIndentation)).toBeTruthy()
      expect(inputFieldInnerHTML.includes(tripleIndentation)).toBeTruthy()
      expect(inputFieldInnerHTML.includes(noIndentation)).toBeTruthy()

      await page.getByText("Speichern").click()
      await page.waitForEvent("requestfinished", { timeout: 5_000 })

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

      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page.getByLabel("invisible-characters").click()
      await page.keyboard.type("Abschnitt mit Einzug")

      let inputFieldInnerHTML = await inputField.innerHTML()
      console.log(inputFieldInnerHTML)
      expect(inputFieldInnerHTML.includes(noIndentation)).toBeTruthy()

      await inputField.click()
      await page.getByLabel("indent").click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML.includes(singleIndentation)).toBeTruthy()

      await inputField.click()
      await page.getByLabel("indent").click()
      await page.getByLabel("indent").click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML.includes(tripleIndentation)).toBeTruthy()

      await inputField.click()
      await page.getByLabel("outdent").click()
      await page.getByLabel("outdent").click()
      await page.getByLabel("outdent").click()

      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML.includes(noIndentation)).toBeTruthy()
    })
  },
)

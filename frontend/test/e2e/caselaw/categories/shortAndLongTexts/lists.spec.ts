import { expect } from "@playwright/test"
import {
  clickCategoryButton,
  navigateToCategories,
  save,
  navigateToHandover,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

test.describe(
  "Create lists",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4112",
    },
  },
  () => {
    test("Enter text and make it into a bullet list, check that list style is applied", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const bulletList = `<ul class="list-disc"><li><p>This is a bullet list</p></li><li><p>Second bullet list item</p></li></ul>`
      const bulletListXMLPreview = `<ul class="list-disc">`

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page.keyboard.type("This is a bullet list")
      await page
        .locator(`[aria-label='Aufzählungsliste']:not([disabled])`)
        .click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Second bullet list item")

      // hide invisible characters
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()

      const inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML).toContain(bulletList)
      await save(page)

      await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
      await expect(page.getByText("XML Vorschau")).toBeVisible()
      await page.getByText("XML Vorschau").click()

      await expect(page.getByText(bulletListXMLPreview)).toBeVisible()
    })

    test("Enter text and make it into an ordered list, check that list style is applied", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const orderedList = `<ol class="list-decimal"><li><p>This is an ordered list</p></li><li><p>Second ordered list item</p></li></ol>`
      const orderedListXMLPreview = `<ol class="list-decimal">`

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      await page.keyboard.type("This is an ordered list")
      await page
        .locator(`[aria-label='Nummerierte Liste']:not([disabled])`)
        .click()
      await page.keyboard.press("Enter")
      await page.keyboard.type("Second ordered list item")

      // hide invisible characters
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()

      const inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML).toContain(orderedList)

      await save(page)

      await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
      await expect(page.getByText("XML Vorschau")).toBeVisible()
      await page.getByText("XML Vorschau").click()

      await expect(page.getByText(orderedListXMLPreview)).toBeVisible()
    })

    test("Switch between list types", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const bulletList = `<ul class="list-disc"><li><p>This is a list</p></li></ul>`
      const orderedList = `<ol class="list-decimal"><li><p>This is a list</p></li></ol>`
      const noList = `<p>This is a list</p>`

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      // hide invisible characters
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()

      await page.keyboard.type("This is a list")
      await page
        .locator(`[aria-label='Aufzählungsliste']:not([disabled])`)
        .click()

      let inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML).toContain(bulletList)

      await page
        .locator(`[aria-label='Nummerierte Liste']:not([disabled])`)
        .click()
      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(orderedList)

      await page
        .locator(`[aria-label='Nummerierte Liste']:not([disabled])`)
        .click()
      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(noList)
    })

    test("Toggle list types with keyboard shortcut", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const bulletList = `<ul class="list-disc"><li><p>This is a list</p></li></ul>`
      const orderedList = `<ol class="list-decimal"><li><p>This is a list</p></li></ol>`
      const noList = `<p>This is a list</p>`

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await clickCategoryButton("Gründe", page)
      const inputField = page.locator("[data-testid='Gründe']")
      await inputField.click()
      // hide invisible characters
      await page
        .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
        .click()

      await page.keyboard.type("This is a list")
      await page.keyboard.press(`ControlOrMeta+Shift+8`)

      let inputFieldInnerHTML = await inputField.innerHTML()
      // Check text styling
      expect(inputFieldInnerHTML).toContain(bulletList)

      await page.keyboard.press(`ControlOrMeta+Shift+7`)
      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(orderedList)

      await page.keyboard.press(`ControlOrMeta+Shift+7`)
      inputFieldInnerHTML = await inputField.innerHTML()
      expect(inputFieldInnerHTML).toContain(noList)
    })
  },
)

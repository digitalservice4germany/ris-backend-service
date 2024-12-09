import { expect } from "@playwright/test"
import {
  clickCategoryButton,
  navigateToCategories,
  navigateToHandover,
  save,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

test.describe(
  "Create table",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4121",
    },
    tag: ["@RISDEV-4121"],
  },
  () => {
    test("Click table button and check that menu buttons are complete", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const tableHTML =
        '<table style="min-width: 25px;"><colgroup><col style="min-width: 25px;"></colgroup>' +
        '<tbody><tr><th class="invisible-table" style="" colspan="1" rowspan="1">' +
        "<p>some text so the table is exportable</p></th></tr></tbody></table>"
      const tableXMLPreview = `<table class="invisible-table" style="min-width: 25px">`
      const inputField = page.getByTestId("Gründe")
      const menu = page.getByLabel("Gründe Button Leiste")

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("Click table button in reasons", async () => {
        await clickCategoryButton("Gründe", page)
        await inputField.click()
        await menu.locator(`[aria-label='Tabelle']:not([disabled])`).click()
      })

      await test.step("Check all table menu buttons are visible", async () => {
        await expect(menu.getByLabel("Tabelle", { exact: true })).toBeVisible()
        await expect(menu.getByLabel("Tabelle löschen")).toBeVisible()
        await expect(menu.getByLabel("Zeile darunter einfügen")).toBeVisible()
        await expect(menu.getByLabel("Zeile löschen")).toBeVisible()
        await expect(menu.getByLabel("Spalte rechts einfügen")).toBeVisible()
        await expect(menu.getByLabel("Spalte löschen")).toBeVisible()
      })

      await test.step("Add table, remove two rows and two columns and check the new table is correct", async () => {
        await menu.getByLabel("Tabelle einfügen").click()
        await page.keyboard.type("some text so the table is exportable")
        await page.keyboard.press("ArrowRight")
        await menu.getByLabel("Tabelle", { exact: true }).click()
        await menu.getByLabel("Spalte löschen").click()
        await menu.getByLabel("Tabelle", { exact: true }).click()
        await menu.getByLabel("Spalte löschen").click()
        await page.keyboard.press("ArrowDown")
        await menu.getByLabel("Tabelle", { exact: true }).click()
        await menu.getByLabel("Zeile löschen").click()
        await menu.getByLabel("Tabelle", { exact: true }).click()
        await menu.getByLabel("Zeile löschen").click()
        await menu.getByLabel("Nicht-druckbare Zeichen").click()
        const inputFieldInnerHTML = await inputField.innerHTML()
        expect(inputFieldInnerHTML).toContain(tableHTML)
      })

      await save(page)

      await test.step("Check table is visible in the XML Vorschau", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await expect(page.getByText("XML Vorschau")).toBeVisible()
        await page.getByText("XML Vorschau").click()

        await expect(page.getByText(tableXMLPreview)).toBeVisible()
      })
    })
  },
)

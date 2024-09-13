import { AxeBuilder } from "@axe-core/playwright"
import { expect } from "@playwright/test"
import { caselawTest as test } from "../../e2e/caselaw/fixtures"
import {
  navigateToAttachments,
  navigateToHandover,
  uploadTestfile,
} from "~/e2e/caselaw/e2e-utils"

test.describe("a11y of document page (/caselaw/documentunit/{documentNumber}/files)", () => {
  test("upload document", async ({ page, documentNumber }) => {
    await navigateToAttachments(page, documentNumber)

    const tableView = page.getByRole("cell", {
      name: "Hochgeladen am",
      exact: true,
    })

    await uploadTestfile(page, "sample.docx")
    await expect(tableView).toBeVisible()
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("delete document", async ({ page, documentNumber }) => {
    await navigateToAttachments(page, documentNumber)
    const tableView = page.getByRole("cell", {
      name: "Hochgeladen am",
      exact: true,
    })
    await uploadTestfile(page, "sample.docx")
    await page.getByLabel("Datei löschen").click()
    await page.getByRole("button", { name: "Löschen", exact: true }).click()
    await expect(tableView).toBeHidden()

    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("delete document modal", async ({ page, documentNumber }) => {
    await navigateToAttachments(page, documentNumber)
    await uploadTestfile(page, "sample.docx")
    await page.getByLabel("Datei löschen").click()
    await page.locator("[aria-label='Anhang löschen']").click()

    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("upload non-docx file per file chooser", async ({
    page,
    documentNumber,
  }) => {
    await navigateToAttachments(page, documentNumber)
    await uploadTestfile(page, "sample.png")
    await expect(
      page.locator(
        "text=sample.png hat ein falsches Format. Laden Sie eine .docx-Version hoch.",
      ),
    ).toBeVisible()

    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

test.describe("a11y of handover page (/caselaw/documentunit/{documentNumber}/handover)", () => {
  test("handover", async ({ page, documentNumber }) => {
    await navigateToHandover(page, documentNumber)
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })

  test("handover not possible", async ({ page, documentNumber }) => {
    await navigateToHandover(page, documentNumber)
    await page
      .locator("[aria-label='Dokumentationseinheit an jDV übergeben']")
      .click()
    await expect(
      page.getByText("Es sind noch nicht alle Pflichtfelder befüllt."),
    ).toBeVisible()

    const accessibilityScanResults = await new AxeBuilder({ page }).analyze()
    expect(accessibilityScanResults.violations).toEqual([])
  })
})

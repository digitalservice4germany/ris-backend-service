import { expect } from "@playwright/test"
import { deleteDocumentUnit } from "./e2e-utils"
import { caselawTest as test } from "./fixtures"

test.describe("create a doc unit and delete it again", () => {
  test("create and delete new doc unit", async ({ page }) => {
    await page.goto("/")
    await page
      .getByRole("button", { name: "Neue Dokumentationseinheit", exact: true })
      .click()
    await expect(page.locator(`text=Oder hier auswählen`)).toBeVisible()
    await expect(page).toHaveURL(
      /\/caselaw\/documentunit\/[A-Z0-9]{13}\/files$/,
    )

    // Given the earlier expectation we can assume that the regex will match...
    const documentNumber = /caselaw\/documentunit\/(.*)\/files/g.exec(
      page.url(),
    )?.[1] as string

    await deleteDocumentUnit(page, documentNumber)
  })

  test("cancel delete doc unit", async ({ page, documentNumber }) => {
    await page.goto("/")
    await page.getByLabel("Dokumentnummer Suche").fill(documentNumber)
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    //TODO: remove the timeout when search performance get better
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
    await page.locator('button:has-text("Abbrechen")').click()
    await expect(
      page.locator(
        `a[href*="/caselaw/documentunit/${documentNumber}/categories"]`,
      ),
    ).toBeVisible()
  })
})

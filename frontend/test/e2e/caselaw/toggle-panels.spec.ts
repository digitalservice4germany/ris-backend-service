import { expect } from "@playwright/test"
import { navigateToCategories } from "./e2e-utils"
import { caselawTest as test } from "./fixtures"

test.describe("test the different layout options", () => {
  test("ensure default layout", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByLabel("Navigation schließen")).toBeVisible()
    await expect(page.getByLabel("Originaldokument öffnen")).toBeVisible()
    await expect(
      page.locator("text=Es wurde noch kein Originaldokument hochgeladen"),
    ).toBeHidden()
  })

  test("open and close original document panel without attached files", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByLabel("Originaldokument öffnen").click()
    await expect(
      page.locator("text=Es wurde noch kein Originaldokument hochgeladen"),
    ).toBeVisible()
    await expect(page).toHaveURL(/showDocPanel=true/)
  })

  test("close and open navigation sidebar", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByLabel("Navigation schließen").click()
    await expect(page).toHaveURL(/showNavBar=false/)
    await expect(page.locator("aside", { hasText: "Zurück" })).toBeHidden()

    await page.getByLabel("Navigation öffnen").click()
    await expect(page).toHaveURL(/showNavBar=true/)
    await expect(page.locator("aside", { hasText: "Zurück" })).toBeVisible()
  })

  test("persist toggle queries for new pages", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByLabel("Originaldokument öffnen").click()
    await expect(page).toHaveURL(/showDocPanel=true/)

    await page.getByLabel("Navigation schließen").click()
    await expect(page).toHaveURL(/showNavBar=false/)

    await page.locator("a >> text=Zum Upload").click()
    await expect(page.getByText("Datei in diesen Bereich ziehen")).toBeVisible()
    await expect(page).toHaveURL(/showDocPanel=true/)
    await expect(page).toHaveURL(/showNavBar=false/)
  })
})

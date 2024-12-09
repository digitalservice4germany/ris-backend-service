import { expect } from "@playwright/test"
import { navigateToCategories, navigateToAttachments } from "./e2e-utils"
import { caselawTest as test } from "./fixtures"

test.describe("test navigation panel", () => {
  test("navigation panel reacts to route parameters", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByLabel("Navigation schließen").click()
    await expect(page).toHaveURL(/showNavigationPanel=false/)
    await expect(page.locator("aside", { hasText: "Rubriken" })).toBeHidden()

    await page.getByLabel("Navigation öffnen").click()
    await expect(page).toHaveURL(/showNavigationPanel=true/)
    await expect(page.getByRole("link", { name: "Rubriken" })).toBeVisible()
  })

  test("navigation toggle state is passed to other pages", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByLabel("Navigation schließen").click()
    await expect(page).toHaveURL(/showNavigationPanel=false/)

    await navigateToAttachments(page, documentNumber)
    await expect(page.locator("aside", { hasText: "Rubriken" })).toBeHidden()
    await expect(page).toHaveURL(/showNavigationPanel=false/)
  })

  test("keyboard accessibility", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    await page.getByRole("button", { name: "Navigation schließen" }).click()
    await expect(
      page.getByRole("button", { name: "Navigation öffnen" }),
    ).toBeFocused()
    await page.keyboard.press("Enter")
    await expect(
      page.getByRole("button", { name: "Navigation schließen" }),
    ).toBeFocused()
    await page.keyboard.press("Enter")
    await expect(
      page.getByRole("button", { name: "Navigation öffnen" }),
    ).toBeFocused()
    await page.keyboard.press("Enter")
    await expect(
      page.getByRole("button", { name: "Navigation schließen" }),
    ).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(page.getByRole("link", { name: "Rubriken" })).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(page.getByRole("link", { name: "Stammdaten" })).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(page.getByRole("link", { name: "Rechtszug" })).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(
      page.getByRole("link", { name: "Inhaltliche Erschließung" }),
    ).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(
      page.getByRole("link", { name: "Kurz- & Langtexte" }),
    ).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(page.getByRole("link", { name: "Dokumente" })).toBeFocused()
    await page.keyboard.press("Enter")
    await expect(
      page.getByText("Ziehen Sie Ihre Dateien in diesen Bereich."),
    ).toBeVisible()
    await page.keyboard.press("Tab")
    await expect(page.getByRole("link", { name: "Fundstellen" })).toBeFocused()
    await page.keyboard.press("Tab")
    await expect(
      page.getByRole("link", { name: "Übergabe an jDV" }),
    ).toBeFocused()
    await page.keyboard.press("Enter")
    await expect(page.getByText("Plausibilitätsprüfung")).toBeVisible()
  })
})

import { expect, test } from "@playwright/test"
import { checkA11y, injectAxe } from "axe-playwright"
import { importTestData } from "~/e2e/norms/e2e-utils"
import { testWithImportedNorm } from "~/e2e/norms/fixtures"
import { norm_with_structure } from "~/e2e/norms/testdata/norm_with_structure"

test.describe("a11y of norms list page (/norms)", () => {
  test("norms list", async ({ page }) => {
    await page.goto("/norms")
    await expect(page.locator("text=Dokumentationseinheiten")).toBeVisible()
    await injectAxe(page)
    await checkA11y(page)
  })
})

test.describe("a11y of a norm frame data (/norms/norm/{guid}/frame)", () => {
  testWithImportedNorm("norm frame data", async ({ page, guid }) => {
    await page.goto(`/norms/norm/${guid}/frame`)
    await expect(
      page.locator("text=Dokumentation des Rahmenelements"),
    ).toBeVisible()

    // eslint-disable-next-line playwright/no-element-handle
    const sections = await page.$$('[test-id="a11y-expandable-dataset"]')
    for (const section of sections) {
      const aufklappenButton = await section.$('[aria-label="Aufklappen"]')
      if (aufklappenButton) {
        await aufklappenButton.click()

        const editButtons = await section.$$(
          '[aria-label="Eintrag bearbeiten"]',
        )
        if (editButtons.length > 0) {
          await editButtons[0].click()
        }
      }
    }

    await injectAxe(page)
    await checkA11y(page)
  })
})

test.describe("a11y of a norm export (/norms/norm/{guid}/export)", () => {
  testWithImportedNorm("norm export", async ({ page, guid }) => {
    await page.goto(`/norms/norm/${guid}/export`)
    await expect(
      page.locator(
        "text=Exportieren Sie die Dokumentationseinheit zur Abgabe an die jDV.",
      ),
    ).toBeVisible()
    await injectAxe(page)
    await checkA11y(page)
  })
})

test.describe("a11y of a norm content (/norms/norm/{guid}/content)", () => {
  test("norm content", async ({ page, request }) => {
    const { guid } = await importTestData(request, norm_with_structure)
    await page.goto(`/norms/norm/${guid}/content`)
    await expect(
      page.locator("text=Nichtamtliches Inhaltsverzeichnis"),
    ).toBeVisible()
    await injectAxe(page)
    await checkA11y(page)
  })
})

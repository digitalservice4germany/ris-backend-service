import { expect } from "@playwright/test"
import {
  navigateToCategories,
  waitForInputValue,
  fillActiveCitationInputs,
  save,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"
import { generateString } from "~/test-helper/dataGenerators"

test.describe("saving behaviour", () => {
  test("could not update documentUnit", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.route("**/*", async (route) => {
      await route.abort("internetdisconnected")
    })

    await page.locator("[aria-label='Spruchkörper']").fill("VG-001")
    await page.locator("[aria-label='Speichern Button']").click()

    await expect(page.getByText("Fehler beim Speichern")).toBeVisible()
  })

  test("input during save not lost", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Spruchkörper']").fill("VG-001")
    await waitForInputValue(page, "[aria-label='Spruchkörper']", "VG-001")

    await save(page)

    await expect(page.locator("[aria-label='Spruchkörper']")).toHaveValue(
      "VG-001",
    )
  })

  test("input not lost on page unload (autosave not triggered yet)", async ({
    page,
    documentNumber,
    browser,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Spruchkörper']").fill("VG-001")
    await waitForInputValue(page, "[aria-label='Spruchkörper']", "VG-001")

    await expect(page.getByText(/Zuletzt .* Uhr/)).toBeHidden()

    // Close the tab -> We expect the document unit to be saved
    await page.close({ runBeforeUnload: true })

    const newPage = await browser.newPage()

    await navigateToCategories(newPage, documentNumber)

    await expect(newPage.locator("[aria-label='Spruchkörper']")).toHaveValue(
      "VG-001",
      { timeout: 500 },
    )
  })

  test("removing last chip element not possible", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").type(generateString())
    await page.keyboard.press("Enter")

    const labels = [
      "Abweichendes Aktenzeichen",
      // "Abweichendes Entscheidungsdatum",
      "Abweichender ECLI",
      "Fehlerhaftes Gericht",
    ]

    for (const label of labels) {
      await page.locator(`[aria-label='${label} anzeigen']`).click()
      await page.locator(`[aria-label='${label}']`).type("22.11.2023")
      await page.keyboard.press("Enter")
    }

    await save(page)

    while ((await page.locator("[data-testid='chip'] > button").count()) > 0) {
      await page.locator("[data-testid='chip'] > button").first().click()
    }

    await save(page)
    await expect(page.locator("[data-testid='chip']")).toHaveCount(0)
  })

  test("change Spruchkörper two times, saving after each change", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Spruchkörper']").fill("VG-001")
    await waitForInputValue(page, "[aria-label='Spruchkörper']", "VG-001")

    await save(page)

    await page.reload()
    await page.locator("[aria-label='Spruchkörper']").fill("VG-002")
    await waitForInputValue(page, "[aria-label='Spruchkörper']", "VG-002")

    await save(page)

    await page.reload()
    await expect(page.locator("[aria-label='Spruchkörper']")).toHaveValue(
      "VG-002",
    )
  })

  test("change Aktivzitierung to test saving of nested elements", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await fillActiveCitationInputs(page, {
      documentType: "Anerkenntnisurteil",
    })
    await page.getByLabel("Aktivzitierung speichern").click()

    await save(page)
    await page.reload()
    await expect(page.getByText("Anerkenntnisurteil")).toBeVisible()
  })

  test("saved changes also visible in document unit entry list", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    const fileNumber = generateString()

    await page.locator("[aria-label='Aktenzeichen']").fill(fileNumber)
    await page.keyboard.press("Enter")

    await save(page)
    await page.goto("/")
    await page.getByLabel("Aktenzeichen Suche").fill(fileNumber)
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect(
      page.locator(".table-row", {
        hasText: documentNumber,
      }),
    ).toBeVisible()
    await expect(
      page.locator(".table-row", {
        hasText: documentNumber,
      }),
    ).toBeVisible()
    await expect(
      page.locator(".table-row", {
        hasText: fileNumber,
      }),
    ).toBeVisible()
  })

  test(
    "fields that are filled while a save request is in progress are not overwritten",
    {
      annotation: {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4521",
      },
    },
    async ({ page, documentNumber }) => {
      await navigateToCategories(page, documentNumber)

      const { promise: lock, resolve: releaseLock } =
        Promise.withResolvers<void>()

      // We want to simulate behavior during an ongoing save request, so we delay the request.
      await page.route("**/api/v1/caselaw/documentunits/*", async (route) => {
        // Block all docunit network traffic until lock is released.
        await lock
        await route.continue()
      })

      await page.locator("[aria-label='Spruchkörper']").fill("VG-001")
      await page.locator("[aria-label='Speichern Button']").click()

      // Fill an input while a save request is in progress
      await page.getByLabel("ECLI", { exact: true }).fill("ECLI-123")

      // Wait until the save request is finished
      releaseLock()
      await expect(page.getByText("speichern...")).toBeHidden()

      // Change a third input and save it
      await page
        .getByLabel("Entscheidungsdatum", { exact: true })
        .fill("01012020")
      await save(page)

      // After the page reload, the three changed inputs should still be filled.
      await page.reload()

      await expect(page.locator("[aria-label='Spruchkörper']")).toHaveValue(
        "VG-001",
      )
      await expect(page.getByLabel("ECLI", { exact: true })).toHaveValue(
        "ECLI-123",
      )
      await expect(
        page.getByLabel("Entscheidungsdatum", { exact: true }),
      ).toHaveValue("01.01.2020")
    },
  )
})

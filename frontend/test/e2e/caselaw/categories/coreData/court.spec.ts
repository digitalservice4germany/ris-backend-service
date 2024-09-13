import { expect } from "@playwright/test"
import { navigateToCategories, waitForInputValue, save } from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

test.describe("court", () => {
  test("input value in court field, press enter and reload", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Gericht']").fill("BGH")
    await waitForInputValue(page, "[aria-label='Gericht']", "BGH")
    await expect(page.getByText("BGH")).toBeVisible()
    await page.getByText("BGH").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "BGH")

    await save(page)

    await page.reload()
    await page.locator("[aria-label='Gericht']").focus()
    // Todo: flaky in chromium
    // await expect(page.locator("[aria-label='Gericht']")).toHaveValue("BGH")
  })

  test("open incorrect court field, input one, save and reload", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Fehlerhaftes Gericht anzeigen']").click()

    await page.locator("[aria-label='Fehlerhaftes Gericht']").type("abc")

    await expect(
      page.locator("[aria-label='Fehlerhaftes Gericht']"),
    ).toHaveValue("abc")

    await page.keyboard.press("Enter")
    await save(page)

    await page.reload()

    await page.locator("[aria-label='Fehlerhaftes Gericht anzeigen']").click()
    await expect(page.getByText("abc").first()).toBeVisible()
  })

  test("open incorrect court field, input two, save, reload, remove first, save and reload", async ({
    page,
    documentNumber,
  }) => {
    test.slow()
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Fehlerhaftes Gericht anzeigen']").click()
    await page
      .locator("[aria-label='Fehlerhaftes Gericht']")
      .type("incorrectCourt1")
    await page.keyboard.press("Enter")
    await page
      .locator("[aria-label='Fehlerhaftes Gericht']")
      .type("incorrectCourt2")
    await page.keyboard.press("Enter")

    await save(page)
    await page.reload()

    await page.locator("[aria-label='Fehlerhaftes Gericht anzeigen']").click()
    await expect(page.getByText("IncorrectCourt1")).toBeVisible()
    await expect(page.getByText("IncorrectCourt2")).toBeVisible()

    await page
      .locator(":text('incorrectCourt1') + button[aria-label='Löschen']")
      .click()

    await save(page)
    await page.reload()

    await page.locator("[aria-label='Fehlerhaftes Gericht anzeigen']").click()
    await expect(page.getByText("IncorrectCourt1")).toHaveCount(0)
    await expect(page.getByText("IncorrectCourt2")).toBeVisible()
  })

  test("court dropdown", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const minTotalCourts = 9

    // on start: closed dropdown, no input text
    await waitForInputValue(page, "[aria-label='Gericht']", "")
    await expect(page.getByText("AG Aachen")).toBeHidden()
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()

    // open dropdown
    await page
      .locator("#coreData div")
      .filter({ hasText: "Gericht * Fehlerhaftes" })
      .getByLabel("Dropdown öffnen")
      .click()
    await expect(
      page.locator("[aria-label='dropdown-option'] >> nth=" + minTotalCourts),
    ).toBeVisible()
    await expect(page.getByText("AG Aachen")).toBeVisible()
    await expect(page.getByText("AG Aalen")).toBeVisible()

    // type search string: 3 results for "bayern"
    await page.locator("[aria-label='Gericht']").fill("bayern")
    await waitForInputValue(page, "[aria-label='Gericht']", "bayern")
    // Todo: flaky in chromium
    // await expect(page.locator("[aria-label='dropdown-option']")).toHaveCount(3)

    // use the clear icon
    await page.locator("[aria-label='Auswahl zurücksetzen']").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "")
    await expect(
      page.locator("[aria-label='dropdown-option'] >> nth=" + minTotalCourts),
    ).toBeVisible()

    // close dropdown
    await page.getByLabel("Dropdown schließen").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "")
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()

    // open dropdown again by focussing
    await page.locator("[aria-label='Gericht']").focus()
    await expect(
      page.locator("[aria-label='dropdown-option'] >> nth=" + minTotalCourts),
    ).toBeVisible()

    // close dropdown using the esc key, user input text gets removed and last saved value restored
    await page.keyboard.down("Escape")
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()
    await waitForInputValue(page, "[aria-label='Gericht']", "")
  })

  test("correct esc/tab behaviour in court dropdown", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await waitForInputValue(page, "[aria-label='Gericht']", "")
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()

    await page.locator("[aria-label='Gericht']").fill("BVerfG")
    await page.getByText("BVerfG").click()

    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()
    await expect(page.locator("[aria-label='Gericht']")).toHaveValue("BVerfG")

    await page.locator("[aria-label='Gericht']").fill("BGH")

    await expect(page.locator("[aria-label='dropdown-option']")).toHaveCount(1)

    await page.keyboard.press("Escape") // reset to last saved value

    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()
    await expect(page.locator("[aria-label='Gericht']")).toHaveValue("BVerfG")

    await page.locator("[aria-label='Gericht']").fill("BGH")
    await page.keyboard.press("Tab") // reset to last saved value

    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()
    await expect(page.locator("[aria-label='Gericht']")).toHaveValue("BVerfG")
  })

  test("that setting a court sets the region automatically", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Gericht']").fill("aalen")

    // clicking on dropdown item triggers auto save
    await page.getByText("AG Aalen").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "AG Aalen")

    await save(page)

    await expect(page.getByText("Region")).toBeVisible()

    // region was set by the backend based on state database table
    await waitForInputValue(page, "[aria-label='Region']", "BW")
    await page.reload()
    // clear the court
    await page.locator("[aria-label='Auswahl zurücksetzen']").click()
    await expect(page.getByText("AG Aalen")).toBeHidden()
    await waitForInputValue(page, "[aria-label='Gericht']", "")

    await save(page)

    await expect(page.getByText("Region")).toBeVisible()
    // region was cleared by the backend
    await waitForInputValue(page, "[aria-label='Region']", "")
  })

  test("that setting a special court sets legal effect to yes, but it can be changed afterwards", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await waitForInputValue(page, "select#legalEffect", "Keine Angabe")
    await page.locator("[aria-label='Gericht']").fill("bgh")
    await page.getByText("BGH").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "BGH")

    // FIXME: Remove this save, when bug RISDEV-4480 is resolved
    await save(page)

    await waitForInputValue(page, "select#legalEffect", "Ja", 500)
    await page
      .getByRole("combobox", { name: "Rechtskraft" })
      .selectOption("Nein")
    await waitForInputValue(page, "select#legalEffect", "Nein")
    await expect(page.getByLabel("Rechtskraft")).toHaveValue("Nein")
  })

  test("that setting a non-special court leaves legal effect unchanged", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await waitForInputValue(page, "select#legalEffect", "Keine Angabe")

    await page.locator("[aria-label='Gericht']").fill("aachen")
    await page.getByText("AG Aachen").click()
    await waitForInputValue(page, "[aria-label='Gericht']", "AG Aachen")
    await save(page)

    await waitForInputValue(page, "select#legalEffect", "Keine Angabe")
    await expect(
      page.getByRole("combobox", { name: "Rechtskraft" }),
    ).toHaveValue("Keine Angabe")
  })
})

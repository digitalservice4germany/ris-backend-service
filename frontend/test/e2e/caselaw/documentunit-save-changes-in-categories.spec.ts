import { expect, Page } from "@playwright/test"
import { generateString } from "../../test-helper/dataGenerators"
import { navigateToCategories } from "./e2e-utils"
import { testWithDocumentUnit as test } from "./fixtures"

async function clickSaveButton(page: Page): Promise<void> {
  await page.locator("[aria-label='Stammdaten Speichern Button']").click()
  await expect(
    page.locator("text=Zuletzt gespeichert um").first()
  ).toBeVisible()
}

async function togglePreviousDecisionsSection(page: Page): Promise<void> {
  await page.locator("text=Vorgehende Entscheidungen").click()
}

async function fillPreviousDecisionInputs(
  page: Page,
  values?: {
    courtType?: string
    courtLocation?: string
    date?: string
    fileNumber?: string
  },
  decisionIndex = 0
): Promise<void> {
  const fillInput = async (ariaLabel: string, value?: string) => {
    await page
      .locator(`[aria-label='${ariaLabel}']`)
      .nth(decisionIndex)
      .fill(value ?? generateString())
  }

  await fillInput("Gerichtstyp Rechtszug", values?.courtType)
  await fillInput("Gerichtsort Rechtszug", values?.courtLocation)
  await fillInput("Datum Rechtszug", values?.date)
  await fillInput("Aktenzeichen Rechtszug", values?.fileNumber)
}

test.describe("save changes in core data and texts and verify it persists", () => {
  test("test core data change", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").fill("abc")
    await page.locator("[aria-label='ECLI']").fill("abc123")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    await page.reload()
    expect(await page.inputValue("[aria-label='Aktenzeichen']")).toBe("")
    expect(await page.inputValue("[aria-label='ECLI']")).toBe("abc123")
  })

  test("saved changes also visible in document unit entry list", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").fill("abc")
    await page.locator("[aria-label='ECLI']").fill("abc123")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await page.goto("/")
    await expect(
      page.locator(`a[href*="/caselaw/documentunit/${documentNumber}/files"]`)
    ).toBeVisible()
    await page.locator(".table-row", {
      hasText: documentNumber,
    })
    await page.locator(".table-row", {
      hasText: "abc",
    })
  })

  test("nested 'Aktenzeichen' input toggles child input and correctly saves and displays data", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").fill("one")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").fill("two")
    await page.keyboard.press("Enter")

    await expect(page.locator("text=one").first()).toBeVisible()
    await expect(page.locator("text=two").first()).toBeVisible()

    await expect(page.locator("text=Abweichendes Aktenzeichen>")).toBeHidden()

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen anzeigen']")
      .click()

    await expect(
      page.locator("text=Abweichendes Aktenzeichen").first()
    ).toBeVisible()

    await page.locator("[aria-label='Abweichendes Aktenzeichen']").fill("three")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    await page.reload()

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen anzeigen']")
      .click()

    await expect(page.locator("text=three").first()).toBeVisible()

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen schließen']")
      .click()

    await expect(
      page.locator("text=Abweichendes Aktenzeichen").first()
    ).toBeHidden()
  })

  test("nested 'Entscheidungsdatum' input toggles child input and correctly saves and displays data", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Entscheidungsdatum']").fill("2022-02-03")
    expect(
      await page.locator("[aria-label='Entscheidungsdatum']").inputValue()
    ).toBe("2022-02-03")

    await expect(
      page.locator("text=Abweichendes Entscheidungsdatum>")
    ).toBeHidden()

    await page
      .locator("[aria-label='Abweichendes Entscheidungsdatum anzeigen']")
      .click()

    await expect(
      page.locator("text=Abweichendes Entscheidungsdatum").first()
    ).toBeVisible()

    await page
      .locator("[aria-label='Abweichendes Entscheidungsdatum']")
      .fill("2022-02-02")
    await page.keyboard.press("Enter")
    await page
      .locator("[aria-label='Abweichendes Entscheidungsdatum']")
      .fill("2022-02-01")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    await page.reload()

    await page
      .locator("[aria-label='Abweichendes Entscheidungsdatum anzeigen']")
      .click()

    await expect(page.locator(".label-wrapper").nth(0)).toHaveText("02.02.2022")

    await expect(page.locator(".label-wrapper").nth(1)).toHaveText("01.02.2022")

    await page
      .locator("[aria-label='Abweichendes Entscheidungsdatum schließen']")
      .click()

    await expect(
      page.locator("text=Abweichendes Entscheidungsdatum").first()
    ).toBeHidden()
  })

  test("nested 'ECLI' input toggles child input and correctly saves and displays data", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='ECLI']").fill("one")
    await expect(page.locator("text=one").first()).toBeVisible()

    await expect(page.locator("text=Abweichender ECLI>")).toBeHidden()

    await page.locator("[aria-label='Abweichender ECLI anzeigen']").click()

    await expect(page.locator("text=Abweichender ECLI").first()).toBeVisible()

    await page.locator("[aria-label='Abweichender ECLI']").fill("two")
    await page.keyboard.press("Enter")
    await page.locator("[aria-label='Abweichender ECLI']").fill("three")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    await page.reload()

    await page.locator("[aria-label='Abweichender ECLI anzeigen']").click()
    await expect(page.locator("text=two").first()).toBeVisible()
    await expect(page.locator("text=three").first()).toBeVisible()

    await page.locator("[aria-label='Abweichender ECLI schließen']").click()

    await expect(page.locator("text=Abweichender ECLI").first()).toBeHidden()
  })

  test("adding, navigating, deleting multiple chips inputs", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").fill("testone")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").fill("testtwo")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").fill("testthree")
    await page.keyboard.press("Enter")

    await expect(page.locator("text=testone").first()).toBeVisible()
    await expect(page.locator("text=testtwo").first()).toBeVisible()

    //Navigate back and delete on enter
    await page.keyboard.press("ArrowLeft")
    await page.keyboard.press("ArrowLeft")
    await page.keyboard.press("Enter")

    await expect(page.locator("text=testtwo").first()).toBeHidden()

    // Tab out and in
    await page.keyboard.press("Tab")
    await page.keyboard.press("Tab")

    await page.keyboard.down("Shift")
    await page.keyboard.press("Tab")
    await page.keyboard.up("Shift")

    await page.keyboard.down("Shift")
    await page.keyboard.press("Tab")
    await page.keyboard.up("Shift")

    await page.keyboard.press("ArrowLeft")

    //Navigate back and delete on backspace
    await page.keyboard.press("Enter")

    await expect(page.locator("text=testone").first()).toBeHidden()

    await page.locator("[aria-label='Stammdaten Speichern Button']").click()

    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    await page.reload()

    await expect(page.locator("text=testthree").first()).toBeVisible()
  })

  test("test previous decision data change", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await togglePreviousDecisionsSection(page)
    await fillPreviousDecisionInputs(page, {
      courtType: "Test Court",
      courtLocation: "Test City",
      date: "12.03.2004",
      fileNumber: "1a2b3c",
    })

    await clickSaveButton(page)
    await page.reload()
    await togglePreviousDecisionsSection(page)

    expect(await page.inputValue("[aria-label='Gerichtstyp Rechtszug']")).toBe(
      "Test Court"
    )
    expect(await page.inputValue("[aria-label='Gerichtsort Rechtszug']")).toBe(
      "Test City"
    )
    expect(await page.inputValue("[aria-label='Datum Rechtszug']")).toBe(
      "12.03.2004"
    )
    expect(await page.inputValue("[aria-label='Aktenzeichen Rechtszug']")).toBe(
      "1a2b3c"
    )
  })

  test("test add another empty previous decision", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await togglePreviousDecisionsSection(page)
    await fillPreviousDecisionInputs(page)
    await page.locator("[aria-label='weitere Entscheidung hinzufügen']").click()

    await expect(
      page.locator("[aria-label='Gerichtstyp Rechtszug']")
    ).toHaveCount(2)
    expect(
      await page
        .locator("[aria-label='Gerichtstyp Rechtszug']")
        .nth(1)
        .inputValue()
    ).toBe("")
    await expect(
      page.locator("[aria-label='Gerichtsort Rechtszug']")
    ).toHaveCount(2)
    expect(
      await page
        .locator("[aria-label='Gerichtsort Rechtszug']")
        .nth(1)
        .inputValue()
    ).toBe("")
    await expect(page.locator("[aria-label='Datum Rechtszug']")).toHaveCount(2)
    expect(
      await page.locator("[aria-label='Datum Rechtszug']").nth(1).inputValue()
    ).toBe("")
    await expect(
      page.locator("[aria-label='Aktenzeichen Rechtszug']")
    ).toHaveCount(2)
    expect(
      await page
        .locator("[aria-label='Aktenzeichen Rechtszug']")
        .nth(1)
        .inputValue()
    ).toBe("")
  })

  test("test delete first previous decision", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await togglePreviousDecisionsSection(page)
    await fillPreviousDecisionInputs(page, { courtType: "Type One" }, 0)
    await page.locator("[aria-label='weitere Entscheidung hinzufügen']").click()
    await fillPreviousDecisionInputs(page, { courtType: "Type Two" }, 1)

    await expect(
      page.locator("[aria-label='Gerichtstyp Rechtszug']")
    ).toHaveCount(2)
    expect(
      await page
        .locator("[aria-label='Gerichtstyp Rechtszug']")
        .nth(0)
        .inputValue()
    ).toBe("Type One")
    expect(
      await page
        .locator("[aria-label='Gerichtstyp Rechtszug']")
        .nth(1)
        .inputValue()
    ).toBe("Type Two")

    await page.locator("[aria-label='Entscheidung Entfernen']").click()

    await expect(
      page.locator("[aria-label='Gerichtstyp Rechtszug']")
    ).toHaveCount(1)
    expect(
      await page
        .locator("[aria-label='Gerichtstyp Rechtszug']")
        .nth(0)
        .inputValue()
    ).toBe("Type Two")
  })

  test("text editor fields should have predefined height", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    // small
    const smallEditor = page.locator("[aria-label='Titelzeile']")
    const smallEditorHeight = await smallEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height")
    )
    expect(parseInt(smallEditorHeight)).toBeGreaterThanOrEqual(60)

    //medium
    const mediumEditor = page.locator("[aria-label='Leitsatz']")
    const mediumEditorHeight = await mediumEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height")
    )
    expect(parseInt(mediumEditorHeight)).toBeGreaterThanOrEqual(120)

    //large
    const largeEditor = page.locator("[aria-label='Gründe']")
    const largeEditorHeight = await largeEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height")
    )
    expect(parseInt(largeEditorHeight)).toBeGreaterThanOrEqual(320)
  })

  test("updated fileNumber should update info panel", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    const infoPanel = page.locator("div", { hasText: documentNumber }).nth(-2)
    const fileNumberPanel = infoPanel
      .locator("div", { hasText: "Aktenzeichen" })
      .nth(-2)
    await expect(fileNumberPanel).toHaveText("Aktenzeichen - ")

    await page.locator("[aria-label='Aktenzeichen']").fill("-firstChip")
    await page.keyboard.press("Enter")
    await expect(fileNumberPanel).toHaveText("Aktenzeichen-firstChip")

    await page.locator("[aria-label='Aktenzeichen']").fill("-secondChip")
    await page.keyboard.press("Enter")
    await expect(fileNumberPanel).toHaveText("Aktenzeichen-firstChip")

    // delete first chip
    await page.locator("div", { hasText: "-firstChip" }).nth(-2).click()
    await page.keyboard.press("Enter")
    await expect(fileNumberPanel).toHaveText("Aktenzeichen-secondChip")
  })
})

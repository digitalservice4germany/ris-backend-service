import { expect, Page } from "@playwright/test"

export const navigateToCategories = async (
  page: Page,
  documentNumber: string
) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/categories`)
  await expect(page.locator("text=Spruchkörper")).toBeVisible()
}

export const navigateToFiles = async (page: Page, documentNumber: string) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/files`)
  await expect(page.locator("h1:has-text('Dokumente')")).toBeVisible()
}

export const navigateToPublication = async (
  page: Page,
  documentNumber: string
) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/publication`)
  await expect(page.locator("h1:has-text('Veröffentlichen')")).toBeVisible()
}

export const uploadTestfile = async (page: Page, filename: string) => {
  const [fileChooser] = await Promise.all([
    page.waitForEvent("filechooser"),
    page.locator("text=oder Datei auswählen").click(),
  ])
  await fileChooser.setFiles("./test/e2e/caselaw/testfiles/" + filename)
  await expect(page.locator("text=Upload läuft")).toBeHidden()
  await expect(page.locator("text=Dokument wird geladen.")).toBeHidden()
}

export async function waitForSaving(page: Page) {
  const saveButton = page.locator("[aria-label='Stammdaten Speichern Button']")
  const saveStatus = page.getByText(/Zuletzt gespeichert um .* Uhr/).first()

  if (await saveStatus.isVisible()) {
    const timeBeforeSave = /Zuletzt gespeichert um (.*) Uhr/.exec(
      await page
        .getByText(/Zuletzt gespeichert um .* Uhr/)
        .first()
        .innerText()
    )?.[1] as string
    await saveButton.click()
    await expect(page.getByText(`Zuletzt gespeichert um`).first()).toBeVisible()
    await expect(page.getByText(`${timeBeforeSave}`).first()).toBeHidden()
  } else {
    await saveButton.click()
    await expect(saveStatus).toBeVisible()
  }
}

export async function waitForSaveStatusChanged(
  page: Page,
  saveTimeStamp: string
) {
  await expect(page.getByText(`Zuletzt gespeichert um`).first()).toBeVisible()
  if (saveTimeStamp) {
    await expect(page.getByText(`${saveTimeStamp}`).first()).toBeHidden()
  }
}

export async function toggleFieldOfLawSection(page: Page): Promise<void> {
  await page.locator("text=Sachgebiete").click()
}

export async function deleteDocumentUnit(page: Page, documentNumber: string) {
  await page.goto("/")
  await expect(
    page.locator(`a[href*="/caselaw/documentunit/${documentNumber}/files"]`)
  ).toBeVisible()
  await page
    .locator(".table-row", {
      hasText: documentNumber,
    })
    .locator("[aria-label='Dokumentationseinheit löschen']")
    .click()
  await page.locator('button:has-text("Löschen")').click()
  await expect(
    page.locator(`a[href*="/caselaw/documentunit/${documentNumber}/files"]`)
  ).toBeHidden()
}

export async function documentUnitExists(
  page: Page,
  documentNumber: string
): Promise<boolean> {
  return (
    await (
      await page.request.get(`/api/v1/caselaw/documentunits/${documentNumber}`)
    ).text()
  ).includes("uuid")
}

export async function waitForInputValue(
  page: Page,
  selector: string,
  expectedValue: string
) {
  await page.waitForFunction(
    ({ selector, expectedValue }) => {
      const input = document.querySelector(selector) as HTMLInputElement
      return input && input.value === expectedValue
    },
    { selector, expectedValue }
  )
}

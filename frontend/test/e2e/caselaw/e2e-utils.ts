import { expect, Page } from "@playwright/test"
import { generateString } from "../../test-helper/dataGenerators"

export const navigateToCategories = async (
  page: Page,
  documentNumber: string,
) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/categories`)
  await expect(page.locator("text=Spruchkörper")).toBeVisible({
    timeout: 15000, // for backend warm up
  })
}

export const navigateToFiles = async (page: Page, documentNumber: string) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/files`)
  await expect(page.locator("h1:has-text('Dokumente')")).toBeVisible({
    timeout: 15000, // for backend warm up
  })
}

export const navigateToPublication = async (
  page: Page,
  documentNumber: string,
) => {
  await page.goto(`/caselaw/documentunit/${documentNumber}/publication`)
  await expect(page.locator("h1:has-text('Veröffentlichen')")).toBeVisible({
    timeout: 15000, // for backend warm up
  })
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

export async function waitForSaving(
  body: () => Promise<void>,
  page: Page,
  options?: { clickSaveButton?: boolean; reload?: boolean },
) {
  if (options?.reload) {
    await page.reload()
  }

  const saveStatus = page.getByText(/Zuletzt .* Uhr/).first()
  let lastSaving: string | undefined = undefined
  if (await saveStatus.isVisible()) {
    lastSaving = /Zuletzt (.*) Uhr/.exec(
      await saveStatus.innerText(),
    )?.[1] as string
  }

  await body()

  if (options?.clickSaveButton) {
    await page.locator("[aria-label='Speichern Button']").click()
  }

  await Promise.all([
    await expect(page.getByText(`Zuletzt`).first()).toBeVisible(),
    lastSaving ??
      (await expect(
        page.getByText(`Zuletzt ${lastSaving} Uhr`).first(),
      ).toBeHidden()),
  ])
}

export async function toggleFieldOfLawSection(page: Page): Promise<void> {
  await page.locator("text=Sachgebiete").click()
}

export async function deleteDocumentUnit(page: Page, documentNumber: string) {
  const response = await page.request.delete(
    `/api/v1/caselaw/documentunits/${documentNumber}`,
  )
  expect(response.ok).toBeTruthy()
}

export async function documentUnitExists(
  page: Page,
  documentNumber: string,
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
  expectedValue: string,
) {
  await page.waitForFunction(
    ({ selector, expectedValue }) => {
      const input = document.querySelector(selector) as HTMLInputElement
      return input && input.value === expectedValue
    },
    { selector, expectedValue },
  )
}

export async function toggleNormsSection(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Normen Aufklappen" }).click()
}

export async function fillPreviousDecisionInputs(
  page: Page,
  values?: {
    court?: string
    decisionDate?: string
    fileNumber?: string
    documentType?: string
    dateUnknown?: boolean
  },
  decisionIndex = 0,
): Promise<void> {
  const fillInput = async (ariaLabel: string, value = generateString()) => {
    const input = page.locator(`[aria-label='${ariaLabel}']`).nth(decisionIndex)
    await input.fill(value ?? ariaLabel)
    await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
  }

  if (values?.court) {
    await fillInput("Gericht Vorgehende Entscheidung", values?.court)
    await page.getByText(values.court, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Gericht Vorgehende Entscheidung']",
      values.court,
    )
  }
  if (values?.decisionDate) {
    await fillInput(
      "Entscheidungsdatum Vorgehende Entscheidung",
      values?.decisionDate,
    )
  }
  if (values?.fileNumber) {
    await fillInput("Aktenzeichen Vorgehende Entscheidung", values?.fileNumber)
  }
  if (values?.documentType) {
    await fillInput("Dokumenttyp Vorgehende Entscheidung", values?.documentType)
    await page.locator("[aria-label='dropdown-option']").first().click()
  }
  if (values?.dateUnknown) {
    const dateUnknownCheckbox = page.getByLabel("Datum unbekannt")
    if (!(await dateUnknownCheckbox.isChecked())) {
      await dateUnknownCheckbox.click()
      await expect(dateUnknownCheckbox).toBeChecked()
      await waitForInputValue(
        page,
        "[aria-label='Entscheidungsdatum Vorgehende Entscheidung']",
        "",
      )
    }
  }
}

export async function fillNormInputs(
  page: Page,
  values?: {
    normAbbreviation?: string
    singleNorm?: string
    dateOfVersion?: string
    dateOfRelevance?: string
  },
): Promise<void> {
  const fillInput = async (ariaLabel: string, value = generateString()) => {
    const input = page.locator(`[aria-label='${ariaLabel}']`)
    await input.fill(value ?? ariaLabel)
    await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
  }

  if (values?.normAbbreviation) {
    await fillInput("RIS-Abkürzung der Norm", values?.normAbbreviation)
    await page.getByRole("button", { name: "dropdown-option" }).click()
    await waitForInputValue(
      page,
      "[aria-label='RIS-Abkürzung der Norm']",
      values.normAbbreviation,
    )
  }

  if (values?.singleNorm) {
    await fillInput("Einzelnorm der Norm", values?.singleNorm)
  }
  if (values?.dateOfVersion) {
    await fillInput("Fassungsdatum der Norm", values?.dateOfVersion)
  }
  if (values?.dateOfRelevance) {
    await fillInput("Jahr der Norm", values?.dateOfRelevance)
  }
}

export async function fillActiveCitationInputs(
  page: Page,
  values?: {
    citationStyle?: string
    court?: string
    decisionDate?: string
    fileNumber?: string
    documentType?: string
  },
): Promise<void> {
  const fillInput = async (ariaLabel: string, value = generateString()) => {
    const input = page.locator(`[aria-label='${ariaLabel}']`)
    await input.fill(value ?? ariaLabel)
    await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
  }

  if (values?.citationStyle) {
    await fillInput("Art der Zitierung", values?.citationStyle)
    await page.getByRole("button", { name: "dropdown-option" }).click()
    await waitForInputValue(
      page,
      "[aria-label='Art der Zitierung']",
      values.citationStyle,
    )
  }

  if (values?.court) {
    await fillInput("Gericht der Aktivzitierung", values?.court)
    await page.getByText(values.court, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Gericht der Aktivzitierung']",
      values.court,
    )
  }
  if (values?.decisionDate) {
    await fillInput(
      "Entscheidungsdatum der Aktivzitierung",
      values?.decisionDate,
    )
  }
  if (values?.fileNumber) {
    await fillInput("Aktenzeichen der Aktivzitierung", values?.fileNumber)
  }
  if (values?.documentType) {
    await fillInput("Dokumenttyp der Aktivzitierung", values?.documentType)
    await page.locator("[aria-label='dropdown-option']").first().click()
  }
}

export async function checkIfPreviousDecisionCleared(page: Page) {
  ;[
    "Gericht Vorgehende Entscheidung",
    "Entscheidungsdatum Vorgehende Entscheidung",
    "Aktenzeichen Vorgehende Entscheidung",
    "Dokumenttyp Vorgehende Entscheidung",
  ].forEach((ariaLabel) =>
    waitForInputValue(page, `[aria-label='${ariaLabel}']`, ""),
  )
}

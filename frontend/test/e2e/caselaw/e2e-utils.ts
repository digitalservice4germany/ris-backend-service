import { expect, Page } from "@playwright/test"
import { generateString } from "../../test-helper/dataGenerators"
import { caselawTest as test } from "./fixtures"
import SingleNorm from "@/domain/singleNorm"

/* eslint-disable playwright/no-conditional-in-test */

const getAllQueryParamsFromUrl = (page: Page): string => {
  const url = new URL(page.url())
  const params = url.searchParams.toString()
  return params ? `?${params}` : ""
}

export const navigateToSearch = async (
  page: Page,
  { navigationBy }: { navigationBy: "click" | "url" } = { navigationBy: "url" },
) => {
  await test.step("Navigate to 'Suche'", async () => {
    if (navigationBy === "url") {
      await page.goto(`/caselaw`)
    } else {
      await page.getByTestId("search-navbar-button").click()
    }
    await page.waitForURL("/caselaw")

    await expect(page.getByText("Übersicht Rechtsprechung")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
  })
}

export const navigateToCategories = async (
  page: Page,
  documentNumber: string,
) => {
  await test.step("Navigate to 'Rubriken'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    const baseUrl = `/caselaw/documentunit/${documentNumber}/categories${queryParams}`

    await page.goto(baseUrl)
    await expect(page.getByText("Spruchkörper")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
    await expect(page.getByText(documentNumber)).toBeVisible()
  })
}

export const navigateToReferences = async (
  page: Page,
  documentNumber: string,
) => {
  await test.step("Navigate to 'Fundstellen'", async () => {
    const baseUrl = `/caselaw/documentunit/${documentNumber}/references`

    await page.goto(baseUrl)
    await expect(page.getByText("Periodikum")).toBeVisible()
  })
}

export const navigateToPreview = async (page: Page, documentNumber: string) => {
  await test.step("Navigate to 'Vorschau'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    const baseUrl = `/caselaw/documentunit/${documentNumber}/preview${queryParams}`

    await page.goto(baseUrl)
    await expect(page.getByTestId("preview")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
    await expect(page.getByText(documentNumber)).toBeVisible()
  })
}

export const navigateToFiles = async (page: Page, documentNumber: string) => {
  await test.step("Navigate to 'Dokumente'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    await page.goto(
      `/caselaw/documentunit/${documentNumber}/files${queryParams}`,
    )
    await expect(page.locator("h1:has-text('Dokumente')")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
  })
}

export const navigateToHandover = async (
  page: Page,
  documentNumber: string,
) => {
  await test.step("Navigate to 'Übergabe an jDV'", async () => {
    await page.goto(`/caselaw/documentunit/${documentNumber}/handover`)
    await expect(page.locator("h1:has-text('Übergabe an jDV')")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
  })
}

export const handoverDocumentationUnit = async (
  page: Page,
  documentNumber: string,
) => {
  await navigateToHandover(page, documentNumber)
  await page
    .locator("[aria-label='Dokumentationseinheit an jDV übergeben']")
    .click()
  await expect(page.getByText("Email wurde versendet")).toBeVisible()

  await expect(page.getByText("Xml Email Abgabe -")).toBeVisible()
}

export const uploadTestfile = async (
  page: Page,
  filename: string | string[],
) => {
  const [fileChooser] = await Promise.all([
    page.waitForEvent("filechooser"),
    page.getByText("Oder hier auswählen").click(),
  ])
  if (Array.isArray(filename)) {
    await fileChooser.setFiles(
      filename.map((file) => "./test/e2e/caselaw/testfiles/" + file),
    )
  } else {
    await fileChooser.setFiles("./test/e2e/caselaw/testfiles/" + filename)
  }
  await expect(async () => {
    await expect(page.getByLabel("Ladestatus")).not.toBeAttached()
  }).toPass({ timeout: 15000 })
}

export async function waitForSaving(
  body: () => Promise<void>,
  page: Page,
  options?: { clickSaveButton?: boolean; reload?: boolean; error?: string },
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

  if (options?.error) {
    await expect(page.getByText(options.error).first()).toBeVisible()
  } else {
    await Promise.all([
      await expect(page.getByText(`Zuletzt`).first()).toBeVisible(),
      lastSaving ??
        (await expect(
          page.getByText(`Zuletzt ${lastSaving} Uhr`).first(),
        ).toBeHidden()),
    ])
  }
}

export async function toggleFieldOfLawSection(page: Page): Promise<void> {
  await page.getByText("Sachgebiete").click()
}

export async function deleteDocumentUnit(page: Page, documentNumber: string) {
  const cookies = await page.context().cookies()
  const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
  const getResponse = await page.request.get(
    `/api/v1/caselaw/documentunits/${documentNumber}`,
    { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
  )
  expect(getResponse.ok()).toBeTruthy()

  const { uuid } = await getResponse.json()

  const deleteResponse = await page.request.delete(
    `/api/v1/caselaw/documentunits/${uuid}`,
    { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
  )
  expect(deleteResponse.ok()).toBeTruthy()
}

export async function deleteProcedure(page: Page, uuid: string) {
  const cookies = await page.context().cookies()
  const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
  const response = await page.request.delete(
    `/api/v1/caselaw/procedure/${uuid}`,
    { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
  )
  expect(response.ok()).toBeTruthy()
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

export async function fillSearchInput(
  page: Page,
  values?: {
    fileNumber?: string
    courtType?: string
    courtLocation?: string
    decisionDate?: string
    decisionDateEnd?: string
    documentNumber?: string
    myDocOfficeOnly?: boolean
    status?: string
  },
) {
  const fillInput = async (ariaLabel: string, value = generateString()) => {
    const input = page.locator(`[aria-label='${ariaLabel}']`)
    await input.fill(value ?? ariaLabel)
    await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
  }

  //reset search first
  await navigateToSearch(page)

  if (values?.fileNumber) {
    await fillInput("Aktenzeichen Suche", values?.fileNumber)
  }
  if (values?.courtType) {
    await fillInput("Gerichtstyp Suche", values?.courtType)
  }

  if (values?.courtLocation) {
    await fillInput("Gerichtsort Suche", values?.courtLocation)
  }

  if (values?.decisionDate) {
    await fillInput("Entscheidungsdatum Suche", values?.decisionDate)
  }

  if (values?.decisionDateEnd) {
    await fillInput("Entscheidungsdatum Suche Ende", values?.decisionDateEnd)
  }

  if (values?.documentNumber) {
    await fillInput("Dokumentnummer Suche", values?.documentNumber)
  }

  if (values?.myDocOfficeOnly === true) {
    const myDocOfficeOnlyCheckbox = page.getByLabel(
      "Nur meine Dokstelle Filter",
    )
    if (!(await myDocOfficeOnlyCheckbox.isChecked())) {
      await myDocOfficeOnlyCheckbox.click()
      await expect(myDocOfficeOnlyCheckbox).toBeChecked()
    }
  }

  if (values?.status) {
    const select = page.locator(`select[id="status"]`)
    await select.selectOption(values?.status)
  }

  await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
  await expect(page.getByLabel("Ladestatus")).toBeHidden()
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
    dateKnown?: boolean
    deviatingFileNumber?: string
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
    await page.getByText(values.documentType, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Dokumenttyp Vorgehende Entscheidung']",
      values.documentType,
    )
  }

  if (values?.dateKnown === false) {
    const dateUnknownCheckbox = page.getByLabel("Datum unbekannt")
    if (!(await dateUnknownCheckbox.isChecked())) {
      await dateUnknownCheckbox.click()
      await expect(dateUnknownCheckbox).toBeChecked()
    }
  }

  if (values?.deviatingFileNumber) {
    if (
      !(await page
        .getByLabel("Abweichendes Aktenzeichen Vorgehende Entscheidung", {
          exact: true,
        })
        .isVisible())
    ) {
      await page
        .locator(
          "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung anzeigen']",
        )
        .click()
    }
    await fillInput(
      "Abweichendes Aktenzeichen Vorgehende Entscheidung",
      values?.deviatingFileNumber,
    )
  }
}

export async function fillEnsuingDecisionInputs(
  page: Page,
  values?: {
    pending?: boolean
    court?: string
    decisionDate?: string
    fileNumber?: string
    documentType?: string
    note?: string
  },
  decisionIndex = 0,
): Promise<void> {
  const fillInput = async (ariaLabel: string, value = generateString()) => {
    const input = page.locator(`[aria-label='${ariaLabel}']`).nth(decisionIndex)
    await input.fill(value ?? ariaLabel)
    await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
  }

  if (values?.court) {
    await fillInput("Gericht Nachgehende Entscheidung", values?.court)
    await page.getByText(values.court, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Gericht Nachgehende Entscheidung']",
      values.court,
    )
  }
  if (values?.decisionDate) {
    await fillInput(
      "Entscheidungsdatum Nachgehende Entscheidung",
      values?.decisionDate,
    )
  }
  if (values?.fileNumber) {
    await fillInput("Aktenzeichen Nachgehende Entscheidung", values?.fileNumber)
  }
  if (values?.documentType) {
    await fillInput(
      "Dokumenttyp Nachgehende Entscheidung",
      values?.documentType,
    )
    await page.getByText(values.documentType, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Dokumenttyp Nachgehende Entscheidung']",
      values.documentType,
    )
  }
  if (values?.pending) {
    const pendingCheckbox = page.getByLabel("Anhängige Entscheidung")
    if (!(await pendingCheckbox.isChecked())) {
      await pendingCheckbox.click()
      await expect(pendingCheckbox).toBeChecked()
    }
  }
}

export async function fillInput(
  page: Page,
  ariaLabel: string,
  value = generateString(),
) {
  const input = page.locator(`[aria-label='${ariaLabel}']`)
  await input.fill(value ?? ariaLabel)
  await waitForInputValue(page, `[aria-label='${ariaLabel}']`, value)
}

export async function clearInput(page: Page, ariaLabel: string) {
  const input = page.locator(`[aria-label='${ariaLabel}']`)
  await input.clear()
}

export async function fillNormInputs(
  page: Page,
  values?: {
    normAbbreviation?: string
    singleNorms?: SingleNorm[]
  },
): Promise<void> {
  if (values?.normAbbreviation) {
    await fillInput(page, "RIS-Abkürzung", values.normAbbreviation)
    await page.getByText(values.normAbbreviation, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='RIS-Abkürzung']",
      values.normAbbreviation,
    )
  }
  if (values?.singleNorms) {
    for (let index = 0; index < values.singleNorms.length; index++) {
      const entry = values.singleNorms[index]
      if (entry.singleNorm) {
        const input = page.getByLabel("Einzelnorm der Norm").nth(index)
        await input.fill(entry.singleNorm)
        await expect(
          page.locator("[aria-label='Einzelnorm der Norm'] >> nth=" + index),
        ).toHaveValue(entry.singleNorm)
      }

      if (entry.dateOfVersion) {
        const input = page.getByLabel("Fassungsdatum der Norm").nth(index)
        await input.fill(entry.dateOfVersion)
        await expect(
          page.locator("[aria-label='Fassungsdatum der Norm'] >> nth=" + index),
        ).toHaveValue(entry.dateOfVersion)
      }
      if (entry.dateOfRelevance) {
        const input = page.getByLabel("Jahr der Norm").nth(index)
        await input.fill(entry.dateOfRelevance)
        await expect(
          page.locator("[aria-label='Jahr der Norm'] >> nth=" + index),
        ).toHaveValue(entry.dateOfRelevance)
      }
    }
  }
}

export async function fillActiveCitationInputs(
  page: Page,
  values?: {
    citationType?: string
    court?: string
    decisionDate?: string
    fileNumber?: string
    documentType?: string
  },
): Promise<void> {
  if (values?.citationType) {
    await fillInput(page, "Art der Zitierung", values?.citationType)
    await page.getByText(values.citationType, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Art der Zitierung']",
      values.citationType,
    )
  }

  if (values?.court) {
    await fillInput(page, "Gericht Aktivzitierung", values?.court)
    await page.getByText(values.court, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Gericht Aktivzitierung']",
      values.court,
    )
  }
  if (values?.decisionDate) {
    await fillInput(
      page,
      "Entscheidungsdatum Aktivzitierung",
      values?.decisionDate,
    )
  }
  if (values?.fileNumber) {
    await fillInput(page, "Aktenzeichen Aktivzitierung", values?.fileNumber)
  }
  if (values?.documentType) {
    await fillInput(page, "Dokumenttyp Aktivzitierung", values?.documentType)
    await page.getByText(values.documentType, { exact: true }).click()
    await waitForInputValue(
      page,
      "[aria-label='Dokumenttyp Aktivzitierung']",
      values.documentType,
    )
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

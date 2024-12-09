import { expect, Locator, Page, Request } from "@playwright/test"
import { Browser } from "playwright"
import { caselawTest as test } from "./fixtures"
import { DocumentUnitCatagoriesEnum } from "@/components/enumDocumentUnitCatagories"
import SingleNorm from "@/domain/singleNorm"
import { generateString } from "~/test-helper/dataGenerators"

/* eslint-disable playwright/no-conditional-in-test */

function scrollToID(category?: DocumentUnitCatagoriesEnum): string {
  return category ? "#" + category : ""
}

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

export const navigateToProcedures = async (
  page: Page,
  searchParam?: string,
) => {
  await test.step("Navigate to 'Vorgänge'", async () => {
    const urlPostFix = searchParam ? `?q=${searchParam}` : ""
    await page.goto(`/caselaw/procedures` + urlPostFix)
    await expect(page.getByLabel("Nach Vorgängen suchen")).toBeVisible()
  })
}

export const navigateToCategories = async (
  page: Page,
  documentNumber: string,
  options?: {
    category?: DocumentUnitCatagoriesEnum
    skipAssert?: boolean
  },
) => {
  await test.step("Navigate to 'Rubriken'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    const baseUrl =
      `/caselaw/documentunit/${documentNumber}/categories${queryParams}` +
      scrollToID(options?.category)

    await page.goto(baseUrl)

    if (options?.skipAssert) return

    await expect(page.getByText("Entscheidungsname")).toBeVisible({
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

export const navigateToPeriodicalEvaluation = async (page: Page) => {
  await test.step("Navigate to 'Periodika'", async () => {
    const baseUrl = "/caselaw/periodical-evaluation"

    await page.goto(baseUrl)

    await expect(page.getByTestId("periodical-evaluation-title")).toBeVisible()
  })
}

export const navigateToPeriodicalReferences = async (
  page: Page,
  editionId: string,
) => {
  await test.step("Navigate to 'Periodikumauswertung > Fundstellen'", async () => {
    const baseUrl = `/caselaw/periodical-evaluation/${editionId}/references`
    await getRequest(baseUrl, page)
    await expect(page.getByTestId("references-title")).toBeVisible()
  })
}

export const navigateToPeriodicalHandover = async (
  page: Page,
  editionId: string,
) => {
  await test.step("Navigate to 'Periodikumauswertung > Übergabe an jDV'", async () => {
    const baseUrl = `/caselaw/periodical-evaluation/${editionId}/handover`

    await page.goto(baseUrl)
    await expect(page.getByTestId("handover-title")).toBeVisible()
  })
}

export const navigateToPreview = async (
  page: Page,
  documentNumber: string,
  options?: {
    skipAssert?: boolean
  },
) => {
  await test.step("Navigate to 'Vorschau'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    const baseUrl = `/caselaw/documentunit/${documentNumber}/preview${queryParams}`

    await page.goto(baseUrl)

    if (options?.skipAssert) return
    await expect(page.getByTestId("preview")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
    await expect(page.getByText(documentNumber)).toBeVisible()
  })
}

export const navigateToAttachments = async (
  page: Page,
  documentNumber: string,
  options?: {
    skipAssert?: boolean
  },
) => {
  await test.step("Navigate to 'Dokumente'", async () => {
    const queryParams = getAllQueryParamsFromUrl(page)
    await page.goto(
      `/caselaw/documentunit/${documentNumber}/attachments${queryParams}`,
    )
    if (options?.skipAssert) return

    await expect(page.getByTestId("document-unit-attachments")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
  })
}

export const navigateToHandover = async (
  page: Page,
  documentNumber: string,
  options?: {
    skipAssert?: boolean
  },
) => {
  await test.step("Navigate to 'Übergabe an jDV'", async () => {
    await page.goto(`/caselaw/documentunit/${documentNumber}/handover`)
    if (options?.skipAssert) return
    await expect(page.locator("h1:has-text('Übergabe an jDV')")).toBeVisible({
      timeout: 15000, // for backend warm up
    })
  })
}

export const handoverDocumentationUnit = async (
  page: Page,
  documentNumber: string,
) => {
  await test.step(`Übergebe Dokumentationseinheit ${documentNumber}`, async () => {
    await navigateToHandover(page, documentNumber)
    await expect(page.getByText("XML Vorschau")).toBeVisible()
    await page.getByLabel("Dokumentationseinheit an jDV übergeben").click()
    await expect(page.getByText("Email wurde versendet")).toBeVisible()

    await expect(page.getByText("Xml Email Abgabe -")).toBeVisible()
  })
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

export async function save(page: Page) {
  const saveRequest = page.waitForRequest("**/api/v1/caselaw/documentunits/*", {
    timeout: 5_000,
  })
  await page.locator("[aria-label='Speichern Button']").click()
  await saveRequest
  await expect(page.getByText(`Zuletzt`).first()).toBeVisible()
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
  timeout?: number,
) {
  await page.waitForFunction(
    ({ selector, expectedValue }) => {
      const input = document.querySelector(selector) as HTMLInputElement
      return input && input.value === expectedValue
    },
    { selector, expectedValue, timeout },
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
        ).toHaveValue(entry.singleNorm.trim())
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

export async function copyPasteTextFromAttachmentIntoEditor(
  page: Page,
  attachmentLocator: Locator,
  editor: Locator,
): Promise<void> {
  await attachmentLocator.selectText()

  // copy from side panel to clipboard
  await page.keyboard.press("ControlOrMeta+C")

  // paste from clipboard into input field
  await editor.click()

  await page.keyboard.press("ControlOrMeta+V")

  await page
    .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
    .click()
}

export async function getRequest(url: string, page: Page): Promise<Request> {
  const requestFinishedPromise = page.waitForEvent("requestfinished")
  // waitUntil, because timeout NS_ERROR_FAILURE is a common issue in Firefox
  await page.goto(url, { waitUntil: "domcontentloaded" })

  return await requestFinishedPromise
}

export async function clickCategoryButton(testId: string, page: Page) {
  await test.step(`click '${testId}' button to open category`, async () => {
    const button = page.getByRole("button", { name: testId, exact: true })
    await button.click()
    await expect(page.getByTestId(testId)).toBeVisible()
  })
}

export async function assignProcedureToDocUnit(
  page: Page,
  documentNumber: string,
  /** Needed to be able to delete all procedures with prefix afterward */
  prefix: string,
) {
  let procedureName = ""
  await test.step("Internal user assigns new procedure to doc unit", async () => {
    await navigateToCategories(page, documentNumber)
    procedureName = prefix + generateString({ length: 10 })
    await page.locator("[aria-label='Vorgang']").fill(procedureName)
    await page
      .getByText(`${procedureName} neu erstellen`)
      .click({ timeout: 5_000 })
    await save(page)
  })
  return procedureName
}

export async function deleteAllProcedures(
  browser: Browser,
  procedurePrefix: string,
) {
  const page = await browser.newPage()
  const response = await page.request.get(
    `/api/v1/caselaw/procedure?sz=50&pg=0&q=${procedurePrefix}&withDocUnits=false`,
  )
  const responseBody = await response.json()
  for (const procedure of responseBody.content) {
    const uuid = procedure.id
    await deleteProcedure(page, uuid)
  }
}

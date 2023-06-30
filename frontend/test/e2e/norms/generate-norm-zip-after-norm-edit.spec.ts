import { expect } from "@playwright/test"

import {
  getDownloadedFileContent,
  getMetaDataFileAsString,
  openNorm,
} from "./e2e-utils"
import { testWithImportedNorm } from "./fixtures"
import { FieldType, fillInputField } from "./utilities"

testWithImportedNorm(
  "Check if norm zip can be generated properly after an edit",
  async ({ page, normData, guid }) => {
    await openNorm(
      page,
      normData.metadataSections?.NORM?.[0]?.OFFICIAL_LONG_TITLE?.[0] ?? "",
      guid
    )
    const fileName = normData["jurisZipFileName"]
    const locatorFrameButton = page.locator("a:has-text('Rahmen')")
    await expect(locatorFrameButton).toBeVisible()
    await locatorFrameButton.click()

    const newValue = "fake-Juris-Abkürzung"
    await fillInputField(page, FieldType.TEXT, "risAbbreviation", newValue)

    await page.locator("[aria-label='Rahmendaten Speichern Button']").click()
    await expect(
      page.locator("text=Zuletzt gespeichert um").first()
    ).toBeVisible()

    const locatorExportMenu = page.locator("a:has-text('Export')")
    await expect(locatorExportMenu).toBeVisible()
    await locatorExportMenu.click()
    await expect(page).toHaveURL(`/norms/norm/${guid}/export`)
    const locatorNewGeneration = page.locator(
      "a:has-text('Neue Zip-Datei generieren')"
    )
    await expect(locatorNewGeneration).toBeVisible()
    await locatorNewGeneration.click()

    const locatorExportButton = page.locator(
      'a:has-text("Zip Datei speichern")'
    )
    await expect(locatorExportButton).toBeVisible()

    const downloadFileContent = await getDownloadedFileContent(page, fileName)

    const metadataFileDownloaded = await getMetaDataFileAsString(
      downloadFileContent
    )

    expect(metadataFileDownloaded.includes(newValue)).toBeTruthy()
  }
)

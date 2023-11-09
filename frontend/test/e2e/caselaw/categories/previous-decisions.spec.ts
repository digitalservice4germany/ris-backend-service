import { expect } from "@playwright/test"
import { generateString } from "../../../test-helper/dataGenerators"
import {
  fillPreviousDecisionInputs,
  navigateToCategories,
  navigateToPublication,
  waitForSaving,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"

test.describe("previous decisions", () => {
  test("renders empty proceeding decision in edit mode, when none in list", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(
      page.getByRole("heading", { name: "Vorgehende Entscheidung " }),
    ).toBeVisible()
    await expect(
      page.getByLabel("Gericht Vorgehende Entscheidung"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Entscheidungsdatum Vorgehende Entscheidung"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Aktenzeichen Vorgehende Entscheidung"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Dokumenttyp Vorgehende Entscheidung"),
    ).toBeVisible()
    await expect(page.getByLabel("Datum unbekannt")).toBeVisible()
  })

  test("create and renders new proceeding decision in list", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(
      page.getByText(
        `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, AnU`,
        {
          exact: true,
        },
      ),
    ).toBeVisible()
    await expect(page.getByLabel("Eintrag löschen")).toHaveCount(1)
    await expect(page.getByLabel("Eintrag bearbeiten")).toHaveCount(1)

    await page.getByLabel("Weitere Angabe").click()
    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await page.getByLabel("Vorgehende Entscheidung speichern").click()

    const proceedingDecisionContainer = page.getByLabel(
      "Vorgehende Entscheidung",
    )
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
    await expect(
      proceedingDecisionContainer.getByLabel("Eintrag löschen"),
    ).toHaveCount(2)
    await expect(
      proceedingDecisionContainer.getByLabel("Eintrag bearbeiten"),
    ).toHaveCount(2)
  })

  test("saving behaviour of proceeding decision", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await waitForSaving(
      async () => {
        await fillPreviousDecisionInputs(page, {
          court: prefilledDocumentUnit.coreData.court?.label,
          fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
          documentType:
            prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
          decisionDate: "01.01.2020",
        })
        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    const proceedingDecisionContainer = page.getByLabel(
      "Vorgehende Entscheidung",
    )
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
    await page.reload()

    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1, { timeout: 10000 }) // reloading can be slow if too many parallel tests

    await page.getByLabel("Weitere Angabe").click()
    await page.getByLabel("Aktenzeichen Vorgehende Entscheidung").fill("two")
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    // "Vorgehende Entscheidung speichern" only saves state in frontend, no communication to backend yet
    await page.reload()
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1, { timeout: 10000 }) // reloading can be slow if too many parallel tests

    await page.getByLabel("Weitere Angabe").click()
    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen Vorgehende Entscheidung")
          .fill("two")
        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.reload()
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2, { timeout: 10000 }) // reloading can be slow if too many parallel tests
  })

  test("manually added proceeding decision can be edited", async ({
    page,
    documentNumber,
  }) => {
    const fileNumber1 = generateString()
    const fileNumber2 = generateString()
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen Vorgehende Entscheidung")
          .fill(fileNumber1)
      },
      page,
      { clickSaveButton: true },
    )
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(page.getByText(fileNumber1)).toBeVisible()

    await page.getByLabel("Eintrag bearbeiten").click()
    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen Vorgehende Entscheidung")
          .fill(fileNumber2)
      },
      page,
      { clickSaveButton: true },
    )
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(page.getByText(fileNumber1)).toBeHidden()
    await expect(page.getByText(fileNumber2)).toBeVisible()
  })

  test("manually added proceeding decision can be deleted", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen Vorgehende Entscheidung")
          .fill("one")
      },
      page,
      { clickSaveButton: true },
    )

    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await page.getByLabel("Weitere Angabe").click()
    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen Vorgehende Entscheidung")
          .fill("two")
      },
      page,
      { clickSaveButton: true },
    )
    const proceedingDecisionContainer = page.getByLabel(
      "Vorgehende Entscheidung",
    )
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
    await page.getByLabel("Eintrag löschen").first().click()
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
  })

  test("search for documentunits and link as proceeding decision", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToPublication(
      page,
      prefilledDocumentUnit.documentNumber || "",
    )

    await page
      .locator("[aria-label='Dokumentationseinheit veröffentlichen']")
      .click()
    await expect(page.locator("text=Email wurde versendet")).toBeVisible()

    await expect(page.locator("text=Xml Email Abgabe -")).toBeVisible()
    await expect(page.locator("text=in Veröffentlichung")).toBeVisible()

    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })
    const proceedingDecisionContainer = page.getByLabel("Rechtszug")
    await proceedingDecisionContainer
      .getByLabel("Nach Entscheidung suchen")
      .click()

    await expect(page.getByText("1 Ergebnis gefunden.")).toBeVisible()

    const result = page.getByText(
      `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, AnU, ${prefilledDocumentUnit.documentNumber}`,
    )

    await expect(result).toBeVisible()
    await page.getByLabel("Treffer übernehmen").click()

    //make sure to have citation style in list
    const listItem = page.getByText(
      `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, AnU, ${prefilledDocumentUnit.documentNumber}`,
    )
    await expect(listItem).toBeVisible()
    await expect(page.getByLabel("Eintrag löschen")).toBeVisible()

    //can not be edited
    await expect(page.getByLabel("Eintrag bearbeiten")).toBeHidden()

    // search for same parameters gives same result, indication that decision is already added
    await page.getByLabel("Weitere Angabe").click()
    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await proceedingDecisionContainer
      .getByLabel("Nach Entscheidung suchen")
      .click()

    await expect(page.getByText("1 Ergebnis gefunden.")).toBeVisible()
    await expect(page.getByText("Bereits hinzugefügt")).toBeVisible()

    //can be deleted
    await page.getByLabel("Eintrag löschen").first().click()
    await expect(
      proceedingDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
    await expect(listItem).toBeHidden()
  })

  test("validates against required fields", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillPreviousDecisionInputs(page, {
      fileNumber: "abc",
    })
    await page.getByLabel("Vorgehende Entscheidung speichern").click()

    await expect(page.getByLabel("Fehlerhafte Eingabe")).toBeVisible()
    await page.getByLabel("Eintrag bearbeiten").click()
    await expect(
      page.getByLabel("Rechtszug").getByText("Pflichtfeld nicht befüllt"),
    ).toHaveCount(2)

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })
    await page.getByLabel("Vorgehende Entscheidung speichern").click()

    await expect(page.getByLabel("Fehlerhafte Eingabe")).toBeHidden()
  })

  test("adding empty proceeding decision not possible", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await page.getByLabel("Vorgehende Entscheidung speichern").isDisabled()
  })

  test("incomplete date input shows error message and does not persist", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await page.locator("[aria-label='Entscheidungsdatum Rechtszug']").fill("03")

    await page.keyboard.press("Tab")

    await expect(
      page.locator("[aria-label='Entscheidungsdatum Rechtszug']"),
    ).toHaveValue("03")

    await expect(page.locator("text=Unvollständiges Datum")).toBeVisible()

    await page.reload()

    await expect(
      page.locator("[aria-label='Entscheidungsdatum Rechtszug']"),
    ).toHaveValue("")
  })
})

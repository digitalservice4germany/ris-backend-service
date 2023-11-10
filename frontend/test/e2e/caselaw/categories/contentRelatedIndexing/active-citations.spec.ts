import { expect } from "@playwright/test"
import { generateString } from "../../../../test-helper/dataGenerators"
import {
  fillActiveCitationInputs,
  navigateToCategories,
  navigateToPublication,
  waitForSaving,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"

test.describe("active citations", () => {
  test("renders empty active citation in edit mode, when no activeCitations in list", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(
      page.getByRole("heading", { name: "Aktivzitierung" }),
    ).toBeVisible()
    await expect(page.getByLabel("Art der Zitierung")).toBeVisible()
    await expect(page.getByLabel("Gericht der Aktivzitierung")).toBeVisible()
    await expect(
      page.getByLabel("Entscheidungsdatum der Aktivzitierung"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Aktenzeichen der Aktivzitierung"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Dokumenttyp der Aktivzitierung"),
    ).toBeVisible()
  })

  test("create and renders new active citations in list", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillActiveCitationInputs(page, {
      citationType: "Änderung",
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await page.getByLabel("Aktivzitierung speichern").click()
    await expect(
      page.getByText(
        `Änderung, AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil`,
        {
          exact: true,
        },
      ),
    ).toBeVisible()
    await expect(page.getByLabel("Eintrag löschen")).toHaveCount(1)
    await expect(page.getByLabel("Eintrag bearbeiten")).toHaveCount(1)

    await page.getByLabel("Weitere Angabe").click()
    await fillActiveCitationInputs(page, {
      citationType: "Änderung",
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })
    await page.getByLabel("Aktivzitierung speichern").click()

    const activeCitationContainer = page.getByLabel("Aktivzitierung")
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
    await expect(
      activeCitationContainer.getByLabel("Eintrag löschen"),
    ).toHaveCount(2)
    await expect(
      activeCitationContainer.getByLabel("Eintrag bearbeiten"),
    ).toHaveCount(2)
  })

  test("saving behaviour of active citation", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await waitForSaving(
      async () => {
        await fillActiveCitationInputs(page, {
          citationType: "Änderung",
          court: prefilledDocumentUnit.coreData.court?.label,
          fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
          documentType:
            prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
          decisionDate: "01.01.2020",
        })
        await page.getByLabel("Aktivzitierung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    const activeCitationContainer = page.getByLabel("Aktivzitierung")
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
    await page.reload()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)

    await page.getByLabel("Weitere Angabe").click()
    await page.getByLabel("Aktenzeichen der Aktivzitierung").fill("two")
    await page.getByLabel("Aktivzitierung speichern").click()
    // "Aktivzitierung speichern" only saves state in frontend, no communication to backend yet
    await page.reload()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)

    await page.getByLabel("Weitere Angabe").click()
    await waitForSaving(
      async () => {
        await page.getByLabel("Aktenzeichen der Aktivzitierung").fill("two")
        await page.getByLabel("Aktivzitierung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.reload()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
  })

  test("manually added active citations can be edited", async ({
    page,
    documentNumber,
  }) => {
    const fileNumber1 = generateString()
    const fileNumber2 = generateString()
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen der Aktivzitierung")
          .fill(fileNumber1)
      },
      page,
      { clickSaveButton: true },
    )
    await page.getByLabel("Aktivzitierung speichern").click()
    await expect(page.getByText(fileNumber1)).toBeVisible()

    await page.getByLabel("Eintrag bearbeiten").click()
    await waitForSaving(
      async () => {
        await page
          .getByLabel("Aktenzeichen der Aktivzitierung")
          .fill(fileNumber2)
      },
      page,
      { clickSaveButton: true },
    )
    await page.getByLabel("Aktivzitierung speichern").click()
    await expect(page.getByText(fileNumber1)).toBeHidden()
    await expect(page.getByText(fileNumber2)).toBeVisible()
  })

  test("manually added active citations can be deleted", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page.getByLabel("Aktenzeichen der Aktivzitierung").fill("one")
      },
      page,
      { clickSaveButton: true },
    )

    await page.getByLabel("Aktivzitierung speichern").click()
    await page.getByLabel("Weitere Angabe").click()
    await waitForSaving(
      async () => {
        await page.getByLabel("Aktenzeichen der Aktivzitierung").fill("two")
      },
      page,
      { clickSaveButton: true },
    )
    const activeCitationContainer = page.getByLabel("Aktivzitierung")
    await page.getByLabel("Aktivzitierung speichern").click()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
    await page.getByLabel("Eintrag löschen").first().click()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
  })

  // Todo enable again when linking possible
  // eslint-disable-next-line playwright/no-skipped-test
  test.skip("search for documentunits and link as active citation", async ({
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

    await fillActiveCitationInputs(page, {
      citationType: "Änderung",
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    const activeCitationContainer = page.getByLabel("Aktivzitierung")
    await activeCitationContainer.getByLabel("Nach Entscheidung suchen").click()
    await expect(
      activeCitationContainer.getByText("1 Ergebnis gefunden."),
    ).toBeVisible()

    //citation style ignored in search results
    const result = page.getByText(
      `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil, ${prefilledDocumentUnit.documentNumber}`,
    )

    await expect(result).toBeVisible()
    await page.getByLabel("Treffer übernehmen").click()

    //make sure to have citation style in list
    const listItem = page.getByText(
      `Änderung, AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil, ${prefilledDocumentUnit.documentNumber}`,
    )
    await expect(listItem).toBeVisible()
    await expect(page.getByLabel("Eintrag löschen")).toBeVisible()

    //can be edited
    await expect(page.getByLabel("Eintrag bearbeiten")).toBeVisible()

    // search for same parameters gives same result, indication that decision is already added
    await activeCitationContainer.getByLabel("Weitere Angabe").click()
    await fillActiveCitationInputs(page, {
      citationType: "Änderung",
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })

    await activeCitationContainer.getByLabel("Nach Entscheidung suchen").click()

    await expect(
      activeCitationContainer.getByText("1 Ergebnis gefunden."),
    ).toBeVisible()
    await expect(
      activeCitationContainer.getByText("Bereits hinzugefügt"),
    ).toBeVisible()

    //can be deleted
    await page.getByLabel("Eintrag löschen").first().click()
    await expect(
      activeCitationContainer.getByLabel("Listen Eintrag"),
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

    await fillActiveCitationInputs(page, {
      fileNumber: "abc",
    })
    await page.getByLabel("Aktivzitierung speichern").click()

    await expect(page.getByLabel("Fehlerhafte Eingabe")).toBeVisible()
    await page.getByLabel("Eintrag bearbeiten").click()
    await expect(
      page.getByLabel("Aktivzitierung").getByText("Pflichtfeld nicht befüllt"),
    ).toHaveCount(3)

    await fillActiveCitationInputs(page, {
      citationType: "Änderung",
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.jurisShortcut,
      decisionDate: "01.01.2020",
    })
    await page.getByLabel("Aktivzitierung speichern").click()

    await expect(page.getByLabel("Fehlerhafte Eingabe")).toBeHidden()
  })

  test("adding empty active citation not possible", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await page.getByLabel("Aktivzitierung speichern").isDisabled()
  })

  test("incomplete date input shows error message and does not persist", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await page
      .locator("[aria-label='Entscheidungsdatum der Aktivzitierung']")
      .fill("03")

    await page.keyboard.press("Tab")

    await expect(
      page.locator("[aria-label='Entscheidungsdatum der Aktivzitierung']"),
    ).toHaveValue("03")

    await expect(page.locator("text=Unvollständiges Datum")).toBeVisible()

    await page.reload()

    await expect(
      page.locator("[aria-label='Entscheidungsdatum der Aktivzitierung']"),
    ).toHaveValue("")
  })
})

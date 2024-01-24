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
  test("renders empty previous decision in edit mode, when none in list", async ({
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
      page.locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']"),
    ).toBeVisible()
    await expect(
      page.getByLabel("Dokumenttyp Vorgehende Entscheidung"),
    ).toBeVisible()
    await expect(page.getByLabel("Datum unbekannt")).toBeVisible()
  })

  test("create and renders new previous decision in list", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.label,
      decisionDate: "01.01.2020",
    })

    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(
      page.getByText(
        `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil`,
        {
          exact: true,
        },
      ),
    ).toBeVisible()

    const previousDecisionContainer = page.getByLabel("Vorgehende Entscheidung")

    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)

    await page.getByLabel("Weitere Angabe").click()
    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.label,
      decisionDate: "01.01.2020",
    })

    await page.getByLabel("Vorgehende Entscheidung speichern").click()

    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
  })

  test("saving behaviour of previous decision", async ({
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
          documentType: prefilledDocumentUnit.coreData.documentType?.label,
          decisionDate: "01.01.2020",
        })

        await page.getByLabel("Vorgehende Entscheidung speichern").click()
        await expect(
          page.getByText(
            `AG Aachen, 01.01.2020, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil`,
            {
              exact: true,
            },
          ),
        ).toBeVisible()
      },
      page,
      { clickSaveButton: true },
    )

    const previousDecisionContainer = page.getByLabel("Vorgehende Entscheidung")

    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)

    await page.reload()

    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1, { timeout: 10000 }) // reloading can be slow if too many parallel tests

    // Change with commit reload by saving
    // await page.getByLabel("Weitere Angabe").click()
    // await page.getByLabel("Aktenzeichen Vorgehende Entscheidung").fill("two")
    // await page.getByLabel("Vorgehende Entscheidung speichern").click()
    // await expect(
    //   page.getByText(`two`, {
    //     exact: true,
    //   }),
    // ).toBeVisible()
    // // "Vorgehende Entscheidung speichern" only saves state in frontend, no communication to backend yet
    // await page.reload()
    // await expect(
    //   previousDecisionContainer.getByLabel("Listen Eintrag"),
    // ).toHaveCount(1, { timeout: 10000 }) // reloading can be slow if too many parallel tests

    await page.getByLabel("Weitere Angabe").click()
    await waitForSaving(
      async () => {
        await page
          .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
          .fill("two")
        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.reload()
    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2, { timeout: 20000 })
  })

  test("manually added previous decision can be edited", async ({
    page,
    documentNumber,
  }) => {
    const fileNumber1 = generateString()
    const fileNumber2 = generateString()
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page
          .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
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
          .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
          .fill(fileNumber2)
      },
      page,
      { clickSaveButton: true },
    )
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(page.getByText(fileNumber1)).toBeHidden()
    await expect(page.getByText(fileNumber2)).toBeVisible()
  })

  test("manually added previous decision can be deleted", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await waitForSaving(
      async () => {
        await page
          .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
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
          .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
          .fill("two")
      },
      page,
      { clickSaveButton: true },
    )
    const previousDecisionContainer = page.getByLabel("Vorgehende Entscheidung")
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(2)
    await page.getByLabel("Eintrag löschen").first().click()
    await expect(
      previousDecisionContainer.getByLabel("Listen Eintrag"),
    ).toHaveCount(1)
  })

  // eslint-disable-next-line playwright/no-skipped-test
  test("search for documentunits and link as previous decision with deviating file number", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    const deviatingFileNumber1 = generateString()
    const deviatingFileNumber2 = generateString()

    await navigateToPublication(
      page,
      prefilledDocumentUnit.documentNumber || "",
    )

    await page
      .locator("[aria-label='Dokumentationseinheit veröffentlichen']")
      .click()
    await expect(page.locator("text=Email wurde versendet")).toBeVisible()

    await expect(page.locator("text=Xml Email Abgabe -")).toBeVisible()
    await expect(page.locator("text=In Veröffentlichung")).toBeVisible()

    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.label,
      decisionDate: "31.12.2019",
      deviatingFileNumber: deviatingFileNumber1,
    })
    const previousDecisionContainer = page.getByLabel("Vorgehende Entscheidung")
    await previousDecisionContainer
      .getByLabel("Nach Entscheidung suchen")
      .click()

    await expect(
      page.getByText(
        `AG Aachen, 31.12.2019, ${prefilledDocumentUnit.coreData.fileNumbers?.[0]}, Anerkenntnisurteil, ${prefilledDocumentUnit.documentNumber}`,
      ),
    ).toBeVisible()
    await page.getByLabel("Treffer übernehmen").click()

    //deviating file number can be edited
    await expect(page.getByLabel("Eintrag bearbeiten")).toBeVisible()
    await page.getByLabel("Eintrag bearbeiten").first().click()

    await expect(
      page.getByLabel("Abweichendes Aktenzeichen Vorgehende Entscheidung"),
    ).toHaveValue(deviatingFileNumber1)

    await waitForSaving(
      async () => {
        await page
          .getByLabel("Abweichendes Aktenzeichen Vorgehende Entscheidung")
          .fill(deviatingFileNumber2)

        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.getByLabel("Eintrag bearbeiten").first().click()
    await expect(page.getByText(deviatingFileNumber2)).toBeVisible()
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
      page
        .getByLabel("Vorgehende Entscheidung")
        .getByText("Pflichtfeld nicht befüllt"),
    ).toHaveCount(2)

    await fillPreviousDecisionInputs(page, {
      court: prefilledDocumentUnit.coreData.court?.label,
      fileNumber: prefilledDocumentUnit.coreData.fileNumbers?.[0],
      documentType: prefilledDocumentUnit.coreData.documentType?.label,
      decisionDate: "01.01.2020",
    })
    await page.getByLabel("Vorgehende Entscheidung speichern").click()

    await expect(page.getByLabel("Fehlerhafte Eingabe")).toBeHidden()
  })

  test("adding empty previous decision not possible", async ({
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

    await page
      .locator("[aria-label='Entscheidungsdatum Vorgehende Entscheidung']")
      .fill("03")

    await page.keyboard.press("Tab")

    await expect(
      page.locator("[aria-label='Entscheidungsdatum Vorgehende Entscheidung']"),
    ).toHaveValue("03")

    await expect(page.locator("text=Unvollständiges Datum")).toBeVisible()

    await page.reload()

    await expect(
      page.locator("[aria-label='Entscheidungsdatum Vorgehende Entscheidung']"),
    ).toHaveValue("")
  })

  test("no date is displayed or sent if date known is false", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    await expect(page.getByText(documentNumber)).toBeVisible()

    await page
      .locator("[aria-label='Datum Unbekannt Vorgehende Entscheidung']")
      .click()

    await expect(
      page.locator("[aria-label='Entscheidungsdatum Vorgehende Entscheidung']"),
    ).toBeHidden()

    await page
      .locator("[aria-label='Datum Unbekannt Vorgehende Entscheidung']")
      .click()

    await page
      .locator("[aria-label='Entscheidungsdatum Vorgehende Entscheidung']")
      .isVisible()
  })

  test("deviating file number can be added and edited for manually added previous decisions", async ({
    page,
    documentNumber,
  }) => {
    const fileNumber = generateString()
    const deviatingFileNumber1 = generateString()
    const deviatingFileNumber2 = generateString()
    await navigateToCategories(page, documentNumber)

    await page
      .locator("[aria-label='Aktenzeichen Vorgehende Entscheidung']")
      .fill(fileNumber)
    await page
      .locator(
        "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung anzeigen']",
      )
      .click()

    // Knödel
    const container = page
      .locator("[aria-label='Vorgehende Entscheidung']")
      .first()
    await expect(
      container.locator("text=Abweichendes Aktenzeichen").first(),
    ).toBeVisible()

    await waitForSaving(
      async () => {
        await page
          .locator(
            "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung']",
          )
          .fill(deviatingFileNumber1)

        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.getByLabel("Eintrag bearbeiten").click()
    await page
      .locator(
        "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung anzeigen']",
      )
      .click()
    // TODO why doesn't "await expect(page.getByText(deviatingFileNumber1)).toBeVisible()" work?
    await expect(
      page.locator(
        "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung']",
      ),
    ).toHaveValue(deviatingFileNumber1)

    await waitForSaving(
      async () => {
        await page
          .locator(
            "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung']",
          )
          .fill(deviatingFileNumber2)

        await page.getByLabel("Vorgehende Entscheidung speichern").click()
      },
      page,
      { clickSaveButton: true },
    )

    await page.getByLabel("Eintrag bearbeiten").click()
    await page
      .locator(
        "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung anzeigen']",
      )
      .click()
    // TODO why doesn't "await expect(page.getByText(deviatingFileNumber2)).toBeVisible()" work?
    await expect(
      page.locator(
        "[aria-label='Abweichendes Aktenzeichen Vorgehende Entscheidung']",
      ),
    ).toHaveValue(deviatingFileNumber2)
  })
})

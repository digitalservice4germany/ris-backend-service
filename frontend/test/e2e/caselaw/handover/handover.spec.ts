import { expect } from "@playwright/test"
import {
  fillActiveCitationInputs,
  fillEnsuingDecisionInputs,
  fillPreviousDecisionInputs,
  navigateToCategories,
  navigateToHandover,
  save,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe("ensuring the handover of documentunits works as expected", () => {
  test("handover page shows all possible missing required fields when no fields filled", async ({
    page,
    documentNumber,
  }) => {
    await navigateToHandover(page, documentNumber)

    await expect(page.locator("li:has-text('Aktenzeichen')")).toBeVisible()
    await expect(page.locator("li:has-text('Gericht')")).toBeVisible()
    await expect(
      page.locator("li:has-text('Entscheidungsdatum')"),
    ).toBeVisible()
    await expect(page.locator("li:has-text('Dokumenttyp')")).toBeVisible()
  })

  test("handover page shows missing required fields of previous decisions", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await fillPreviousDecisionInputs(page, {
      court: "AG Aalen",
    })
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await save(page)

    await expect(
      page.getByText(`AG Aalen`, {
        exact: true,
      }),
    ).toBeVisible()
    await navigateToHandover(page, documentNumber)

    await expect(
      page.locator("li:has-text('Vorgehende Entscheidungen')"),
    ).toBeVisible()

    await expect(
      page
        .locator("li:has-text('Vorgehende Entscheidungen')")
        .getByText("Entscheidungsdatum"),
    ).toBeVisible()
    await expect(
      page
        .locator("li:has-text('Vorgehende Entscheidungen')")
        .getByText("Aktenzeichen"),
    ).toBeVisible()
  })

  // RISDEV-2183
  test("handover page shows missing required fields of previous decisions with only missing fields in previous decisions", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

    await fillPreviousDecisionInputs(page, {
      court: "AG Aalen",
    })
    await page.getByLabel("Vorgehende Entscheidung speichern").click()
    await save(page)

    await expect(
      page.getByText(`AG Aalen`, {
        exact: true,
      }),
    ).toBeVisible()

    await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)

    await expect(
      page.locator("li:has-text('Vorgehende Entscheidungen')"),
    ).toBeVisible()

    await expect(
      page
        .locator("li:has-text('Vorgehende Entscheidungen')")
        .getByText("Entscheidungsdatum"),
    ).toBeVisible()
    await expect(
      page
        .locator("li:has-text('Vorgehende Entscheidungen')")
        .getByText("Aktenzeichen"),
    ).toBeVisible()
  })

  test("handover page shows missing required fields of ensuing decisions", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await fillEnsuingDecisionInputs(page, {
      court: "AG Aalen",
    })
    await page.getByLabel("Nachgehende Entscheidung speichern").click()
    await save(page)

    await expect(
      page.getByText(`nachgehend, AG Aalen`, {
        exact: true,
      }),
    ).toBeVisible()
    await navigateToHandover(page, documentNumber)

    await expect(
      page.locator("li:has-text('Nachgehende Entscheidungen')"),
    ).toBeVisible()

    await expect(
      page
        .locator("li:has-text('Nachgehende Entscheidungen')")
        .getByText("Entscheidungsdatum"),
    ).toBeVisible()
    await expect(
      page
        .locator("li:has-text('Nachgehende Entscheidungen')")
        .getByText("Aktenzeichen"),
    ).toBeVisible()
  })

  test("handover page does not show missing required decision date, when ensuing decision is pending", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await fillEnsuingDecisionInputs(page, {
      pending: true,
      court: "AG Aalen",
      fileNumber: "123",
    })
    await page.getByLabel("Nachgehende Entscheidung speichern").click()
    await save(page)
    await expect(
      page.getByText(`anhängig, AG Aalen, Datum unbekannt, 123`, {
        exact: true,
      }),
    ).toBeVisible()
    await navigateToHandover(page, documentNumber)

    await expect(
      page.locator("li:has-text('Nachgehende Entscheidungen')"),
    ).toBeHidden()
  })

  test("handover page shows missing required fields of active citations", async ({
    page,
    documentNumber,
    prefilledDocumentUnit,
  }) => {
    await navigateToCategories(page, documentNumber)

    await fillActiveCitationInputs(page, {
      documentType: prefilledDocumentUnit.coreData.documentType?.label,
    })
    await page.getByLabel("Aktivzitierung speichern").click()
    await save(page)

    await navigateToHandover(page, documentNumber)

    await expect(page.locator("li:has-text('Aktivzitierung')")).toBeVisible()

    await expect(
      page
        .locator("li:has-text('Aktivzitierung')")
        .getByText("Art der Zitierung"),
    ).toBeVisible()
    await expect(
      page.locator("li:has-text('Aktivzitierung')").getByText("Gericht"),
    ).toBeVisible()
    await expect(
      page
        .locator("li:has-text('Aktivzitierung')")
        .getByText("Entscheidungsdatum"),
    ).toBeVisible()
    await expect(
      page.locator("li:has-text('Aktivzitierung')").getByText("Aktenzeichen"),
    ).toBeVisible()
  })

  // Todo: very flaky
  // eslint-disable-next-line playwright/no-skipped-test
  test.skip("handover page updates missing required fields after fields were updated", async ({
    page,
    documentNumber,
  }) => {
    await navigateToHandover(page, documentNumber)
    await expect(page.locator("li:has-text('Aktenzeichen')")).toBeVisible()
    await expect(page.locator("li:has-text('Gericht')")).toBeVisible()
    await expect(
      page.locator("li:has-text('Entscheidungsdatum')"),
    ).toBeVisible()
    await expect(page.locator("li:has-text('Dokumenttyp')")).toBeVisible()
    await page.locator("[aria-label='Rubriken bearbeiten']").click()

    await page.locator("[aria-label='Aktenzeichen']").fill("abc")
    await page.keyboard.press("Enter")
    await save(page)

    await page.locator("[aria-label='Gericht']").fill("aalen")
    await page.getByText("AG Aalen").click()

    await expect(page.locator("[aria-label='Gericht']")).toHaveValue("AG Aalen")

    await navigateToHandover(page, documentNumber)
    await expect(page.locator("li:has-text('Aktenzeichen')")).toBeHidden()
    await expect(page.locator("li:has-text('Gericht')")).toBeHidden()
    await expect(
      page.locator("li:has-text('Entscheidungsdatum')"),
    ).toBeVisible()
    await expect(page.locator("li:has-text('Dokumenttyp')")).toBeVisible()
  })

  test("handover not possible if required fields missing", async ({
    page,
    documentNumber,
  }) => {
    await navigateToHandover(page, documentNumber)

    await page
      .locator("[aria-label='Dokumentationseinheit an jDV übergeben']")
      .click()
    await expect(
      page.getByText("Es sind noch nicht alle Pflichtfelder befüllt."),
    ).toBeVisible()
  })

  test("handover possible when all required fields filled", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)

    await expect(page.getByText("XML Vorschau")).toBeVisible()

    await page.getByText("XML Vorschau").click()

    await expect(
      page.locator("text='        <entsch-datum>2019-12-31</entsch-datum>'"),
    ).toBeVisible()

    await expect(
      page.getByText("Alle Pflichtfelder sind korrekt ausgefüllt"),
    ).toBeVisible()

    await expect(
      page.locator(
        "text=Diese Dokumentationseinheit wurde bisher nicht an die jDV übergeben",
      ),
    ).toBeVisible()

    await page
      .locator("[aria-label='Dokumentationseinheit an jDV übergeben']")
      .click()

    await expect(page.getByText("Email wurde versendet")).toBeVisible()

    await expect(page.getByText("Xml Email Abgabe -")).toBeVisible()
  })

  test(
    "handover does not change publication status",
    {
      annotation: [
        {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4293",
        },
      ],
    },
    async ({ page, prefilledDocumentUnit }) => {
      await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
      await expect(page.getByText("unveröffentlicht")).toBeVisible()

      await expect(
        page.getByText("Alle Pflichtfelder sind korrekt ausgefüllt"),
      ).toBeVisible()

      await page
        .locator("[aria-label='Dokumentationseinheit an jDV übergeben']")
        .click()

      await expect(page.getByText("Email wurde versendet")).toBeVisible()
      await expect(page.getByText("Xml Email Abgabe -")).toBeVisible()
      await expect(page.getByText("unveröffentlicht")).toBeVisible()
    },
  )
})

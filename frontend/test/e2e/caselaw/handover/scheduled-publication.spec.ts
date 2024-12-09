import { expect, Page } from "@playwright/test"
import dayjs from "dayjs"
import {
  handoverDocumentationUnit,
  navigateToHandover,
  navigateToSearch,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"

test.describe(
  "Scheduled publication (Terminierte Abgabe)",
  {
    annotation: {
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-5234",
      type: "epic",
    },
    tag: ["@RISDEV-5234"],
  },
  () => {
    test(
      "Terminierte Abgabe kann erstellt werden",
      {
        annotation: {
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-5419",
          type: "story",
        },
        tag: ["@RISDEV-5419"],
      },
      async ({ page, prefilledDocumentUnit }) => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber)

        await test.step("Terminierte Abgabe kann befüllt werden", async () => {
          // Datum will be enabled once XML preview is loaded
          await expect(page.getByLabel("Terminiertes Datum")).toBeEditable()
          await expect(page.getByLabel("Terminiertes Datum")).toBeEmpty()
          await expect(page.getByLabel("Terminierte Uhrzeit")).toBeEditable()
          await expect(page.getByLabel("Terminierte Uhrzeit")).toHaveValue(
            "05:00",
          )
          await expect(page.getByLabel("Termin setzen")).toBeDisabled()
        })

        await test.step("Terminierte Abgabe in der Vergangenheit ist nicht möglich", async () => {
          await page.getByLabel("Terminiertes Datum").fill("01.01.2020")
          await expect(
            page.getByTestId("scheduledPublishingDate_errors"),
          ).toHaveText("Der Terminierungszeitpunkt muss in der Zukunft liegen.")
          await expect(page.getByLabel("Termin setzen")).toBeDisabled()
        })

        await test.step("Setze terminierte Abgabe in der Zukunft", async () => {
          await page.getByLabel("Terminiertes Datum").fill("31.12.2080")
          await page.getByLabel("Terminierte Uhrzeit").fill("13:14")
          await expect(
            page.getByTestId("scheduledPublishingDate_errors"),
          ).toBeHidden()
          await expect(page.getByLabel("Termin setzen")).toBeEnabled()
          await page.getByLabel("Termin setzen").click()

          await expect(page.getByLabel("Terminiertes Datum")).not.toBeEditable()
          await expect(
            page.getByLabel("Terminierte Uhrzeit"),
          ).not.toBeEditable()
          await expect(
            page.getByLabel("Dokumentationseinheit an jDV übergeben"),
          ).toBeDisabled()
        })

        await test.step("Terminierte Abgabe hat korrekte Daten nach Reload", async () => {
          await page.reload()
          await expect(page.getByLabel("Terminiertes Datum")).toHaveValue(
            "31.12.2080",
          )
          await expect(page.getByLabel("Terminierte Uhrzeit")).toHaveValue(
            "13:14",
          )
          await expect(
            page.getByTestId("scheduledPublishingDate_errors"),
          ).toBeHidden()
        })

        await test.step("Lösche terminierte Abgabe", async () => {
          await expect(page.getByLabel("Termin löschen")).toBeEnabled()
          await page.getByLabel("Termin löschen").click()
          await expect(
            page.getByLabel("Dokumentationseinheit an jDV übergeben"),
          ).toBeEnabled()

          await expect(page.getByLabel("Terminiertes Datum")).toBeEditable()
          await expect(page.getByLabel("Terminiertes Datum")).toBeEmpty()
          await expect(page.getByLabel("Terminierte Uhrzeit")).toBeEditable()
          await expect(page.getByLabel("Terminierte Uhrzeit")).toHaveValue(
            "05:00",
          )
        })
      },
    )

    test(
      "Terminierte Abgabe ist nicht möglich, wenn Plausibilitätsprüfung fehlschlägt",
      {
        annotation: {
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-5419",
          type: "story",
        },
        tag: ["@RISDEV-5419"],
      },
      async ({ page, documentNumber }) => {
        await navigateToHandover(page, documentNumber)

        await test.step("Terminierte Abgabe ist nicht möglich", async () => {
          await expect(page.getByLabel("Plausibilitätsprüfung")).toHaveText(
            /Die folgenden Rubriken-Pflichtfelder sind nicht befüllt/,
          )

          await expect(page.getByLabel("Terminiertes Datum")).toBeDisabled()
          await expect(page.getByLabel("Terminierte Uhrzeit")).toBeDisabled()
          await expect(page.getByLabel("Termin setzen")).toBeDisabled()
        })
      },
    )

    test(
      "Terminierte Dok-Einheiten können in Suche gefunden werden",
      {
        annotation: {
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-5419",
          type: "story",
        },
        tag: ["@RISDEV-5419"],
      },
      async ({ page, prefilledDocumentUnit }) => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber)

        await test.step("Setze terminierte Abgabe in der Zukunft", async () => {
          await page.getByLabel("Terminiertes Datum").fill("31.12.2080")
          await page.getByLabel("Terminierte Uhrzeit").fill("13:14")
          await page.getByLabel("Termin setzen").click()
        })

        await navigateToSearch(page)

        await test.step("Suchfilter für Terminierte Abgabe nur mit 'Nur meine Dokstelle' sichtbar", async () => {
          await expect(page.getByLabel("Terminiert Filter")).toBeHidden()
          await page.getByLabel("Nur meine Dokstelle Filter").check()
          await expect(page.getByLabel("Terminiert Filter")).toBeVisible()
          // jdv Übergabe column is not visible by default
          await expect(
            page.getByRole("cell", { name: "jDV Übergabe", exact: true }),
          ).toBeHidden()
        })

        await test.step("Suche nach terminierten Dok-Einheiten zeigt neue Terminierung", async () => {
          await page.getByLabel("Terminiert Filter").check()
          await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
          const resultRow = page.getByTestId(
            `listEntry_${prefilledDocumentUnit.documentNumber}`,
          )
          // jDV Übergabe date column is visible when filter is active
          await expect(
            page.getByRole("cell", { name: "jDV Übergabe", exact: true }),
          ).toBeVisible()
          await expect(resultRow).toBeVisible()
          await expect(
            resultRow.getByLabel("Terminierte Übergabe am 31.12.2080 13:14"),
          ).toBeVisible()
          await expect(resultRow.getByTestId("publicationDate")).toHaveText(
            "31.12.2080 13:14",
          )
        })

        await checkPublicationDateSorting(page)
      },
    )

    test(
      "Abgegebene Dok-Einheiten können in Suche nach Abgabedatum gefiltert werden",
      {
        annotation: {
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-5419",
          type: "story",
        },
        tag: ["@RISDEV-5419"],
      },
      async ({ page, prefilledDocumentUnit, secondPrefilledDocumentUnit }) => {
        await handoverDocumentationUnit(
          page,
          prefilledDocumentUnit.documentNumber,
        )
        await handoverDocumentationUnit(
          page,
          secondPrefilledDocumentUnit.documentNumber,
        )

        await navigateToSearch(page)

        await test.step("Suchfilter für Abgabedatum nur mit 'Nur meine Dokstelle' sichtbar", async () => {
          await expect(page.getByLabel("jDV Übergabedatum Suche")).toBeHidden()
          await page.getByLabel("Nur meine Dokstelle Filter").check()
          await expect(page.getByLabel("jDV Übergabedatum Suche")).toBeVisible()
          // jdv Übergabe column is not visible by default
          await expect(
            page.getByRole("cell", { name: "jDV Übergabe", exact: true }),
          ).toBeHidden()
        })

        await test.step("Suche nach Übergabedatum", async () => {
          await page
            .getByLabel("jDV Übergabedatum Suche")
            .fill(dayjs().format("DD.MM.YYYY"))

          await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
          // jDV Übergabe date column is visible when filter is active
          await expect(
            page.getByRole("cell", { name: "jDV Übergabe", exact: true }),
          ).toBeVisible()
        })

        await checkResultListForPublishedDocUnit(
          page,
          prefilledDocumentUnit.documentNumber,
        )
        await checkResultListForPublishedDocUnit(
          page,
          secondPrefilledDocumentUnit.documentNumber,
        )

        await checkPublicationDateSorting(page)
      },
    )
  },
)

async function checkPublicationDateSorting(page: Page) {
  await test.step("Dok-Einheiten sind nach jDV-Übergabe sortiert", async () => {
    const publicationDateCells = await page.getByTestId("publicationDate").all()
    const publicationDates = await Promise.all(
      publicationDateCells.map((date) => date.textContent()),
    ).then((dates) => dates.map((date) => dayjs(date, "DD.MM.YYYY HH:mm")))

    const areDatesSortedDesc = publicationDates.every(
      (date, i) =>
        date && (i === 0 || !publicationDates[i - 1]!.isBefore(date)),
    )
    expect(areDatesSortedDesc).toBe(true)
  })
}

async function checkResultListForPublishedDocUnit(
  page: Page,
  documentNumber: string,
) {
  await test.step(`Ergebnisliste enthält abgegebene Dok-Einheit ${documentNumber}`, async () => {
    const resultRow = page.getByTestId(`listEntry_${documentNumber}`)
    await expect(resultRow).toBeVisible()
    await expect(
      resultRow.getByLabel("Keine Übergabe terminiert"),
    ).toBeVisible()
    await expect(resultRow.getByTestId("publicationDate")).toContainText(
      dayjs().format("DD.MM.YYYY"),
    )
  })
}

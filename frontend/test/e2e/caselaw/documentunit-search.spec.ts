import { expect } from "@playwright/test"
import dayjs from "dayjs"
import errorMessages from "@/i18n/errors.json"

import {
  deleteDocumentUnit,
  fillInput,
  fillSearchInput,
  navigateToCategories,
  navigateToSearch,
  save,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"
import { noteContent } from "~/e2e/testdata"
import { generateString } from "~/test-helper/dataGenerators"

/* eslint-disable playwright/no-conditional-in-test */
test.describe("search", () => {
  test("renders search entry form", async ({ page }) => {
    await navigateToSearch(page)
    await expect(
      page.getByRole("button", {
        name: "Neue Dokumentationseinheit",
        exact: true,
      }),
    ).toBeVisible()

    await expect(
      page.getByRole("heading", { name: "Übersicht Rechtsprechung" }),
    ).toBeVisible()

    await expect(page.getByLabel("Aktenzeichen Suche")).toBeVisible()
    await expect(page.getByLabel("Gerichtstyp Suche")).toBeVisible()
    await expect(page.getByLabel("Gerichtsort Suche")).toBeVisible()
    await expect(
      page.getByLabel("Entscheidungsdatum Suche", {
        exact: true,
      }),
    ).toBeVisible()
    await expect(
      page.getByLabel("Entscheidungsdatum Suche Ende", {
        exact: true,
      }),
    ).toBeVisible()
    await expect(page.getByLabel("Dokumentnummer Suche")).toBeVisible()
    await expect(page.getByLabel("Status Suche")).toBeVisible()
    await expect(page.getByLabel("Nur meine Dokstelle Filter")).toBeVisible()

    await expect(
      page.getByText(
        "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit.",
      ),
    ).toBeVisible()
  })

  // Suchzustände
  test("renders search results", async ({ page, documentNumber }) => {
    await navigateToSearch(page)

    //initial state
    await expect(
      page.getByText(
        "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit.",
      ),
    ).toBeVisible()
    await expect(
      page.getByRole("button", {
        name: "Neue Dokumentationseinheit erstellen",
      }),
    ).toBeVisible()

    //results
    await page.getByLabel("Dokumentnummer Suche").fill(documentNumber)
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect
      .poll(async () =>
        page
          .locator(".table-row", {
            hasText: documentNumber,
          })
          .count(),
      )
      .toBe(1)

    //no results
    await page.getByLabel("Dokumentnummer Suche").fill("wrong document number")
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(
      page.locator(".table-row", {
        hasText: documentNumber,
      }),
    ).toBeHidden()
  })

  test("renders message when error occurs", async ({ page }) => {
    await navigateToSearch(page)

    //error
    await page.getByLabel("Nur meine Dokstelle Filter").click()
    await page.route("**/*", async (route) => {
      await route.fulfill({
        status: 404,
        contentType: "text/plain",
        body: "Not Found!",
      })

      await expect(
        page.getByText("Die Suchergebnisse konnten nicht geladen werden."),
      ).toBeVisible()
    })
  })

  test("starting search with all kinds of errors or no search parameters not possible", async ({
    page,
  }) => {
    await navigateToSearch(page)
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(
      page.getByText("Geben Sie mindestens ein Suchkriterium ein"),
    ).toBeVisible()

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill("24.12.2022")
    await page.getByLabel("Entscheidungsdatum Suche Ende").fill("29.02.2023")
    await expect(page.getByText("Kein valides Datum")).toBeVisible()
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(page.getByText("Fehler in Suchkriterien")).toBeVisible()

    await page.getByLabel("Entscheidungsdatum Suche Ende").fill("25.12.2022")
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(page.getByText("Fehler in Suchkriterien")).toBeHidden()
    await expect(
      page.getByText(errorMessages.SEARCH_RESULTS_NOT_FOUND.title),
    ).toBeVisible()
  })

  // Datumskomponente Zeitraum
  test("search for exact dates", async ({
    page,
    prefilledDocumentUnit,
    secondPrefilledDocumentUnit,
  }) => {
    //1st date input provided: display results matching exactly this date.
    await navigateToSearch(page)

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill(
        dayjs(prefilledDocumentUnit.coreData.decisionDate).format("DD.MM.YYYY"),
      )
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () =>
        page
          .locator(".table-row", {
            hasText: dayjs(prefilledDocumentUnit.coreData.decisionDate).format(
              "DD.MM.YYYY",
            ),
          })
          .count(),
      )
      .toBeGreaterThanOrEqual(1)

    await expect(
      page.locator(".table-row", {
        hasText: dayjs(
          secondPrefilledDocumentUnit.coreData.decisionDate,
        ).format("DD.MM.YYYY"),
      }),
    ).toHaveCount(0)

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill(
        dayjs(secondPrefilledDocumentUnit.coreData.decisionDate).format(
          "DD.MM.YYYY",
        ),
      )
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () =>
        page
          .locator(".table-row", {
            hasText: dayjs(
              secondPrefilledDocumentUnit.coreData.decisionDate,
            ).format("DD.MM.YYYY"),
          })
          .count(),
      )
      .toBeGreaterThanOrEqual(1)

    await expect(
      page.locator(".table-row", {
        hasText: dayjs(prefilledDocumentUnit.coreData.decisionDate).format(
          "DD.MM.YYYY",
        ),
      }),
    ).toHaveCount(0)
  })

  test("search results between two dates", async ({ page }) => {
    //Both inputs provided: display results matching the date range.
    await page.goto("/")

    await page.getByLabel("Dokumentnummer Suche").fill("YYTestDoc")

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill("02.02.2022")

    await page
      .getByLabel("Entscheidungsdatum Suche Ende", { exact: true })
      .fill("02.02.2024")

    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () => page.locator(".table-row").count())
      .toBeGreaterThanOrEqual(5)

    //Same date entered: Show results for the exact date
    await page
      .getByLabel("Entscheidungsdatum Suche Ende", { exact: true })
      .fill("02.02.2022")

    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () => page.locator(".table-row").count())
      .toBeGreaterThanOrEqual(1)
  })

  test("search for file number", async ({ page }) => {
    await navigateToSearch(page)

    await test.step("search for file number case insensitive works", async () => {
      await page.getByLabel("Aktenzeichen Suche").fill("FILEnumber1")
      await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
      await expect
        .poll(async () => page.getByText("YYTestDoc0001").count())
        .toBe(1)
    })

    await test.step("search for file number starting with", async () => {
      await page.getByLabel("Aktenzeichen Suche").fill("fileNumber")
      await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
      await expect
        .poll(async () => page.getByText("YYTestDoc0001").count())
        .toBe(1)
      await expect
        .poll(async () => page.locator(".table-row").count())
        .toBeGreaterThanOrEqual(4)
    })

    await test.step("search for file number ending does not work by default", async () => {
      await page.getByLabel("Aktenzeichen Suche").fill("Number1")
      await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
      await expect(
        page.getByText("Keine Suchergebnisse gefunden"),
      ).toBeVisible()
    })

    await test.step("search for file ending with does work when adding '%'", async () => {
      await page.getByLabel("Aktenzeichen Suche").fill("%number1")
      await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
      await expect
        .poll(async () => page.getByText("YYTestDoc0001").count())
        .toBe(1)
    })
  })

  test("search for status", async ({ page }) => {
    await navigateToSearch(page)

    await page.getByLabel("Dokumentnummer Suche").fill("YYTestDoc")

    await page.getByLabel("Nur meine Dokstelle Filter").click()
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () => page.getByText("Unveröffentlicht").count())
      .toBe(7)

    const select = page.locator(`select[id="status"]`)
    await select.selectOption("Veröffentlicht")

    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () =>
        page.getByText("Veröffentlicht", { exact: true }).count(),
      )
      .toBe(7)

    // only the unpublished in select should be counted.
    await expect
      .poll(async () => page.getByText("Unveröffentlicht").count())
      .toBe(1)
  })

  test("filter for documentunits with errors only", async ({ page }) => {
    await page.goto("/")

    await page.getByLabel("Dokumentnummer Suche").fill("YYTestDoc")
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    //15 + table header
    await expect.poll(async () => page.locator(".table-row").count()).toBe(16)

    const docofficeOnly = page.getByLabel("Nur meine Dokstelle Filter")
    await docofficeOnly.click()
    const errorsOnly = page.getByLabel(
      "Nur fehlerhafte Dokumentationseinheiten",
    )
    await errorsOnly.click()
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    //3 + table header
    await expect.poll(async () => page.locator(".table-row").count()).toBe(4)

    //unclick my dokstelle should also reset errors only filter
    await docofficeOnly.click()
    await expect(errorsOnly).toBeHidden()
  })

  test("appraisal/judicial body shown in test results", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    await navigateToSearch(page)

    await fillSearchInput(page, {
      documentNumber: prefilledDocumentUnit.documentNumber,
    })
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect
      .poll(async () =>
        page
          .locator(".table-row", {
            hasText: prefilledDocumentUnit.coreData.appraisalBody,
          })
          .count(),
      )
      .toBeGreaterThanOrEqual(1)
  })

  test("exisiting headnote/ guiding principle are indicated as icon", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    await navigateToSearch(page)

    await fillSearchInput(page, {
      documentNumber: prefilledDocumentUnit.documentNumber,
    })
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(page.getByTestId("headnote-principle-icon")).toBeVisible()
  })

  test(
    "existing note is indicated as icon",
    {
      annotation: {
        type: "story",
        description: "RISDEV-4176",
      },
    },
    async ({ page, prefilledDocumentUnit }) => {
      await test.step("fill notiz", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

        await page.getByRole("button", { name: "Seitenpanel öffnen" }).click()

        await fillInput(page, "Notiz Eingabefeld", noteContent)
        await save(page)
      })

      await test.step("search indicates by icon that doc unit has notiz", async () => {
        await navigateToSearch(page)

        await fillSearchInput(page, {
          documentNumber: prefilledDocumentUnit.documentNumber,
        })
        await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
        const trimmedNote = noteContent.slice(0, 50) + "..."
        await expect(page.getByLabel(trimmedNote)).toBeVisible()
      })

      await test.step("delete notiz", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

        await fillInput(page, "Notiz Eingabefeld", "")
        await save(page)
      })

      await test.step("search indicates by icon that doc unit has no notiz", async () => {
        await navigateToSearch(page)

        await fillSearchInput(page, {
          documentNumber: prefilledDocumentUnit.documentNumber,
        })
        await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
        await expect(page.getByLabel("Keine Notiz vorhanden")).toBeVisible()
      })
    },
  )

  test("displaying errors on focus and blur", async ({
    page,
    prefilledDocumentUnit,
    secondPrefilledDocumentUnit,
  }) => {
    await navigateToSearch(page)

    await page
      .getByLabel("Entscheidungsdatum Suche Ende")
      .fill(
        dayjs(prefilledDocumentUnit.coreData.decisionDate).format("DD.MM.YYYY"),
      )

    await page.getByLabel("Entscheidungsdatum Suche Ende").blur()
    await expect(page.getByText("Startdatum fehlt")).toBeVisible()

    await page.getByLabel("Entscheidungsdatum Suche", { exact: true }).focus()
    await expect(page.getByText("Startdatum fehlt")).toBeHidden()

    //on blur without changes error reappears
    await page.getByLabel("Entscheidungsdatum Suche", { exact: true }).blur()
    await expect(page.getByText("Startdatum fehlt")).toBeVisible()

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill(
        dayjs(secondPrefilledDocumentUnit.coreData.decisionDate).format(
          "DD.MM.YYYY",
        ),
      )

    await expect(page.getByText("Startdatum fehlt")).toBeHidden()
    await expect(
      page.getByText("Enddatum darf nicht vor Startdatum liegen"),
    ).toBeVisible()

    await page.getByLabel("Entscheidungsdatum Suche", { exact: true }).clear()
    await page.getByLabel("Entscheidungsdatum Suche", { exact: true }).blur()
    await expect(page.getByText("Startdatum fehlt")).toBeVisible()
    await expect(
      page.getByText("Enddatum darf nicht vor Startdatum liegen"),
    ).toBeHidden()

    // removes startdate missing error if 2nd date is removed
    await page
      .getByLabel("Entscheidungsdatum Suche Ende", { exact: true })
      .clear()
    await page
      .getByLabel("Entscheidungsdatum Suche Ende", { exact: true })
      .blur()
    await expect(page.getByText("Startdatum fehlt")).toBeHidden()
  })

  test("updating of date input errors and interdependent errors", async ({
    page,
  }) => {
    await navigateToSearch(page)

    const firstDate = page.getByLabel("Entscheidungsdatum Suche", {
      exact: true,
    })
    const secondDate = page.getByLabel("Entscheidungsdatum Suche Ende")
    const firstDateInput = page.getByTestId("decision-date-input")
    const secondDateInput = page.getByTestId("decision-date-end-input")

    await firstDate.fill("29.02.2022")
    await expect(firstDateInput.getByText("Kein valides Datum")).toBeVisible()
    await secondDate.fill("29.02.2022")
    await expect(secondDateInput.getByText("Kein valides Datum")).toBeVisible()

    await secondDate.clear()
    await secondDate.fill("28.02.2022")

    // no valid error "wins" against missing start date error
    await expect(firstDateInput.getByText("Kein valides Datum")).toBeVisible()
    await firstDate.clear()
    await expect(page.getByText("Startdatum fehlt")).toBeVisible()
    await firstDate.fill("28.02.2023")
    await expect(
      secondDateInput.getByText("Enddatum darf nicht vor Startdatum liegen"),
    ).toBeVisible()

    // no valid error "wins" against range error
    await secondDate.clear()
    await secondDate.fill("29.02.2022")
    await expect(secondDateInput.getByText("Kein valides Datum")).toBeVisible()
  })

  // Suche zurücksetzen
  test("resetting the search", async ({ page }) => {
    // on input button is visible
    await navigateToSearch(page)
    const resetSearch = page.getByLabel("Suche zurücksetzen")
    const searchTerm = generateString()
    await expect(resetSearch).toBeHidden()
    await page.getByLabel("Aktenzeichen Suche").fill(searchTerm)
    await expect(resetSearch).toBeVisible()
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect(page.getByLabel("Ladestatus")).toBeHidden()
    await expect(
      page.getByText(errorMessages.SEARCH_RESULTS_NOT_FOUND.title),
    ).toBeVisible()
    await resetSearch.click()
    await expect(page.getByText(searchTerm)).toBeHidden()
    await expect(
      page.getByText(errorMessages.SEARCH_RESULTS_NOT_FOUND.title),
    ).toBeHidden()
    await expect(
      page.getByText(
        "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit.",
      ),
    ).toBeVisible()
  })

  test("reload and navigation back in browser history persist search parameters in url", async ({
    page,
    prefilledDocumentUnit,
    secondPrefilledDocumentUnit,
  }) => {
    await navigateToSearch(page)

    await page
      .getByLabel("Entscheidungsdatum Suche", { exact: true })
      .fill(
        dayjs(prefilledDocumentUnit.coreData.decisionDate).format("DD.MM.YYYY"),
      )

    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()

    await expect(page).toHaveURL(/decisionDate=2019-12-31/)
    await page.reload()
    await expect(page).toHaveURL(/decisionDate=2019-12-31/)

    await page
      .getByLabel("Entscheidungsdatum Suche Ende", { exact: true })
      .fill(
        dayjs(secondPrefilledDocumentUnit.coreData.decisionDate).format(
          "DD.MM.YYYY",
        ),
      )
    await page.getByLabel("Nach Dokumentationseinheiten suchen").click()
    await expect(page).toHaveURL(
      /decisionDate=2019-12-31&decisionDateEnd=2020-01-01/,
    )

    await page.goBack()
    await expect(page).not.toHaveURL(
      /decisionDate=2019-12-31&decisionDateEnd=2020-01-01/,
    )
    await expect(page).toHaveURL(/decisionDate=2019-12-31/)
  })

  test("show button for creating new doc unit from search parameters", async ({
    page,
  }) => {
    await navigateToSearch(page)

    //initial state
    await expect(
      page.getByText(
        "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit.",
      ),
    ).toBeVisible()
    await expect(
      page.getByRole("button", {
        name: "Neue Dokumentationseinheit erstellen",
      }),
    ).toBeVisible()

    const fileNumber = generateString()
    const courtType = "AG"
    const courtLocation = "Lüneburg"
    const decisionDate = "25.12.1999"

    await test.step("all the data can be used", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
      })

      await expect(
        page.getByText(
          `${fileNumber}, ${courtType} ${courtLocation}, ${decisionDate}`,
        ),
      ).toBeVisible()
    })

    await test.step("only file number set", async () => {
      await fillSearchInput(page, {
        fileNumber,
      })
      await expect(
        page.getByText(`${fileNumber}, Gericht unbekannt, Datum unbekannt`),
      ).toBeVisible()
    })

    await test.step("only date set", async () => {
      await fillSearchInput(page, { decisionDate })
      await expect(
        page.getByText(
          `Aktenzeichen unbekannt, Gericht unbekannt, ${decisionDate}`,
        ),
      ).toBeVisible()
    })

    await test.step("invalid court", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType: "invalid",
        courtLocation,
        decisionDate,
      })
      await expect(
        page.getByText(`${fileNumber}, Gericht unbekannt, ${decisionDate}`),
      ).toBeVisible()
    })

    await test.step("decision date end is set, so decision date can not be used", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
        decisionDateEnd: "01.01.2002",
      })
      await expect(
        page.getByText(
          `${fileNumber}, ${courtType} ${courtLocation}, Datum unbekannt`,
        ),
      ).toBeVisible()
    })
  })

  test("do not show button for creating new doc unit, when filters set", async ({
    page,
    browserName,
  }) => {
    const fileNumber = generateString()
    const courtType = "AG"
    const courtLocation = "Lüneburg"
    const decisionDate = "25.12.1999"

    await test.step("document number is set, so nothing can be used", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
        documentNumber: "test",
      })
      await expect(
        page.getByRole("button", {
          name: "Neue Dokumentationseinheit erstellen",
        }),
      ).toBeVisible()
    })

    // Todo: Known error in firefox (NS_BINDING_ABORTED),
    // when navigating with a concurrent navigation triggered
    // eslint-disable-next-line playwright/no-wait-for-timeout
    if (browserName === "firefox") await page.waitForTimeout(500)

    await test.step("my docoffice only is set, so nothing can be used", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
        myDocOfficeOnly: true,
      })
      await expect(
        page.getByRole("button", {
          name: "Neue Dokumentationseinheit erstellen",
        }),
      ).toBeVisible()
    })

    // Todo: Known error in firefox (NS_BINDING_ABORTED),
    // when navigating with a concurrent navigation triggered
    // eslint-disable-next-line playwright/no-wait-for-timeout
    if (browserName === "firefox") await page.waitForTimeout(500)
    await test.step("status is set, so nothing can be used", async () => {
      await fillSearchInput(page, {
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
        status: "Veröffentlicht",
      })
      await expect(
        page.getByRole("button", {
          name: "Neue Dokumentationseinheit erstellen",
        }),
      ).toBeVisible()
    })
  })

  test("create new doc unit from search parameter and switch to categories page", async ({
    page,
  }) => {
    await navigateToSearch(page)

    const fileNumber = generateString()
    const courtType = "AG"
    const courtLocation = "Aachen"
    const decisionDate = "25.12.2000"

    await fillSearchInput(page, {
      fileNumber,
      courtType,
      courtLocation,
      decisionDate,
    })

    await expect(
      page.getByText(
        `${fileNumber}, ${courtType} ${courtLocation}, ${decisionDate}`,
      ),
    ).toBeVisible()

    await page
      .getByRole("button", {
        name: "Übernehmen und fortfahren",
      })
      .click()
    await page.waitForURL(/categories$/)

    const documentNumber = page
      .url()
      .match(/documentunit\/([A-Z0-9]{13})\/categories/)![1]

    const infoPanel = page.getByText(new RegExp(`${documentNumber}|.*`))

    await expect(page.locator("[aria-label='Gericht']")).toHaveValue(
      `${courtType} ${courtLocation}`,
    )
    await expect(
      infoPanel.getByText(
        `${courtType} ${courtLocation}, ${fileNumber}, ${decisionDate}`,
      ),
    ).toBeVisible()

    await deleteDocumentUnit(page, documentNumber)
  })

  test("show error message when creating new doc unit from search parameters fails", async ({
    page,
  }) => {
    await navigateToSearch(page)

    const fileNumber = generateString()

    await fillSearchInput(page, {
      fileNumber: fileNumber,
    })

    await expect(
      page.getByText(`${fileNumber}, Gericht unbekannt, Datum unbekannt`),
    ).toBeVisible()

    await page.route("**/new", async (route) => {
      await route.fulfill({
        status: 404,
        contentType: "text/plain",
        body: "Not Found!",
      })

      await expect(
        page.getByText(
          "Neue Dokumentationseinheit konnte nicht erstellt werden.",
        ),
      ).toBeVisible()
    })

    await page
      .getByRole("button", {
        name: "Übernehmen und fortfahren",
      })
      .click()
  })
})

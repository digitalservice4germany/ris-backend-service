import { expect, Page } from "@playwright/test"
import dayjs from "dayjs"
import {
  fillInput,
  navigateToPeriodicalEvaluation,
  navigateToPeriodicalReferences,
  navigateToPreview,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

const formattedDate = dayjs().format("DD.MM.YYYY")

/* eslint-disable playwright/no-conditional-in-test */

test.describe("Editing and deleting references in periodical evaluation", () => {
  test(
    "Editing references in periodical evaluation",
    {
      tag: "@RISDEV-4560",
    },
    async ({
      context,
      page,
      edition,
      prefilledDocumentUnit,
      secondPrefilledDocumentUnit,
    }) => {
      const fileNumber = prefilledDocumentUnit.coreData.fileNumbers?.[0] || ""
      const secondFileNumber =
        secondPrefilledDocumentUnit.coreData.fileNumbers?.[0] || ""
      const suffix = edition.suffix || ""

      await navigateToPeriodicalReferences(page, edition.id || "")

      await test.step("Citation shows selected prefix and suffix", async () => {
        await expect(page.getByLabel("Zitatstelle Präfix")).toHaveValue(
          "2024, ",
        )
        await expect(page.getByLabel("Zitatstelle Suffix")).toHaveValue(suffix)
      })

      await test.step("Prefix, suffix, Name and Legal Periodical can't be edited", async () => {
        await expect(page.locator("[aria-label='Periodikum']")).toBeHidden()

        await expect(
          page.locator("[aria-label='Zitatstelle Präfix']"),
        ).not.toBeEditable()
        await expect(
          page.locator("[aria-label='Zitatstelle Suffix']"),
        ).not.toBeEditable()
      })

      await test.step("should open and close document preview in side panel", async () => {
        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await openExtraContentSidePanelPreview(page, fileNumber)
        await expect(page.getByLabel("Seitenpanel öffnen")).toBeHidden()

        await closeExtraContentSidePanelPreview(page)
        await page.reload()
      })

      await test.step("open editing from side panel", async () => {
        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await openExtraContentSidePanelPreview(page, fileNumber)

        const newTabPromise = page.context().waitForEvent("page")
        await openDocumentationUnitEditModeTabThroughSidePanel(page)
        const newTab = await newTabPromise
        expect(newTab.url()).toContain("/categories")
        await newTab.close()
        await closeExtraContentSidePanelPreview(page)
        await page.reload() // to clean the search parameters.
      })

      await test.step("Citation input is validated when input is left", async () => {})

      await test.step("A docunit can be added as reference by entering citation and search fields", async () => {
        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")
        await expect(
          page.getByText(
            `AG Aachen, 31.12.2019, ${fileNumber}, Anerkenntnisurteil, Unveröffentlicht`,
          ),
        ).toBeVisible()
        await expect(page.getByText("Bereits hinzugefügt")).toBeHidden()
        await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(
          1,
        )

        await fillInput(page, "Zitatstelle *", "5")
        await fillInput(page, "Klammernzusatz", "LT")
        await page.getByLabel("Treffer übernehmen").click()
        await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(
          2,
        )
      })

      const secondPage = await context.newPage()

      await test.step("A docunit can be added to an edition multiple times", async () => {
        const saveRequest = page.waitForResponse(
          "**/api/v1/caselaw/legalperiodicaledition",
          { timeout: 5_000 },
        )
        await navigateToPeriodicalReferences(secondPage, edition.id)
        await searchForDocUnitWithFileNumber(page, fileNumber, "31.12.2019")

        await expect(
          page
            .getByTestId("reference-list-summary")
            .getByText(
              `AG Aachen, 31.12.2019, ${fileNumber}, Anerkenntnisurteil, Unveröffentlicht`,
            ),
        ).toBeVisible()

        await expect(page.getByText("Bereits hinzugefügt")).toBeVisible()
        await fillInput(page, "Zitatstelle *", "99")
        await fillInput(page, "Klammernzusatz", "LT")
        await page.getByLabel("Treffer übernehmen").click()

        const locator = page.locator("[aria-label='Listen Eintrag']")
        await saveRequest
        await expect(locator).toHaveCount(3)
      })

      await test.step("An added reference is shown on a second tab", async () => {
        const editionIntervalFetchResponse = secondPage.waitForResponse(
          `**/api/v1/caselaw/legalperiodicaledition/${edition.id}`,
          { timeout: 10_000 }, // auto fetch takes place every 10 seconds
        )

        await editionIntervalFetchResponse
        const secondPageLocator = secondPage.locator(
          "[aria-label='Listen Eintrag']",
        )
        await expect(secondPageLocator).toHaveCount(3)
        await secondPage.close()
      })

      await test.step("A reference is added to the editable list after being added", async () => {
        const decisionElement = page.getByText(
          `AG Aachen, 31.12.2019, ${fileNumber}, Anerkenntnisurteil`,
        )
        await expect(decisionElement).toHaveCount(2)

        // Assert that both elements are visible
        await expect(decisionElement.nth(0)).toBeVisible()
        await expect(decisionElement.nth(1)).toBeVisible()

        await expect(
          page.getByText(`MMG 2024, 5${suffix} (LT)`, { exact: true }),
        ).toBeVisible()
      })

      await test.step("Other docUnits can be added to an edition", async () => {
        await searchForDocUnitWithFileNumber(
          page,
          secondFileNumber,
          "01.01.2020",
        )
        await expect(
          page.getByText(
            `AG Aachen, 01.01.2020, ${secondFileNumber}, Anerkenntnisurteil, Unveröffentlicht`,
          ),
        ).toBeVisible()

        await fillInput(page, "Zitatstelle *", "104")
        await fillInput(page, "Klammernzusatz", "LT")

        await page.getByLabel("Treffer übernehmen").click()
        await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(
          4,
        )

        await expect(
          page.getByText(`MMG 2024, 104${suffix} (LT)`, { exact: true }),
        ).toBeVisible()
      })

      await test.step("The form is cleared after adding a reference", async () => {
        await expect(page.getByLabel("Zitatstelle *")).toBeEmpty()
        await expect(page.getByLabel("Klammernzusatz")).toBeEmpty()
        await expect(page.getByLabel("Gericht")).toBeEmpty()
        await expect(page.getByLabel("Aktenzeichen")).toBeEmpty()
        await expect(page.getByLabel("Entscheidungsdatum")).toBeEmpty()
        await expect(page.getByLabel("Dokumenttyp")).toBeEmpty()
      })

      // open documentation unit preview in new tab
      const previewTab = await context.newPage()
      await navigateToPreview(
        previewTab,
        prefilledDocumentUnit.documentNumber || "",
      )

      // open documentation unit preview in new tab
      const secondPreviewTab = await context.newPage()
      await navigateToPreview(
        secondPreviewTab,
        secondPrefilledDocumentUnit.documentNumber || "",
      )

      await test.step("An added citation is visible in the documentation unit's preview", async () => {
        await expect(
          previewTab.getByText(`MMG 2024, 5${suffix} (LT)`, { exact: true }),
        ).toHaveCount(1)
        await expect(
          previewTab.getByText(`MMG 2024, 99${suffix} (LT)`, { exact: true }),
        ).toHaveCount(1)
        await expect(
          secondPreviewTab.getByText(`MMG 2024, 104${suffix} (LT)`, {
            exact: true,
          }),
        ).toHaveCount(1)
      })

      await test.step("When editing a reference, the citation is a single input containing the joined value of prefix, citation and suffix", async () => {
        await expect(
          page.getByText(`MMG 2024, 5${suffix} (LT)`, { exact: true }),
        ).toBeVisible()

        await page.getByTestId("list-entry-0").click()

        await expect(
          page.getByText(`MMG 2024, 5${suffix} (LT)`, { exact: true }),
          "should not be hidden when entry extended",
        ).toBeHidden()

        await expect(page.getByLabel("Zitatstelle *")).toHaveValue(
          `2024, 5${suffix}`,
        )
        await expect(page.getByLabel("Klammernzusatz")).toHaveValue("LT")

        await expect(page.getByLabel("Zitatstelle Präfix")).toBeHidden()
        await expect(page.getByLabel("Zitatstelle Suffix")).toBeHidden()

        await expect(
          page
            .getByTestId("reference-input-summary")
            .getByText(
              `AG Aachen, 31.12.2019, ${fileNumber}, Anerkenntnisurteil, Unveröffentlicht`,
            ),
        ).toBeVisible()

        await expect(
          page.getByText("MMG 2024, 5, Heft uwkkf (LT)"),
          "Should hide the summary when in edit mode",
        ).toBeHidden()

        await expect(page.locator("[aria-label='Dokumenttyp']")).toBeHidden()

        // validate citation in edit mode (not allowed to be empty)
        await fillInput(page, "Zitatstelle *", "")
        await page.getByLabel("Fundstelle vermerken", { exact: true }).click()
        await expect(page.getByText("Pflichtfeld nicht befüllt")).toBeVisible()

        await fillInput(page, "Zitatstelle *", `2021, 2${suffix}`)
        await fillInput(page, "Klammernzusatz", "L")
        await page.getByLabel("Fundstelle vermerken", { exact: true }).click()

        await expect(
          page.getByText(`MMG 2021, 2${suffix} (L)`, { exact: true }),
        ).toBeVisible()
      })

      await test.step("Changes to the citation are visible in the documentation unit's preview", async () => {
        await previewTab.reload()
        await expect(
          previewTab.getByText(`MMG 2021, 2${suffix} (L)`, { exact: true }),
        ).toHaveCount(1, { timeout: 10_000 })
        await expect(
          previewTab.getByText(`MMG 2024, 99${suffix} (LT)`, { exact: true }),
        ).toHaveCount(1)
      })

      await test.step("Unchanged citation is unchanged in preview", async () => {
        await secondPreviewTab.reload()
        await expect(
          secondPreviewTab.getByText(`MMG 2024, 104${suffix} (LT)`, {
            exact: true,
          }),
        ).toHaveCount(1)
      })

      await test.step("The edition can't be deleted as long as it has references", async () => {
        await navigateToPeriodicalEvaluation(page)

        await fillInput(page, "Periodikum", "MMG")
        await page
          .getByText("MMG | Medizin Mensch Gesellschaft", { exact: true })
          .click()

        const line = page.getByText(
          (edition.name || "") + "MMG" + "3" + formattedDate,
        )

        await expect(line).toBeVisible()
        // delete button should not be clickable
        await expect(
          line.locator("[aria-label='Ausgabe löschen']"),
        ).toBeHidden()

        await expect(
          line
            .locator("[aria-label='Ausgabe kann nicht gelöscht werden']")
            .first(),
        ).toBeVisible()
      })
    },
  )

  test(
    "Page number resets when new search started",
    { tag: "@RISDEV-5434" },
    async ({ page, edition }) => {
      await test.step("Page number resets when new search started", async () => {
        await navigateToPeriodicalReferences(page, edition.id)

        await page.getByText("Suchen").click()
        await page.getByLabel("nächste Ergebnisse").click()
        await expect(page.getByText("Seite 2")).toBeVisible()
        await page.getByText("Suchen").click()

        await expect(page.getByText("Seite 2")).toBeHidden()
        await expect(page.getByText("Seite 1")).toBeVisible()
      })
    },
  )

  test(
    "Deleting references in periodical evaluation",
    {
      tag: "@RISDEV-5146",
    },
    async ({ context, page, editionWithReferences, prefilledDocumentUnit }) => {
      await test.step("A reference can be deleted", async () => {
        await navigateToPeriodicalReferences(
          page,
          editionWithReferences.id || "",
        )
        await expect(
          page.getByText(`MMG 2024, 12-22, Heft 1 (L)`, { exact: true }),
        ).toBeVisible()
        const count = page.getByLabel("Listen Eintrag")
        // 2 references + 1 empty in edit mode
        await expect(count).toHaveCount(3)

        await page.getByTestId("list-entry-0").click()

        await page.getByText("Eintrag löschen").click()

        await expect(
          page.locator("[aria-label='Eintrag löschen']"),
        ).toBeHidden()
        await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(
          1,
        )
        await expect(
          page.getByText(`MMG 2024, 12-22, Heft 1 (L)`, { exact: true }),
        ).toBeHidden()
      })

      await test.step("On reload deleted reference is not visible", async () => {
        await page.reload()
        await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(
          2,
        )
        await expect(
          page.getByText(`MMG 2024, 12-22, Heft 1 (L)`, { exact: true }),
        ).toBeHidden()
      })

      await test.step("Deleted references disappear from the documentation unit's preview", async () => {
        // open documentation unit preview in new tab

        const previewTab = await context.newPage()
        await navigateToPreview(
          previewTab,
          prefilledDocumentUnit.documentNumber || "",
        )
        await expect(
          page.getByText(`MMG 2024, 12-22, Heft 1 (L)`, { exact: true }),
        ).toBeHidden()
        await expect(
          previewTab.getByText("Fundstellen", { exact: true }),
        ).toBeHidden()
      })
    },
  )

  test("Adding references for other docoffices' documentation units", async ({
    context,
    page,
    edition,
    foreignDocumentationUnit,
  }) => {
    await test.step("Search for documentation unit of foreign doc office", async () => {
      await navigateToPeriodicalReferences(page, edition.id || "")
      const court = foreignDocumentationUnit.court?.label || ""
      const fileNumber = foreignDocumentationUnit.fileNumber || ""
      const date = foreignDocumentationUnit.decisionDate
        ? dayjs(foreignDocumentationUnit.decisionDate).format("DD.MM.YYYY")
        : ""

      await fillInput(page, "Zitatstelle *", "12")
      await fillInput(page, "Klammernzusatz", "L")
      await fillInput(page, "Gericht", court)
      await page.getByText(court, { exact: true }).click()
      await fillInput(page, "Aktenzeichen", fileNumber)
      await fillInput(page, "Entscheidungsdatum", date)

      await page.getByText("Suchen").click()

      await expect(
        page.getByText(
          `BAG, 02.01.1963, fileNumber1, Änderungsnorm, Veröffentlicht`,
        ),
      ).toBeVisible()
    })

    await test.step("An added reference is visible in the other docoffice's documentation unit's preview", async () => {
      await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(1)
      await page.getByLabel("Treffer übernehmen").click()
      await expect(page.locator("[aria-label='Listen Eintrag']")).toHaveCount(2)

      const previewTab = await context.newPage()
      await navigateToPreview(
        previewTab,
        foreignDocumentationUnit.documentNumber || "",
      )
      await expect(
        previewTab.getByText(`MMG 2024, 12${edition.suffix} (L)`, {
          exact: true,
        }),
      ).toBeVisible()
    })
  })

  test(
    "External user cannot edit or delete periodical editions",
    { tag: ["@RISDEV-4724", "@RISDEV-4519"] },
    async ({ pageWithExternalUser, edition }) => {
      await navigateToPeriodicalEvaluation(pageWithExternalUser)

      await test.step("A periodical can be selected using a combo box.", async () => {
        await fillInput(pageWithExternalUser, "Periodikum", "MMG")
        const periodical = pageWithExternalUser.getByText(
          "MMG | Medizin Mensch Gesellschaft" + "nicht amtlich",
          {
            exact: true,
          },
        )
        await expect(periodical).toBeVisible()
        await periodical.click()
        await waitForInputValue(
          pageWithExternalUser,
          "[aria-label='Periodikum']",
          "MMG",
        )
      })

      await test.step("User can view but not edit or delete the editions", async () => {
        await expect(
          pageWithExternalUser
            .getByLabel("Ausgabe kann nicht editiert werden")
            .first(),
        ).toBeVisible()
        await expect(
          pageWithExternalUser
            .getByLabel("Ausgabe kann nicht gelöscht werden")
            .first(),
        ).toBeVisible()

        await expect(
          pageWithExternalUser.getByText(
            (edition.name || "") + "MMG" + "0" + formattedDate,
          ),
        ).toBeVisible()

        await expect(
          pageWithExternalUser.getByLabel("Ausgabe editieren"),
        ).toBeHidden()
        await expect(
          pageWithExternalUser.getByLabel("Ausgabe löschen"),
        ).toBeHidden()
      })
    },
  )

  async function openExtraContentSidePanelPreview(
    page: Page,
    fileNumber: string,
  ) {
    await page.getByTestId(`document-number-link-${fileNumber}`).click()
    await expect(page).toHaveURL(/showAttachmentPanel=true/)
  }

  async function closeExtraContentSidePanelPreview(page: Page) {
    await page.getByLabel("Seitenpanel schließen").click()
    await expect(page).toHaveURL(/showAttachmentPanel=false/)
  }

  async function openDocumentationUnitEditModeTabThroughSidePanel(page: Page) {
    await expect(
      page,
      "Opened content side panel is required to proceed",
    ).toHaveURL(/showAttachmentPanel=true/)

    await page
      .getByRole("link", {
        name: "Dokumentationseinheit in einem neuen Tab bearbeiten",
      })
      .click()
  }

  async function searchForDocUnitWithFileNumber(
    page: Page,
    fileNumber: string,
    date: string,
  ) {
    await fillInput(page, "Gericht", "AG Aachen")
    await page.getByText("AG Aachen", { exact: true }).click()
    await fillInput(page, "Aktenzeichen", fileNumber)
    await fillInput(page, "Entscheidungsdatum", date)
    await fillInput(page, "Dokumenttyp", "AnU")
    await page.getByText("Anerkenntnisurteil", { exact: true }).click()

    await page.getByText("Suchen").click()
  }
})

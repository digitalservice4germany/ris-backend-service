import { expect, Locator } from "@playwright/test"
import { deleteDocumentUnit, navigateToCategories, save } from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

test.describe("core data", () => {
  test("core data change", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").fill("abc")
    await page.keyboard.press("Enter")
    await page.locator("[aria-label='ECLI']").fill("abc123")
    await page.keyboard.press("Enter")

    await save(page)

    await page.reload()
    await expect(page.locator("[aria-label='Aktenzeichen']")).toHaveValue("")
    await expect(page.locator("[aria-label='ECLI']")).toHaveValue("abc123")
  })

  test("nested 'ECLI' input toggles child input and correctly saves and displays data", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='ECLI']").type("one")
    await expect(page.getByText("one").first()).toBeVisible()

    await expect(page.getByText("Abweichender ECLI>")).toBeHidden()

    await page.locator("[aria-label='Abweichender ECLI anzeigen']").click()

    await expect(page.getByText("Abweichender ECLI").first()).toBeVisible()

    await page.locator("[aria-label='Abweichender ECLI']").type("two")
    await page.keyboard.press("Enter")
    await page.locator("[aria-label='Abweichender ECLI']").type("three")
    await page.keyboard.press("Enter")

    await save(page)

    await page.reload()

    // If deviating data is available, it is automatically expanded
    await expect(page.getByText("two").first()).toBeVisible()
    await expect(page.getByText("three").first()).toBeVisible()

    await page.locator("[aria-label='Abweichender ECLI schließen']").click()
    await expect(page.getByText("Abweichender ECLI").first()).toBeHidden()
  })

  test("nested fileNumbers input toggles child input and correctly saves and displays data", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").type("one")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").type("two")
    await page.keyboard.press("Enter")

    await expect(page.getByText("one").first()).toBeVisible()
    await expect(page.getByText("two").first()).toBeVisible()

    await expect(page.getByText("Abweichendes Aktenzeichen>")).toBeHidden()

    await page
      .locator("[aria-label='Abweichendes Aktenzeichen anzeigen']")
      .click()

    await expect(
      page.getByText("Abweichendes Aktenzeichen").first(),
    ).toBeVisible()

    await page.locator("[aria-label='Abweichendes Aktenzeichen']").type("three")
    await page.keyboard.press("Enter")

    await save(page)
    await page.reload()

    // If deviating data is available, it is automatically expanded
    await expect(page.getByText("three").first()).toBeVisible()
    await page
      .locator("[aria-label='Abweichendes Aktenzeichen schließen']")
      .click()
    await expect(
      page.getByText("Abweichendes Aktenzeichen").first(),
    ).toBeHidden()
  })

  test("adding, navigating, deleting multiple fileNumbers", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    await page.locator("[aria-label='Aktenzeichen']").type("testone")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").type("testtwo")
    await page.keyboard.press("Enter")

    await page.locator("[aria-label='Aktenzeichen']").type("testthree")
    await page.keyboard.press("Enter")

    await expect(page.getByText("testone").first()).toBeVisible()
    await expect(page.getByText("testtwo").first()).toBeVisible()
    await expect(page.getByText("testthree").first()).toBeVisible()

    // Navigate back and delete on enter
    await page.keyboard.press("ArrowLeft")
    await page.keyboard.press("ArrowLeft")
    await page.keyboard.press("Enter")

    await expect(page.getByText("testtwo").first()).toBeHidden()

    // Tab out and in
    await page.keyboard.press("Tab")
    await page.keyboard.press("Tab")

    await page.keyboard.down("Shift")
    await page.keyboard.press("Tab")
    await page.keyboard.up("Shift")

    await page.keyboard.down("Shift")
    await page.keyboard.press("Tab")
    await page.keyboard.up("Shift")

    await page.keyboard.press("ArrowLeft")

    //Navigate back and delete on backspace
    await page.keyboard.press("Enter")

    await expect(page.getByText("testone").first()).toBeHidden()
    await save(page)

    await page.reload()
    await expect(page.getByText("testthree").first()).toBeVisible()
  })

  test("legal effect dropdown", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const dropdown = page.getByRole("combobox", { name: "Rechtskraft" })
    await expect(dropdown).toHaveValue("Keine Angabe")

    await expect(dropdown.getByRole("option", { name: "Ja" })).toHaveCount(1)
    await expect(dropdown.getByRole("option", { name: "Nein" })).toHaveCount(1)
    await expect(
      dropdown.getByRole("option", { name: "Keine Angabe" }),
    ).toHaveCount(1)
  })

  test("document type dropdown", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    // on start: closed dropdown, no input text
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue("")
    await expect(page.getByText("AnU - Anerkenntnisurteil")).toBeHidden()
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()

    // open dropdown
    await page
      .locator("#coreData div")
      .filter({ hasText: "Spruchkörper Dokumenttyp" })
      .getByLabel("Dropdown öffnen")
      .click()
    await expect(
      page.locator("[aria-label='dropdown-option']"),
    ).not.toHaveCount(0)
    await expect(page.getByText("Anerkenntnisurteil")).toBeVisible()
    await expect(page.getByText("Anhängiges Verfahren")).toBeVisible()

    // type search string: 3 results for "zwischen"
    await page.locator("[aria-label='Dokumenttyp']").fill("zwischen")
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue(
      "zwischen",
    )
    await expect(page.locator("[aria-label='dropdown-option']")).toHaveCount(3)

    // use the clear icon
    await page.locator("[aria-label='Auswahl zurücksetzen']").click()
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue("")
    await expect(
      page.locator("[aria-label='dropdown-option']"),
    ).not.toHaveCount(0)
    await expect(page.getByText("Anerkenntnisurteil")).toBeVisible()

    // close dropdown
    await page.getByLabel("Dropdown schließen").click()
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue("")
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()

    // open dropdown again by focussing
    await page.locator("[aria-label='Dokumenttyp']").focus()

    // close dropdown using the esc key, user input text gets removed and last saved value restored
    await page.keyboard.down("Escape")
    await expect(page.locator("[aria-label='dropdown-option']")).toBeHidden()
    await expect(page.locator("[aria-label='Dokumenttyp']")).toHaveValue("")
  })

  // skipped as we don't show the Dokstelle anymore as of RISDEV-4177
  // eslint-disable-next-line playwright/no-skipped-test
  test.skip("ensure new docUnit has correct documentationOffice for DS user", async ({
    page,
  }) => {
    await test.step("create new docUnit with logged in user", async () => {
      await page.goto("/caselaw")
      const tableView = page.getByRole("cell", {
        name: "Dateiname",
        exact: true,
      })

      await page
        .getByRole("button", {
          name: "Neue Dokumentationseinheit",
          exact: true,
        })
        .click()

      await expect(tableView).toBeHidden()

      await page.getByText("Rubriken").click()
      await expect(page.getByText("DOKUMENTATIONSSTELLEDS")).toBeVisible()

      const documentNumber = page
        .url()
        .match(/documentunit\/([A-Z0-9]{13})\/files/)![1]

      await deleteDocumentUnit(page, documentNumber)
    })
  })

  // skipped as we don't show the Dokstelle anymore as of RISDEV-4177
  // eslint-disable-next-line playwright/no-skipped-test
  test.skip("ensure new docUnit has correct documentationOffice for BGH user", async ({
    pageWithBghUser,
  }) => {
    await test.step("create new docUnit with logged in user", async () => {
      await pageWithBghUser.goto("/caselaw")
      await pageWithBghUser
        .getByRole("button", {
          name: "Neue Dokumentationseinheit",
          exact: true,
        })
        .click()

      const tableView = pageWithBghUser.getByRole("cell", {
        name: "Dateiname",
        exact: true,
      })

      await expect(tableView).toBeHidden()

      await pageWithBghUser.getByText("Rubriken").click()
      await expect(
        pageWithBghUser.getByText("DOKUMENTATIONSSTELLEBGH"),
      ).toBeVisible()

      const documentNumber = pageWithBghUser
        .url()
        .match(/documentunit\/([A-Z0-9]{13})\/files/)![1]

      await deleteDocumentUnit(pageWithBghUser, documentNumber)
    })
  })

  test("ensure that leadingDecisionNormReferences are visible, savable, and deletable for the BGH and not for others", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    const nswInput = page.locator("[aria-label='BGH Nachschlagewerk']")
    const CITATION = "1968, 249-252 (ST)"
    const nswChipTag = page.getByText(CITATION)
    await expect(nswInput).toBeHidden()

    await page.locator("[aria-label='Gericht']").fill("BGH")
    await page.getByText("BGH").click()
    await expect(nswInput).toBeVisible()
    await nswInput.fill(CITATION)
    await page.keyboard.press("Enter")

    await save(page)
    await expect(
      nswChipTag,
      "NSW Fundstelle is not visible for BGH",
    ).toBeVisible()

    await page
      .getByTestId("chips-input_leadingDecisionNormReferences")
      .getByLabel("Löschen")
      .click()
    await save(page)

    await expect(nswChipTag, "Citation was not deleted").toBeHidden()

    await page.locator("[aria-label='Gericht']").fill("AG Aalen")
    await page.getByText("AG Aalen").click()
    await expect(
      nswInput,
      "NSW Fundstelle is visible for other courts",
    ).toBeHidden()
  })

  test(
    "core data is hidden for external user",
    {
      annotation: {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4523",
      },
      tag: ["@RISDEV-4523"],
    },
    async ({ pageWithExternalUser, documentNumber }) => {
      await test.step("Navigiere zu Rubriken als external Nutzer", async () => {
        await navigateToCategories(pageWithExternalUser, documentNumber)
      })

      await test.step("Gericht und Fehlerhaftes Gericht sind nicht sichtbar", async () => {
        const court = pageWithExternalUser.locator("[aria-label='Gericht']")
        const deviatingCourt = pageWithExternalUser.getByTestId(
          "chips-input_deviatingCourt",
        )
        await expect(court).toBeHidden()
        await expect(deviatingCourt).toBeHidden()
      })

      await test.step("Aktenzeichen und abweichendes Aktenzeichen sind nicht sichtbar", async () => {
        const fileNumber = pageWithExternalUser.getByTestId(
          "chips-input_fileNumber",
        )
        const deviatingFileNumber = pageWithExternalUser.getByTestId(
          "chips-input_deviatingFileNumber",
        )
        await expect(fileNumber).toBeHidden()
        await expect(deviatingFileNumber).toBeHidden()
      })

      await test.step("Entscheidungsdatum und abweichendes Entscheidungsdatum sind nicht sichtbar", async () => {
        const decisionDate = pageWithExternalUser.locator(
          "[aria-label='Entscheidungsdatum']",
        )
        const deviatingDecisionDate = pageWithExternalUser.getByTestId(
          "chips-input_deviatingDecisionDates",
        )
        await expect(decisionDate).toBeHidden()
        await expect(deviatingDecisionDate).toBeHidden()
      })

      await test.step("Spruchkörper ist nicht sichtbar", async () => {
        const appraisalBody = pageWithExternalUser.locator(
          "[aria-label='Spruchkörper']",
        )
        await expect(appraisalBody).toBeHidden()
      })

      await test.step("Dokumenttyp ist nicht sichtbar", async () => {
        const documentType = pageWithExternalUser.locator(
          "[aria-label='Dokumenttyp']",
        )
        await expect(documentType).toBeHidden()
      })

      await test.step("ECLI und abweichender ECLI sind nicht sichtbar", async () => {
        const ecli = pageWithExternalUser.locator("[aria-label='ECLI']")
        const deviatingEcli = pageWithExternalUser.getByTestId(
          "chips-input_deviatingEclis",
        )
        await expect(ecli).toBeHidden()
        await expect(deviatingEcli).toBeHidden()
      })

      await test.step("Vorgang ist nicht sichtbar", async () => {
        const procedure = pageWithExternalUser.locator("[aria-label='Vorgang']")
        await expect(procedure).toBeHidden()
      })

      await test.step("Rechtskraft ist nicht sichtbar", async () => {
        const legalEffect = pageWithExternalUser.locator(
          "[aria-label='Rechtskraft']",
        )
        await expect(legalEffect).toBeHidden()
      })

      await test.step("Region ist nicht sichtbar", async () => {
        const region = pageWithExternalUser.locator("[aria-label='Region']")
        await expect(region).toBeHidden()
      })

      await test.step("Streitjahr ist nicht sichtbar", async () => {
        const yearsOfDispute = pageWithExternalUser.getByTestId(
          "chips-input_yearOfDispute",
        )
        await expect(yearsOfDispute).toBeHidden()
      })
    },
  )

  test(
    "core data is editable for internal user",
    {
      annotation: {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4523",
      },
      tag: ["@RISDEV-4523"],
    },
    async ({ page, documentNumber }) => {
      await test.step("Navigiere zu Rubriken als interner Nutzer", async () => {
        await navigateToCategories(page, documentNumber)
      })

      await test.step("Gericht und Fehlerhaftes Gericht sind bearbeitbar", async () => {
        const court = page.locator("[aria-label='Gericht']")
        const deviatingCourt = page.getByTestId("chips-input_deviatingCourt")
        await expect(court).toBeEditable()
        expect(await isReadOnly(deviatingCourt)).toBe(false)
      })

      await test.step("Aktenzeichen und abweichendes Aktenzeichen sind bearbeitbar", async () => {
        const fileNumber = page.getByTestId("chips-input_fileNumber")
        const deviatingFileNumber = page.getByTestId(
          "chips-input_deviatingFileNumber",
        )
        await expect(fileNumber).toBeEditable()
        expect(await isReadOnly(deviatingFileNumber)).toBe(false)
      })

      await test.step("Entscheidungsdatum und abweichendes Entscheidungsdatum sind bearbeitbar", async () => {
        const decisionDate = page.locator("[aria-label='Entscheidungsdatum']")
        const deviatingDecisionDate = page.getByTestId(
          "chips-input_deviatingDecisionDates",
        )
        await expect(decisionDate).toBeEditable()
        expect(await isReadOnly(deviatingDecisionDate)).toBe(false)
      })

      await test.step("Spruchkörper ist bearbeitbar", async () => {
        const appraisalBody = page.locator("[aria-label='Spruchkörper']")
        await expect(appraisalBody).toBeEditable()
      })

      await test.step("Dokumenttyp ist bearbeitbar", async () => {
        const documentType = page.locator("[aria-label='Dokumenttyp']")
        await expect(documentType).toBeEditable()
      })

      await test.step("ECLI und abweichender ECLI sind bearbeitbar", async () => {
        const ecli = page.locator("[aria-label='ECLI']")
        const deviatingEcli = page.getByTestId("chips-input_deviatingEclis")
        await expect(ecli).toBeEditable()
        expect(await isReadOnly(deviatingEcli)).toBe(false)
      })

      await test.step("Vorgang ist bearbeitbar", async () => {
        const procedure = page.locator("[aria-label='Vorgang']")
        await expect(procedure).toBeEditable()
      })

      await test.step("Rechtskraft ist bearbeitbar", async () => {
        const legalEffect = page.locator("[aria-label='Rechtskraft']")
        await expect(legalEffect).toBeEnabled()
      })

      await test.step("Region ist readonly", async () => {
        const region = page.locator("[aria-label='Region']")
        await expect(region).not.toBeEditable()
      })

      await test.step("Streitjahr ist bearbeitbar", async () => {
        const yearsOfDispute = page.locator("[aria-label='Streitjahr']")
        expect(await isReadOnly(yearsOfDispute)).toBe(false)
      })
    },
  )

  const isReadOnly = async (locator: Locator) => {
    const classAttribute = await locator.getAttribute("class")
    return classAttribute?.split(" ").includes("!bg-blue-300")
  }
})

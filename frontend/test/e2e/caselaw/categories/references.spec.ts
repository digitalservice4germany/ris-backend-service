import { expect } from "@playwright/test"
import {
  navigateToPreview,
  navigateToHandover,
  save,
  navigateToReferences,
  fillInput,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "references",
  {
    tag: "@RISDEV-4264",
    annotation: {
      type: "epic",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4264",
    },
  },
  () => {
    test(
      "Display, adding, editing and deleting multiple references",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4336",
        },
      },
      async ({ page, documentNumber }) => {
        await test.step("References is a new selectable menu item in a documentation unit", async () => {
          await navigateToReferences(page, documentNumber)
        })

        await test.step("When typing the legal periodical abbreviation (MM), the entry including abbreviation, title and subtitle can be found in the combobox", async () => {
          await fillInput(page, "Periodikum", "MM")
          await expect(
            page.getByText("Magazin des Berliner Mieterverein e.V.", {
              exact: true,
            }),
          ).toBeVisible()
          await page.getByText("MM | Mieter Magazin", { exact: true }).click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "MM")
        })

        await test.step("citation shows citation example", async () => {
          await expect(
            page.getByText("Zitierbeispiel: 2011, Nr 6, 22-23"),
          ).toBeVisible()
        })

        await test.step("citation and supplement can be added", async () => {
          await fillInput(page, "Zitatstelle", "2024, Nr 1, 2-5")
          await fillInput(page, "Klammernzusatz", "LT")
        })

        await test.step("Reference can be added to editable list", async () => {
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("MM 2024, Nr 1, 2-5 (LT)")).toBeVisible()
          await expect(
            page.getByText("sekundär", { exact: true }),
          ).toBeVisible()
        })

        await save(page)

        await test.step("Reference is persisted and shown after reload", async () => {
          await page.reload()
          await expect(page.getByText("MM 2024, Nr 1, 2-5 (LT)")).toBeVisible()
          await expect(
            page.getByText("sekundär", { exact: true }),
          ).toBeVisible()
        })

        await test.step("Edit legal periodical in reference, verify that it is updated in the list", async () => {
          await page.getByTestId("list-entry-0").click()
          await fillInput(page, "Periodikum", "GVBl BB")
          await page
            .getByText(
              "GVBl BB | Gesetz- und Verordnungsblatt für das Land Brandenburg",
              { exact: true },
            )
            .click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "GVBl BB")
          await expect(
            page.getByText("Zitierbeispiel: 1991, 676-681"),
          ).toBeVisible()
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (LT)"),
          ).toBeVisible()

          await expect(page.getByText("primär", { exact: true })).toBeVisible()
        })

        await test.step("Edit reference supplement in reference, verify that it is updated in the list", async () => {
          await page.getByTestId("list-entry-0").click()
          await fillInput(page, "Klammernzusatz", "S")

          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (LT)"),
          ).toBeHidden()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (S)"),
          ).toBeVisible()

          await expect(page.getByText("primär", { exact: true })).toBeVisible()
        })

        await test.step("Edit reference, click cancel, verify that it is not updated in the list", async () => {
          await page.getByTestId("list-entry-0").click()
          await fillInput(page, "Klammernzusatz", "LT")

          await page.getByLabel("Abbrechen").click()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (LT)"),
          ).toBeHidden()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (S)"),
          ).toBeVisible()

          await expect(page.getByText("primär", { exact: true })).toBeVisible()
        })

        await test.step("Add second reference, verify that it is shown in the list", async () => {
          await page.getByLabel("Weitere Angabe").click()
          await fillInput(page, "Periodikum", "wdg")
          await page
            .getByText("WdG | Welt der Gesundheitsversorgung", {
              exact: true,
            })
            .click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "WdG")
          await fillInput(page, "Zitatstelle", "2024, 10-12")
          await fillInput(page, "Klammernzusatz", "ST")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("WdG 2024, 10-12 (ST)")).toBeVisible()
        })
        await save(page)

        await test.step("Add third reference, verify that it is shown in the list", async () => {
          await fillInput(page, "Periodikum", "AllMBl")
          await page
            .getByText("AllMBl | Allgemeines Ministerialblatt", {
              exact: true,
            })
            .click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "AllMBl")
          await fillInput(page, "Zitatstelle", "2024, 2")
          await fillInput(page, "Klammernzusatz", "L")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("AllMBl 2024, 2 (L)")).toBeVisible()
        })
        await save(page)
        await test.step("Delete second of 3 reference and verify it disappears, order of remaining items stays the same", async () => {
          await page.getByTestId("list-entry-1").click()
          await page.locator("[aria-label='Eintrag löschen']").click()
          await expect(page.getByText("WdG 2024, 10-12 (ST)")).toBeHidden()

          await expect(page.getByLabel("Listen Eintrag").nth(0)).toHaveText(
            "GVBl BB 2024, Nr 1, 2-5 (S)primär",
          )
          await expect(page.getByLabel("Listen Eintrag").nth(1)).toHaveText(
            "AllMBl 2024, 2 (L)primär",
          )
        })

        await test.step("Delete second of 2 references and verify it disappears from the list, first item stays the same", async () => {
          await page.getByTestId("list-entry-1").click()
          await page.locator("[aria-label='Eintrag löschen']").click()
          await expect(page.getByText("AllMBl 2024, 2 (L)")).toBeHidden()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (S)"),
          ).toBeVisible()
        })
        await test.step("Delete last reference and verify the list is empty", async () => {
          await page.getByTestId("list-entry-0").click()
          await page.locator("[aria-label='Eintrag löschen']").click()
          await expect(
            page.getByText("GVBl BB 2024, Nr 1, 2-5 (S)"),
          ).toBeHidden()
          await save(page)

          await page.reload()

          await expect(page.getByLabel("Listen Eintrag")).not.toBeAttached()
        })
      },
    )

    test(
      "References input validated against required fields",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4336",
        },
      },
      async ({ page, prefilledDocumentUnit }) => {
        await navigateToReferences(
          page,
          prefilledDocumentUnit.documentNumber ?? "",
        )

        await test.step("Add reference with only 'Klammerzusatz' shows error in 'Periodikum' & 'Zitatstelle'", async () => {
          await fillInput(page, "Klammernzusatz", "LT")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("Pflichtfeld nicht befüllt")).toHaveCount(
            2,
          )
        })
        await test.step("Add 'Periodikum' removes error there, but still shows error for 'Zitatstelle'", async () => {
          await fillInput(page, "Periodikum", "wdg")
          await page
            .getByText("WdG | Welt der Gesundheitsversorgung", {
              exact: true,
            })
            .click()
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("Pflichtfeld nicht befüllt")).toHaveCount(
            1,
          )
        })
        await test.step("Add 'Zitatstelle' removes error, reference can be saved", async () => {
          await fillInput(page, "Zitatstelle", "2024, 10-12")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("Pflichtfeld nicht befüllt")).toBeHidden()
        })
        await test.step("Add 'Periodikum' with empty Klammernzusatz (referenceSupplement)", async () => {
          await fillInput(page, "Periodikum", "wdg")
          await fillInput(page, "Zitatstelle", "2024, 10-12")
          await fillInput(page, "Klammernzusatz", "")

          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(
            page.getByText("Pflichtfeld nicht befüllt"),
            "Should not count empty reference supplement error",
          ).toHaveCount(1)
        })
      },
    )

    test(
      "Literature references can be added to documentation unit",
      {
        tag: "@RISDEV-5236 @RISDEV-5454",
      },
      async ({ page, documentNumber }) => {
        await test.step("Caselaw reference type is preselected", async () => {
          await navigateToReferences(page, documentNumber)

          await expect(
            page.getByLabel("Rechtsprechung Fundstelle"),
          ).toBeChecked()

          await expect(
            page.getByLabel("Literatur Fundstelle"),
          ).not.toBeChecked()

          await expect(page.getByLabel("Klammernzusatz")).toBeVisible()

          await expect(
            page.getByLabel("Dokumenttyp Literaturfundstelle"),
          ).toBeHidden()

          await expect(
            page.getByLabel("Autor Literaturfundstelle"),
          ).toBeHidden()
        })

        await test.step("Selecting literature reference type, renders different inputs", async () => {
          await page.getByLabel("Literatur Fundstelle").click()
          await expect(
            page.getByLabel("Rechtsprechung Fundstelle"),
          ).not.toBeChecked()

          await expect(page.getByLabel("Literatur Fundstelle")).toBeChecked()

          await expect(
            page.getByLabel("Dokumenttyp Literaturfundstelle"),
          ).toBeVisible()

          await expect(
            page.getByLabel("Autor Literaturfundstelle"),
          ).toBeVisible()
          await expect(page.getByLabel("Klammernzusatz")).toBeHidden()
        })

        await test.step("Literature references are validated for required inputs", async () => {
          await fillInput(page, "Periodikum", "AllMBl")
          await page
            .getByText("AllMBl | Allgemeines Ministerialblatt", {
              exact: true,
            })
            .click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "AllMBl")
          await fillInput(page, "Zitatstelle", "2024, 2")

          await page.locator("[aria-label='Fundstelle speichern']").click()
          // check that both fields display error message
          await expect(
            page.locator("text=Pflichtfeld nicht befüllt"),
          ).toHaveCount(2)

          // Switching between radio buttons resets the validation errors
          await page.getByLabel("Rechtsprechung Fundstelle").click()
          await page.getByLabel("Literatur Fundstelle").click()
          await expect(
            page.locator("text=Pflichtfeld nicht befüllt"),
          ).toHaveCount(0)
        })

        await test.step("Save literature reference, verify that it is shown in the list", async () => {
          await fillInput(page, "Autor Literaturfundstelle", "Bilen, Ulviye")
          await fillInput(page, "Dokumenttyp Literaturfundstelle", "Ean")
          await page.getByText("Ean", { exact: true }).click()
          await waitForInputValue(
            page,
            "[aria-label='Dokumenttyp Literaturfundstelle']",
            "Anmerkung",
          )
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(
            page.getByText("Bilen, Ulviye, AllMBl 2024, 2 (Ean)"),
          ).toBeVisible()
        })

        await test.step("Radio buttons should not be visible after saving", async () => {
          await page.getByTestId("list-entry-0").click()
          await expect(
            page.getByLabel("Rechtsprechung Fundstelle"),
          ).toBeHidden()

          await expect(page.getByLabel("Literatur Fundstelle")).toBeHidden()
        })
      },
    )

    test(
      "References are visible in preview",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4335",
        },
      },
      async ({ page, documentNumber }) => {
        await test.step("References are not rendered in preview when empty", async () => {
          await navigateToPreview(page, documentNumber)
          await expect(page.getByText("Fundstellen")).toBeHidden()
        })

        await navigateToReferences(page, documentNumber)

        await test.step("Add primary and secondary references with all data, verify remdering in preview", async () => {
          await fillInput(page, "Periodikum", "wdg")
          await page
            .getByText("WdG | Welt der Gesundheitsversorgung", {
              exact: true,
            })
            .click()
          await fillInput(page, "Zitatstelle", "2024, 10-12")
          await fillInput(page, "Klammernzusatz", "LT")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("WdG 2024, 10-12 (LT)")).toBeVisible()
          await expect(
            page.getByText("sekundär", {
              exact: true,
            }),
          ).toBeVisible()
          await fillInput(page, "Periodikum", "GVBl BB")
          await page
            .getByText(
              "GVBl BB | Gesetz- und Verordnungsblatt für das Land Brandenburg",
              { exact: true },
            )
            .click()
          await fillInput(page, "Zitatstelle", "2020, 01-99")
          await fillInput(page, "Klammernzusatz", "L")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("GVBl BB 2020, 01-99 (L)")).toBeVisible()
          await expect(
            page.getByText("primär", {
              exact: true,
            }),
          ).toBeVisible()
        })
        await save(page)

        await navigateToPreview(page, documentNumber)

        await expect(
          page.getByText("Fundstellen", { exact: true }),
        ).toBeVisible()
        await expect(page.getByText("Literaturfundstellen")).toBeHidden()
        await expect(
          page.getByText("Primäre FundstellenGVBl BB 2020, 01-99 (L)"),
        ).toBeVisible()
        await expect(
          page.getByText("Sekundäre FundstellenWdG 2024, 10-12 (LT)"),
        ).toBeVisible()
      },
    )

    test(
      "Literature references are visible in preview",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4335",
        },
      },
      async ({ page, documentNumber }) => {
        await test.step("References are not rendered in preview when empty", async () => {
          await navigateToPreview(page, documentNumber)
          await expect(page.getByText("Literaturfundstellen")).toBeHidden()
        })

        await test.step("Add literature reference, verify remdering in preview", async () => {
          await navigateToReferences(page, documentNumber)
          await page.getByLabel("Literatur Fundstelle").click()
          await fillInput(page, "Periodikum", "AllMBl")
          await page
            .getByText("AllMBl | Allgemeines Ministerialblatt", {
              exact: true,
            })
            .click()
          await waitForInputValue(page, "[aria-label='Periodikum']", "AllMBl")
          await fillInput(page, "Zitatstelle", "2024, 2")
          await fillInput(page, "Autor Literaturfundstelle", "Bilen, Ulviye")
          await fillInput(page, "Dokumenttyp Literaturfundstelle", "Ean")
          await page.getByText("Ean", { exact: true }).click()
          await waitForInputValue(
            page,
            "[aria-label='Dokumenttyp Literaturfundstelle']",
            "Anmerkung",
          )

          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(
            page.getByText("Bilen, Ulviye, AllMBl 2024, 2 (Ean)"),
          ).toBeVisible()
        })

        await save(page)

        await navigateToPreview(page, documentNumber)

        await expect(page.getByText("Literaturfundstellen")).toBeVisible()
        await expect(
          page.getByText("Fundstellen", { exact: true }),
        ).toBeHidden()

        await expect(
          page.getByText("Bilen, Ulviye, AllMBl 2024, 2 (Ean)"),
        ).toBeVisible()
      },
    )

    test(
      "References can be exported",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4337",
        },
      },
      async ({ page, prefilledDocumentUnit }) => {
        await navigateToReferences(
          page,
          prefilledDocumentUnit.documentNumber ?? "",
        )

        await test.step("Add primary and secondary references with all data", async () => {
          await fillInput(page, "Periodikum", "wdg")
          await page
            .getByText("WdG | Welt der Gesundheitsversorgung", {
              exact: true,
            })
            .click()
          await fillInput(page, "Zitatstelle", "2024, 10-12")
          await fillInput(page, "Klammernzusatz", "LT")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("WdG 2024, 10-12 (LT)")).toBeVisible()
          await expect(
            page.getByText("sekundär", { exact: true }),
          ).toBeVisible()
          await fillInput(page, "Periodikum", "GVBl BB")
          await page
            .getByText(
              "GVBl BB | Gesetz- und Verordnungsblatt für das Land Brandenburg",
              { exact: true },
            )
            .click()
          await fillInput(page, "Zitatstelle", "2020, 01-99")
          await fillInput(page, "Klammernzusatz", "L")
          await page.locator("[aria-label='Fundstelle speichern']").click()
          await expect(page.getByText("GVBl BB 2020, 01-99 (L)")).toBeVisible()
          await expect(page.getByText("primär", { exact: true })).toBeVisible()
        })
        await save(page)

        await test.step("Navigate to handover, click in 'XML-Vorschau', check references are visible in correct order", async () => {
          await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
          await expect(page.getByText("XML Vorschau")).toBeVisible()
          await page.getByText("XML Vorschau").click()

          const referencesNodes = await page
            .locator('code:has-text("<fundstelle ")')
            .all()
          await expect(referencesNodes[0]).toHaveText(
            '<fundstelle typ="nichtamtlich">',
          )
          await expect(referencesNodes[1]).toHaveText(
            '<fundstelle typ="amtlich">',
          )

          const legalPeriodicalNodes = await page
            .locator('code:has-text("<periodikum>")')
            .all()
          await expect(legalPeriodicalNodes[0]).toHaveText(
            "<periodikum>WdG</periodikum>",
          )
          await expect(legalPeriodicalNodes[1]).toHaveText(
            "<periodikum>GVBl BB</periodikum>",
          )

          const citationNodes = await page
            .locator('code:has-text("<zitstelle>")')
            .all()
          await expect(citationNodes[0]).toHaveText(
            "<zitstelle>2024, 10-12 (LT)</zitstelle>",
          )
          await expect(citationNodes[1]).toHaveText(
            "<zitstelle>2020, 01-99 (L)</zitstelle>",
          )
        })
      },
    )
  },
)

import { expect } from "@playwright/test"
import {
  navigateToCategories,
  navigateToHandover,
  navigateToPreview,
  save,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "Participating Judges (Mitwirkende Richter)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4574",
    },
    tag: ["@RISDEV-4574"],
  },
  () => {
    test("participating judge input should be saved and displayed in preview and in 'XML-Vorschau'", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("check button is displayed when field is empty", async () => {
        await expect(
          page.getByRole("button", { name: "Mitwirkende Richter" }),
        ).toBeVisible()
      })
      await page.getByRole("button", { name: "Mitwirkende Richter" }).click()

      await test.step("Übernehmen is disabled if name is empty", async () => {
        await expect(
          page.getByLabel("Mitwirkenden Richter speichern"),
        ).toBeDisabled()
      })

      await test.step("enter participating judge name", async () => {
        await page
          .getByTestId("participating-judge-name-input")
          .fill("Name of judge")
      })

      await test.step("enter participating judge referenced opinions (Art der Mitwirkung)", async () => {
        await page
          .getByTestId("participating-judge-reference-opinions-input")
          .fill("Abweichende Meinung")
      })

      await test.step("click 'Übernehmen' to save the entry", async () => {
        await page.getByLabel("Mitwirkenden Richter speichern").click()
      })

      await test.step("Summary of list entry is visible", async () => {
        await expect(
          page.getByText("Name of judge (Abweichende Meinung)", {
            exact: true,
          }),
        ).toBeVisible()
      })

      await save(page)

      await test.step("Preview should show participating judges", async () => {
        await navigateToPreview(page, prefilledDocumentUnit.documentNumber!)

        await expect(
          page.getByText("Mitwirkende Richter", { exact: true }),
        ).toBeVisible()
        await expect(
          page.getByText("Name of judge (Abweichende Meinung)", {
            exact: true,
          }),
        ).toBeVisible()
      })

      await test.step("XML preview should display 'mitwirkung' fields in 'paratrubriken'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await page.getByText("XML Vorschau").click()
        const xmlPreview = page.getByTitle("XML Vorschau")
        const innerText = await xmlPreview.innerText()
        expect(innerText).toBeDefined()
        expect(innerText).toContain(
          "13\n" +
            "    <paratrubriken>\n" +
            "14\n" +
            "        <mitwirkung>Name of judge (Abweichende Meinung)</mitwirkung>\n" +
            "15\n" +
            "        <zuordnung>",
        )
      })

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("Change the name of the judge", async () => {
        await page.getByTestId("list-entry-0").click()
        await page
          .getByTestId("participating-judge-name-input")
          .fill("Updated name")
      })

      await test.step("click 'Übernehmen' to save the entry", async () => {
        await page.getByLabel("Mitwirkenden Richter speichern").click()
      })

      await test.step("Summary of list entry is visible", async () => {
        await expect(
          page.getByText("Updated name (Abweichende Meinung)", { exact: true }),
        ).toBeVisible()
      })

      await test.step("Delete entry", async () => {
        await page.getByTestId("list-entry-0").click()
        await page.getByRole("button", { name: "Eintrag löschen" }).click()
      })

      await test.step("Summary of list entry should be gone", async () => {
        await expect(
          page.getByText("Updated name (Abweichende Meinung)", { exact: true }),
        ).toBeHidden()
      })
    })
  },
)

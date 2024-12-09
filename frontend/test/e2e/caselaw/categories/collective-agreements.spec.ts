import { expect } from "@playwright/test"
import {
  navigateToCategories,
  navigateToHandover,
  navigateToPreview,
  save,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "Collective Agreements (Tarifvertrag)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4578",
    },
    tag: ["@RISDEV-4578"],
  },
  () => {
    test("saving and exporting collective agreement", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("check button is not displayed without labor court", async () => {
        await expect(
          page.getByRole("button", { name: "Tarifvertrag" }),
        ).toBeHidden()
      })

      await test.step("Select BAG (labor court)", async () => {
        await page.locator("[aria-label='Gericht']").fill("BAG")
        await waitForInputValue(page, "[aria-label='Gericht']", "BAG")
        await expect(
          page.locator("button").filter({ hasText: /^BAG$/ }),
        ).toBeVisible()
        await page.locator("button").filter({ hasText: /^BAG$/ }).click()
        await waitForInputValue(page, "[aria-label='Gericht']", "BAG")

        await save(page)
      })

      await test.step("check button is displayed when field is empty", async () => {
        await expect(
          page.getByRole("button", { name: "Tarifvertrag" }),
        ).toBeVisible()
      })
      await page.getByRole("button", { name: "Tarifvertrag" }).click()

      await test.step("enter collective agreement", async () => {
        await page
          .getByTestId("Tarifvertrag_ListInputEdit")
          .fill("Stehende Bühnen")

        await page.getByLabel("Tarifvertrag übernehmen").click()

        await expect(
          page.getByTestId("ListInputDisplay_Tarifvertrag_Stehende Bühnen"),
        ).toHaveText("Stehende Bühnen")

        await save(page)
      })

      await test.step("XML preview should display 'Tarifvertrag' fields in 'paratrubriken'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await page.getByText("XML Vorschau").click()
        const xmlPreview = page.getByTitle("XML Vorschau")
        const innerText = await xmlPreview.innerText()
        expect(innerText).toBeDefined()
        expect(innerText).toContain(
          "<paratrubriken>\n" +
            "13\n" +
            "        <zuordnung>\n" +
            "14\n" +
            "            <aspekt>Tarifvertrag</aspekt>\n" +
            "15\n" +
            "            <begriff>Stehende Bühnen</begriff>\n" +
            "16\n" +
            "        </zuordnung>\n",
        )
      })

      await test.step("Preview should show collective agreement", async () => {
        await navigateToPreview(page, prefilledDocumentUnit.documentNumber!)

        await expect(page.getByTestId("Tarifvertrag")).toHaveText(
          "Stehende Bühnen",
        )
      })
    })
  },
)

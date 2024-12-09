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
  "Dimissal types and grounds (Kündigungsarten / Kündigungsgründe)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4577",
    },
    tag: ["@RISDEV-4577"],
  },
  () => {
    test("saving and exporting dismissal attributes", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("check button is not displayed without labor court", async () => {
        await expect(
          page.getByRole("button", { name: "Kündigung" }),
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
          page.getByRole("button", { name: "Kündigung" }),
        ).toBeVisible()
      })
      await page.getByRole("button", { name: "Kündigung" }).click()

      await test.step("enter dismissal type", async () => {
        await page
          .getByLabel("Kündigungsarten")
          .fill("Betriebsbedingte Kündigung")
        await page.keyboard.press("Enter")
        await expect(
          page
            .getByTestId("chips-input_dismissalTypes")
            .getByTestId("chip-value"),
        ).toHaveText("Betriebsbedingte Kündigung")
      })

      await test.step("enter dismissal ground", async () => {
        await page
          .getByLabel("Kündigungsgründe")
          .fill("Einführung neuer Technologien")
        await page.keyboard.press("Enter")
        await expect(
          page
            .getByTestId("chips-input_dismissalGrounds")
            .getByTestId("chip-value"),
        ).toHaveText("Einführung neuer Technologien")
      })
      await save(page)

      await test.step("XML preview should display 'Kündigungs' fields in 'paratrubriken'", async () => {
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
            "            <aspekt>Kündigungsgrund</aspekt>\n" +
            "15\n" +
            "            <begriff>Betriebsbedingte Kündigung</begriff>\n" +
            "16\n" +
            "        </zuordnung>\n" +
            "17\n" +
            "        <zuordnung>\n" +
            "18\n" +
            "            <aspekt>Kündigungsgrund</aspekt>\n" +
            "19\n" +
            "            <begriff>Einführung neuer Technologien</begriff>\n" +
            "20\n" +
            "        </zuordnung>",
        )
      })

      await test.step("Preview should show dismissal fields", async () => {
        await navigateToPreview(page, prefilledDocumentUnit.documentNumber!)

        await expect(page.getByTestId("Kündigungsarten")).toHaveText(
          "Betriebsbedingte Kündigung",
        )
        await expect(page.getByTestId("Kündigungsgründe")).toHaveText(
          "Einführung neuer Technologien",
        )
      })
    })
  },
)

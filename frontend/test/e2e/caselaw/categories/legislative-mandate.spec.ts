import { expect, Page } from "@playwright/test"
import {
  navigateToCategories,
  navigateToHandover,
  navigateToPreview,
  save,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "legislative mandate (Gesetzgebungsauftrag)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4575",
    },
    tag: ["@RISDEV-4575"],
  },
  () => {
    test("legislative mandate (display logic and XML-Vorschau)", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("check that with courType 'AG Aachen' checkbox is hidden and button is hidden", async () => {
        await expect(page.getByText("AG Aachen")).toBeVisible()
        await expect(
          page.getByRole("button", { name: "Gesetzgebungsauftrag" }),
        ).toBeHidden()
        await expect(
          page.getByText("Gesetzgebungsauftrag vorhanden"),
        ).toBeHidden()
      })

      await addCourtType(page, "BVerfG")

      await test.step("check button is displayed instead of checkbox when checkbox is unchecked", async () => {
        await expect(
          page.getByRole("button", { name: "Gesetzgebungsauftrag" }),
        ).toBeVisible()
        await expect(
          page.getByText("Gesetzgebungsauftrag vorhanden"),
        ).toBeHidden()
      })

      await test.step("click 'Gesetzgebungsauftrag'-button", async () => {
        await page.getByRole("button", { name: "Gesetzgebungsauftrag" }).click()
      })

      await checkLegislativeMandate(page)

      await reloadWithCheck(page)

      await test.step("remove court type", async () => {
        await page.locator("[aria-label='Gericht']").fill("")
        await save(page)
      })

      await reloadWithCheck(page)

      await addCourtType(page, "BVerfG")

      await test.step("check legislative mandate is visible in preview", async () => {
        await navigateToPreview(
          page,
          prefilledDocumentUnit.documentNumber as string,
        )
        await expect(page.getByText("Gesetzgebungsauftrag")).toBeVisible()
      })

      await test.step("XML preview should display 'Gesetzgebungsauftrag' in 'paratrubriken'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await page.getByText("XML Vorschau").click()
        const xmlPreview = page.getByTitle("XML Vorschau")
        const innerText = await xmlPreview.innerText()
        expect(innerText).toContain(
          "13\n" +
            "        <zuordnung>\n" +
            "14\n" +
            "            <aspekt>Gesetzgebungsauftrag</aspekt>\n" +
            "15\n" +
            "            <begriff>ja</begriff>\n" +
            "16\n" +
            "        </zuordnung>\n" +
            "17",
        )
      })

      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("uncheck legislative mandate", async () => {
        const checkBox = page.getByTestId("legislative-mandate")
        await checkBox.uncheck()
        await expect(checkBox).not.toBeChecked()
        await save(page)
      })

      await test.step("check legislative mandate is not visible in preview", async () => {
        await navigateToPreview(
          page,
          prefilledDocumentUnit.documentNumber as string,
        )
        await expect(page.getByText("Gesetzgebungsauftrag")).toBeHidden()
      })

      await test.step("XML preview should not display 'Gesetzgebungsauftrag' in 'paratrubriken'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await page.getByText("XML Vorschau").click()
        const xmlPreview = page.getByTitle("XML Vorschau")
        const innerText = await xmlPreview.innerText()
        expect(innerText).not.toContain("<aspekt>Gesetzgebungsauftrag</aspekt>")
      })
    })

    async function reloadWithCheck(page: Page) {
      await test.step("reload page and check that checkbox is checked and button is hidden", async () => {
        await page.reload()
        const checkBox = page.getByTestId("legislative-mandate")
        await expect(checkBox).toBeChecked()
        await expect(
          page.getByText("Gesetzgebungsauftrag vorhanden"),
        ).toBeVisible()
        await expect(
          page.getByRole("button", { name: "Gesetzgebungsauftrag" }),
        ).toBeHidden()
      })
    }

    async function addCourtType(page: Page, courtType: string) {
      await test.step(`add court type : ${courtType}`, async () => {
        await page.locator("[aria-label='Gericht']").fill(courtType)
        await waitForInputValue(page, "[aria-label='Gericht']", courtType)
        await expect(page.getByText(courtType)).toBeVisible()
        await page.getByText(courtType).click()
        await waitForInputValue(page, "[aria-label='Gericht']", courtType)

        await save(page)
      })
    }

    async function checkLegislativeMandate(page: Page) {
      await test.step("check legislative mandate", async () => {
        const checkBox = page.getByTestId("legislative-mandate")
        await checkBox.check()
        await expect(checkBox).toBeChecked()
        await save(page)
      })
    }
  },
)

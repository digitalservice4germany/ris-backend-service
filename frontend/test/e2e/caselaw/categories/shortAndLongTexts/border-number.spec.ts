import { expect } from "@playwright/test"
import {
  navigateToCategories,
  navigateToFiles,
  uploadTestfile,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

// eslint-disable-next-line playwright/no-skipped-test
test.skip(
  ({ browserName }) => browserName !== "chromium",
  "Skipping in engines other than chromium, reason playwright driven for firefox and safari does not support copy paste type='text/html' from clipboard",
)

test.beforeEach(async ({ page, documentNumber }) => {
  await navigateToFiles(page, documentNumber)
})

/*
    1. Upload document with border numbers
    2. Copy border Numbers (Randnummern) into reasons
    3. Delete all border Numbers (Randnummern) with button
    4. Delete only selected border Numbers (Randnummern) via button
    5. Delete border number via backspace in content
    6. delete border number via backspace in number
*/
// eslint-disable-next-line playwright/no-skipped-test
test.describe(
  "Remove border numbers (Randnummern)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4119",
    },
  },
  () => {
    test("delete border Numbers (Randnummern) via button and backspace", async ({
      page,
      documentNumber,
    }) => {
      const documentOrigin = "Gründe:"
      const firstReason = "First reason"
      const secondReason = "Second reason"
      const thirdReason = "Third reason"
      // eslint-disable-next-line playwright/no-conditional-in-test
      const modifier = (await page.evaluate(() => navigator.platform))
        .toLowerCase()
        .includes("mac")
        ? "Meta"
        : "Control"

      await test.step("Upload file with border Numbers (Randnummern)", async () => {
        await uploadTestfile(page, "some-border-numbers.docx")
        await expect(page.getByText("some-border-numbers.docx")).toBeVisible()
        await expect(page.getByLabel("Datei löschen")).toBeVisible()
        await expect(page.getByText(firstReason)).toBeVisible()
        await expect(page.getByText(secondReason)).toBeVisible()
        await expect(page.getByText(thirdReason)).toBeVisible()
      })

      await test.step("Click on 'Rubriken' und check if original document loaded", async () => {
        await navigateToCategories(page, documentNumber)
        await expect(page.getByLabel("Ladestatus")).toBeHidden()
        await expect(page.getByText(firstReason)).toBeVisible()
        await expect(page.getByText(secondReason)).toBeVisible()
        await expect(page.getByText(thirdReason)).toBeVisible()
        await expect(page.getByText(documentOrigin)).toBeVisible()
      })

      const editor = page.locator("[data-testid='Gründe']")
      let inputFieldInnerHTML = await editor.innerText()

      await test.step("Copy border numbers (Randnummern) from side panel into reasons to have reference data", async () => {
        // Selected all text from sidepanel
        await page.getByText(documentOrigin).evaluate((element) => {
          const originalFile =
            element.parentElement?.parentElement?.parentElement

          if (!originalFile) {
            throw new Error("No original file available.")
          }

          const selection = window.getSelection()
          const elementChildsLength = originalFile.childNodes.length
          const range = document.createRange()
          range.setStart(originalFile.childNodes[0], 0)
          range.setEnd(originalFile.childNodes[elementChildsLength - 1], 0)
          selection?.removeAllRanges()
          selection?.addRange(range)
        })

        // copy from side panel to clipboard
        await page.keyboard.press(`${modifier}+KeyC`)

        // paste from clipboard into input field "Entscheidungsgründe"
        await editor.click()
        await page.keyboard.press(`${modifier}+KeyV`)
      })

      await checkAllBorderNumbersAreVisible()

      await test.step("Select all text", async () => {
        await page.keyboard.press(`${modifier}+KeyA`)
      })

      await clickBorderNumberButton()

      await test.step("Check all border Numbers (Randnummern) have gone", async () => {
        inputFieldInnerHTML = await editor.innerText()
        expect(inputFieldInnerHTML.includes("1\n\n" + firstReason)).toBeFalsy()
        expect(inputFieldInnerHTML.includes("2\n\n" + secondReason)).toBeFalsy()
        expect(inputFieldInnerHTML.includes("3\n\n" + thirdReason)).toBeFalsy()
      })

      await reinsertAllBorderNumbers()

      await test.step("Select text of last border number (Randnummer)", async () => {
        await page.keyboard.down("Shift")
        for (let i = 0; i < 10; i++) {
          await page.keyboard.press("ArrowLeft")
        }
        await page.keyboard.up("Shift")
      })

      await clickBorderNumberButton()

      await checkLastBorderNumberHasGone()

      await reinsertAllBorderNumbers()

      await test.step("Navigate cursor to the start of the last border number content", async () => {
        for (let i = 0; i < 13; i++) {
          await page.keyboard.press("ArrowLeft")
        }
      })

      await clickBackspace()

      await checkLastBorderNumberHasGone()

      await reinsertAllBorderNumbers()

      await test.step("Navigate cursor to the last border number number", async () => {
        for (let i = 0; i < 14; i++) {
          await page.keyboard.press("ArrowLeft")
        }
      })

      await clickBackspace()

      await checkLastBorderNumberHasGone()

      async function clickBackspace() {
        await test.step("Press Backspace to delete last border number", async () => {
          await page.keyboard.press("Backspace")
        })
      }

      async function clickBorderNumberButton() {
        await test.step("Click border number button to delete border numbers from selection", async () => {
          await page.getByLabel("borderNumber").click()
        })
      }

      async function reinsertAllBorderNumbers() {
        await test.step("Reinsert all border numbers (Randnummern)", async () => {
          await page.keyboard.press(`${modifier}+KeyA`)
          await page.keyboard.press(`${modifier}+KeyV`)
          await checkAllBorderNumbersAreVisible()
        })
      }

      async function checkAllBorderNumbersAreVisible() {
        await test.step("Check all border numbers (Randnummern) are visible", async () => {
          inputFieldInnerHTML = await editor.innerText()
          expect(
            inputFieldInnerHTML.includes("1\n\n" + firstReason),
          ).toBeTruthy()
          expect(
            inputFieldInnerHTML.includes("2\n\n" + secondReason),
          ).toBeTruthy()
          expect(
            inputFieldInnerHTML.includes("3\n\n" + thirdReason),
          ).toBeTruthy()
        })
      }

      async function checkLastBorderNumberHasGone() {
        await test.step("Check the last border Number (Randnummer) has gone", async () => {
          inputFieldInnerHTML = await editor.innerText()
          expect(
            inputFieldInnerHTML.includes("1\n\n" + firstReason),
          ).toBeTruthy()
          expect(
            inputFieldInnerHTML.includes("2\n\n" + secondReason),
          ).toBeTruthy()
          expect(
            inputFieldInnerHTML.includes("3\n\n" + thirdReason),
          ).toBeFalsy()
        })
      }
    })
  },
)

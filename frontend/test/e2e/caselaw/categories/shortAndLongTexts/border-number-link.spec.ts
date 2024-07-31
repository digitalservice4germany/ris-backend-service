import { expect } from "@playwright/test"
import {
  navigateToCategories,
  navigateToAttachments,
  save,
  uploadTestfile,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

// eslint-disable-next-line playwright/no-skipped-test
test.skip(
  ({ browserName }) => browserName !== "chromium",
  "Skipping in engines other than chromium, reason playwright diriven for firefox and safari does not support copy paste type='text/html' from clipboard",
)

test.beforeEach(async ({ page, documentNumber }) => {
  await navigateToAttachments(page, documentNumber)
})

/*
    1. Upload document with border numbers
    2. Copy border numbers into reasons
    3. Create a valid and an invalid border number link in the Leitsatz
    4. Check validation state after save
    5. Delete original border numbers in reasons
    6. Check validation state after save
*/
// eslint-disable-next-line playwright/no-skipped-test
test("create and validate border number links", async ({
  page,
  documentNumber,
}) => {
  // Copy border numbers from side panel into reasons to have reference data
  const documentOrigin = "Gründe:"
  const firstReason = "First reason"
  const secondReason = "Second reason"
  const thirdReason = "Third reason"

  // upload file
  await uploadTestfile(page, "some-border-numbers.docx")
  await expect(page.getByText("some-border-numbers.docx")).toBeVisible()
  await expect(page.getByLabel("Datei löschen")).toBeVisible()
  await expect(page.getByText(firstReason)).toBeVisible()
  await expect(page.getByText(secondReason)).toBeVisible()
  await expect(page.getByText(thirdReason)).toBeVisible()

  // Click on "Rubriken" und check if original document loaded
  await navigateToCategories(page, documentNumber)
  await expect(page.getByLabel("Ladestatus")).toBeHidden()
  await expect(page.getByText(firstReason)).toBeVisible()
  await expect(page.getByText(secondReason)).toBeVisible()
  await expect(page.getByText(thirdReason)).toBeVisible()
  const originalFileParagraph = page.getByText(documentOrigin)
  await expect(originalFileParagraph).toBeVisible()

  // Selected all text from sidepanel
  await originalFileParagraph.evaluate((element) => {
    const originalFile = element.parentElement?.parentElement?.parentElement

    if (!originalFile) {
      throw new Error("No original file available.")
    }

    const selection = window.getSelection()
    const elementChildsLength = originalFile.childNodes.length
    const startOffset = 0
    const range = document.createRange()
    range.setStart(originalFile.childNodes[0], startOffset)
    range.setEnd(originalFile.childNodes[elementChildsLength - 1], startOffset)
    selection?.removeAllRanges()
    selection?.addRange(range)
  })

  // copy from sidepanel to clipboard
  // eslint-disable-next-line playwright/no-conditional-in-test
  const modifier = (await page.evaluate(() => navigator.platform))
    .toLowerCase()
    .includes("mac")
    ? "Meta"
    : "Control"
  await page.keyboard.press(`${modifier}+KeyC`)

  // paste from clipboard into input field "Entscheidungsgründe"
  const inputField = page.locator("[data-testid='Gründe']")
  await inputField.click()
  await page.keyboard.press(`${modifier}+KeyV`)
  const inputFieldInnerHTML = await inputField.innerText()

  // Check all text copied
  const inputFieldAllText = await inputField.allTextContents()
  expect(inputFieldAllText[0]).toContain(firstReason)
  expect(inputFieldAllText[0]).toContain(secondReason)
  expect(inputFieldAllText[0]).toContain(thirdReason)
  expect(inputFieldInnerHTML).toContain(firstReason)
  expect(inputFieldInnerHTML).toContain(secondReason)
  expect(inputFieldInnerHTML).toContain(thirdReason)

  // Create valid and invalid border number links in Leitsatz
  const guidingPrincipleInput = page.locator("[data-testid='Leitsatz']")
  await guidingPrincipleInput.click()
  await page.keyboard.type(`#1# #4# #99999# #1000000# #not a border number#`)

  // save
  await save(page)

  // check valid border number link
  const locators = await page
    .locator("[data-testid='Leitsatz']")
    .locator("border-number-link")
    .all()

  // only three of the input values should be rendered as borderNumberLinks
  expect(locators.length).toBe(3)

  const validLink = locators[0]
  const invalidLink = locators[1]
  const invalidHighestNumberLink = locators[2]

  await expect(validLink).toHaveAttribute("valid", "true")
  await expect(validLink).toHaveClass(
    'font-bold text-white bg-blue-700 before:content-["Rd_"]',
  )

  // check invalid border number links
  await expect(invalidLink).toHaveAttribute("valid", "false")
  await expect(invalidLink).toHaveClass(
    'font-bold text-red-900 bg-red-200 before:content-["⚠Rd_"]',
  )
  await expect(invalidHighestNumberLink).toHaveAttribute("valid", "false")
  await expect(invalidHighestNumberLink).toHaveClass(
    'font-bold text-red-900 bg-red-200 before:content-["⚠Rd_"]',
  )

  // Delete border numbers in reasons
  const reasons = page.locator("[data-testid='Gründe']")
  await reasons.click()
  await page.keyboard.press(`${modifier}+KeyA`)
  await page.keyboard.press(`${modifier}+Backspace`)

  // save
  await save(page)

  // check first border number link: should be invalid now
  await expect(validLink).toHaveAttribute("valid", "false")
  await expect(validLink).toHaveClass(
    'font-bold text-red-900 bg-red-200 before:content-["⚠Rd_"]',
  )
})

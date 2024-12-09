import { expect, Locator, Page } from "@playwright/test"
import {
  navigateToCategories,
  navigateToAttachments,
  uploadTestfile,
  copyPasteTextFromAttachmentIntoEditor,
  clickCategoryButton,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"

// eslint-disable-next-line playwright/no-skipped-test
test.skip(
  ({ browserName }) => browserName !== "chromium",
  "Skipping in engines other than chromium, reason playwright driven for firefox and safari does not support copy paste type='text/html' from clipboard",
)

test.beforeEach(async ({ page, documentNumber }) => {
  await navigateToAttachments(page, documentNumber)
})

const documentOrigin = "Headline:"
const firstParagraph = "First paragraph"
const secondParagraph = "Second paragraph"
const thirdParagraph = "Third paragraph"
const firstParagraphHtml =
  '<span style="color: rgb(0, 0, 0)">First <strong>paragraph</strong></span>'

test.describe(
  "Remove border numbers (Randnummern)",
  {
    annotation: [
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4119",
      },
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4973",
      },
    ],
    tag: ["@RISDEV-4119", "@RISDEV-4973"],
  },
  () => {
    /*
      1. Upload document with border numbers
      2. Copy border Numbers into reasons
      3. Select all and delete border Numbers via button
      4. Select first border number text and delete via button
      5. Delete border number via backspace in content
      6. delete border number via backspace in number
      7. Insert cursor into first border number text and delete via button
      8. Select first paragraph (without border number) and click button (only recalculates the following border numbers)
    */
    test("delete border Numbers (Randnummern) via button and backspace in 'Gründe'", async ({
      page,
      documentNumber,
    }) => {
      await test.step("Upload file with border Numbers", async () => {
        await uploadTestfile(page, "some-border-numbers.docx")
        await expect(page.getByText("some-border-numbers.docx")).toBeVisible()
        await expect(page.getByLabel("Datei löschen")).toBeVisible()
        await expect(page.getByText(firstParagraph)).toBeVisible()
        await expect(page.getByText(secondParagraph)).toBeVisible()
        await expect(page.getByText(thirdParagraph)).toBeVisible()
      })

      await test.step("Click on 'Rubriken' und check if original document loaded", async () => {
        await navigateToCategories(page, documentNumber)
        await expect(page.getByLabel("Ladestatus")).toBeHidden()
        await expect(page.getByText(firstParagraph)).toBeVisible()
        await expect(page.getByText(secondParagraph)).toBeVisible()
        await expect(page.getByText(thirdParagraph)).toBeVisible()
        await expect(page.getByText(documentOrigin)).toBeVisible()
      })

      const attachmentLocator = page
        .getByText(documentOrigin)
        .locator("..")
        .locator("..")
        .locator("..")

      await clickCategoryButton("Gründe", page)
      const editor = page.getByTestId("Gründe")

      await test.step("Copy border numbers from side panel into 'Gründe' to have reference data", async () => {
        await copyPasteTextFromAttachmentIntoEditor(
          page,
          attachmentLocator,
          editor,
        )
      })

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickRemoveBorderNumberButton(page)

      await checkAllBorderNumbersAreRemoved(editor)

      await reinsertAllBorderNumbers(page)

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Select text of first border number", async () => {
        await editor.getByText(firstParagraph).selectText()
      })

      await clickRemoveBorderNumberButton(page)

      await checkFirstBorderNumberIsRemoved(editor)

      await checkOtherBorderNumbersAreRecalculated(editor)

      await reinsertAllBorderNumbers(page)

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Navigate cursor to the start of the first border number content", async () => {
        await editor.getByText(firstParagraph).selectText()
        await page.keyboard.press("ArrowLeft")
      })

      await clickBackspace(page)

      await checkFirstBorderNumberIsRemoved(editor)

      await checkOtherBorderNumbersAreRecalculated(editor)

      await reinsertAllBorderNumbers(page)

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Navigate cursor to the first border number number", async () => {
        await editor.getByText(firstParagraph).selectText()
        await page.keyboard.press("ArrowLeft")
        await page.keyboard.press("ArrowLeft")
      })

      await clickBackspace(page)

      await checkFirstBorderNumberIsRemoved(editor)

      await checkOtherBorderNumbersAreRecalculated(editor)

      await reinsertAllBorderNumbers(page)

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Navigate cursor inside first border number content (without selection)", async () => {
        await editor.getByText(firstParagraph).click()
      })

      await clickRemoveBorderNumberButton(page)

      await checkFirstBorderNumberIsRemoved(editor)

      await checkOtherBorderNumbersAreRecalculated(editor)

      await test.step("Change number of second border number to 99", async () => {
        await editor.getByText("2").selectText()
        await page.keyboard.insertText("99")
      })

      await test.step("Select text of first border number which has been removed", async () => {
        await editor.getByText(firstParagraph).selectText()
      })

      await clickRemoveBorderNumberButton(page)

      await checkOtherBorderNumbersAreRecalculated(editor)
    })

    test("delete border Numbers (Randnummern) in 'Leitsatz'", async ({
      page,
      documentNumber,
    }) => {
      await test.step("Upload file with border Numbers", async () => {
        await uploadTestfile(page, "some-border-numbers.docx")
        await expect(page.getByText("some-border-numbers.docx")).toBeVisible()
        await expect(page.getByLabel("Datei löschen")).toBeVisible()
        await expect(page.getByText(firstParagraph)).toBeVisible()
        await expect(page.getByText(secondParagraph)).toBeVisible()
        await expect(page.getByText(thirdParagraph)).toBeVisible()
      })

      await test.step("Click on 'Rubriken' und check if original document loaded", async () => {
        await navigateToCategories(page, documentNumber)
        await expect(page.getByLabel("Ladestatus")).toBeHidden()
        await expect(page.getByText(firstParagraph)).toBeVisible()
        await expect(page.getByText(secondParagraph)).toBeVisible()
        await expect(page.getByText(thirdParagraph)).toBeVisible()
        await expect(page.getByText(documentOrigin)).toBeVisible()
      })

      const attachmentLocator = page
        .getByText(documentOrigin)
        .locator("..")
        .locator("..")
        .locator("..")

      await clickCategoryButton("Leitsatz", page)
      const editor = page.locator("[data-testid='Leitsatz']")

      await test.step("Copy border numbers from side panel into 'Leitsatz' to have reference data", async () => {
        await copyPasteTextFromAttachmentIntoEditor(
          page,
          attachmentLocator,
          editor,
        )
      })

      await checkAllBorderNumbersAreVisible(editor)

      await checkStyleOfFirstParagraph(editor)

      await test.step("Select text of first border number", async () => {
        await editor.getByText(firstParagraph).selectText()
      })

      await clickRemoveBorderNumberButton(page)

      await checkFirstBorderNumberIsRemoved(editor)

      await checkOtherBorderNumbersAreNotRecalculated(editor)
    })
  },
)

test.describe(
  "Add border numbers (Randnummern)",
  {
    annotation: [
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4973",
      },
    ],
    tag: ["@RISDEV-4973"],
  },
  () => {
    // eslint-disable-next-line playwright/expect-expect
    test("add border Numbers (Randnummern) via button in 'Gründe'", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Gründe", page)
      const editor = page.locator("[data-testid='Gründe']")

      await test.step("Add three paragraphs into Gründe", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(editor)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await checkAllBorderNumbersAreVisible(editor)

      await test.step("Change number of second border number to 99", async () => {
        await editor.getByText("2").selectText()
        await page.keyboard.insertText("99")
      })

      await test.step("Select text of first border number", async () => {
        await editor.getByText(firstParagraph).selectText()
      })

      await clickAddBorderNumberButton(page)

      await checkAllBorderNumbersAreVisible(editor)
    })

    test("add border Numbers (Randnummern) in correct order ('Gründe', 'Abweichende Meinung' and 'Sonstiger Langtext')", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      // Gründe
      await clickCategoryButton("Gründe", page)
      const reasons = page.locator("[data-testid='Gründe']")

      await test.step("Add three paragraphs into 'Gründe'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(reasons)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await checkAllBorderNumbersAreVisible(reasons)

      // Abweichende Meinung
      await clickCategoryButton("Abweichende Meinung", page)
      const dissentingOpinion = page.locator(
        "[data-testid='Abweichende Meinung']",
      )

      await test.step("Add three paragraphs into 'Abweichende Meinung'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(dissentingOpinion)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await test.step("Check all border numbers are visible and have correct sequence", async () => {
        await expect(
          dissentingOpinion.getByText(`4${firstParagraph}`),
        ).toBeVisible()
        await expect(
          dissentingOpinion.getByText(`5${secondParagraph}`),
        ).toBeVisible()
        await expect(
          dissentingOpinion.getByText(`6${thirdParagraph}`),
        ).toBeVisible()
      })

      // Sonstiger Langtext
      await clickCategoryButton("Sonstiger Langtext", page)
      const otherLongText = page.locator("[data-testid='Sonstiger Langtext']")

      await test.step("Add three paragraphs into 'Sonstiger Langtext'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(otherLongText)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await test.step("Check all border numbers are visible and have correct sequence", async () => {
        await expect(
          otherLongText.getByText(`7${firstParagraph}`),
        ).toBeVisible()
        await expect(
          otherLongText.getByText(`8${secondParagraph}`),
        ).toBeVisible()
        await expect(
          otherLongText.getByText(`9${thirdParagraph}`),
        ).toBeVisible()
      })
    })

    test("add border Numbers (Randnummern) in correct order ('Tatbestand', 'Abweichende Meinung' and 'Sonstiger Langtext')", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      // Tatbestand
      await clickCategoryButton("Tatbestand", page)
      const casefacts = page.locator("[data-testid='Tatbestand']")

      await test.step("Add three paragraphs into 'Tatbestand'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(casefacts)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await checkAllBorderNumbersAreVisible(casefacts)

      // Abweichende Meinung
      await clickCategoryButton("Abweichende Meinung", page)
      const dissentingOpinion = page.locator(
        "[data-testid='Abweichende Meinung']",
      )

      await test.step("Add three paragraphs into 'Abweichende Meinung'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(dissentingOpinion)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await test.step("Check all border numbers are visible and have correct sequence", async () => {
        await expect(
          dissentingOpinion.getByText(`4${firstParagraph}`),
        ).toBeVisible()
        await expect(
          dissentingOpinion.getByText(`5${secondParagraph}`),
        ).toBeVisible()
        await expect(
          dissentingOpinion.getByText(`6${thirdParagraph}`),
        ).toBeVisible()
      })

      // Sonstiger Langtext
      await clickCategoryButton("Sonstiger Langtext", page)
      const otherLongText = page.locator("[data-testid='Sonstiger Langtext']")

      await test.step("Add three paragraphs into 'Sonstiger Langtext'", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(otherLongText)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await test.step("Check all border numbers are visible and have correct sequence", async () => {
        await expect(
          otherLongText.getByText(`7${firstParagraph}`),
        ).toBeVisible()
        await expect(
          otherLongText.getByText(`8${secondParagraph}`),
        ).toBeVisible()
        await expect(
          otherLongText.getByText(`9${thirdParagraph}`),
        ).toBeVisible()
      })
    })
  },
)

test.describe(
  "Add/remove border numbers with shortcuts (Randnummern)",
  {
    annotation: [
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-5137",
      },
    ],
    tag: ["@RISDEV-5137"],
  },
  () => {
    test("add remove border numbers (Randnummern) via shortcuts", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Tatbestand", page)
      const editor = page.locator("[data-testid='Tatbestand']")

      await test.step("Add one paragraph into Tatbestand", async () => {
        await page.keyboard.insertText(firstParagraph)
        await expect(editor.getByText(firstParagraph)).toBeVisible()
        await editor.getByText(firstParagraph).click()
      })

      await test.step("Add border number via shortcut", async () => {
        await page.keyboard.press(`ControlOrMeta+Alt+.`)
        await expect(editor.getByText(`1${firstParagraph}`)).toBeVisible()
      })

      await test.step("Remove border number via shortcut", async () => {
        await page.keyboard.press(`ControlOrMeta+Alt+-`)
        await expect(editor.getByText(`1${firstParagraph}`)).toBeHidden()
      })
    })
  },
)

test.describe(
  "Copy/paste border-numbers on top-level only (Randnummern)",
  {
    annotation: [
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-5136",
      },
    ],
    tag: ["@RISDEV-5136"],
  },
  () => {
    test("Paste a border number within another border number", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Entscheidungsgründe", page)
      const editor = page.locator("[data-testid='Entscheidungsgründe']")

      await test.step("Add two paragraphs into Entscheidungsgründe", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await expect(editor.getByText(firstParagraph)).toBeVisible()
        await expect(editor.getByText(secondParagraph)).toBeVisible()
      })

      await test.step("Add border numbers to the paragraphs", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
        await page.keyboard.press(`ControlOrMeta+Alt+.`)
      })

      await test.step("Copy paragraphs with border numbers", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
        await page.keyboard.press(`ControlOrMeta+C`)
      })

      await test.step("Paste paragraphs into middle of first border number content", async () => {
        const middleOfFirstParagraph = firstParagraph.slice(4)
        await editor.getByText(middleOfFirstParagraph).click()
        await page.keyboard.press(`ControlOrMeta+V`)
      })

      await test.step("Check border numbers were inserted after the first paragraph", async () => {
        // We check the newly inserted paragraphs first, so that we don't run into strict mode violation.
        // Before recalculation, we have two firstParagraph elements with border number 1
        await expect(editor.getByText(`2${firstParagraph}`)).toBeVisible()
        await expect(editor.getByText(`3${secondParagraph}`)).toBeVisible()

        await expect(editor.getByText(`1${firstParagraph}`)).toBeVisible()
        await expect(editor.getByText(`4${secondParagraph}`)).toBeVisible()
      })
    })

    test("Paste a border number to an empty paragraph", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Entscheidungsgründe", page)
      const editor = page.locator("[data-testid='Entscheidungsgründe']")

      await test.step("Add two paragraphs into Entscheidungsgründe", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await expect(editor.getByText(firstParagraph)).toBeVisible()
        await expect(editor.getByText(secondParagraph)).toBeVisible()
      })

      await test.step("Add border numbers to the paragraphs", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
        await page.keyboard.press(`ControlOrMeta+Alt+.`)
      })

      await test.step("Copy paragraphs with border numbers", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
        await page.keyboard.press(`ControlOrMeta+C`)
      })

      await test.step("Paste paragraphs into empty paragraph", async () => {
        // Move caret to end of input
        await editor.getByText(`2${secondParagraph}`).click()
        await page.keyboard.press(`ControlOrMeta+End`)
        await page.keyboard.press(`ControlOrMeta+V`)
      })

      await test.step("Check border numbers were inserted after the first paragraph", async () => {
        // We check the newly inserted paragraphs first, so that we don't run into strict mode violation.
        // Before recalculation, we have two firstParagraph elements with border number 1
        await expect(editor.getByText(`3${firstParagraph}`)).toBeVisible()
        await expect(editor.getByText(`4${secondParagraph}`)).toBeVisible()

        await expect(editor.getByText(`1${firstParagraph}`)).toBeVisible()
        await expect(editor.getByText(`2${secondParagraph}`)).toBeVisible()
      })
    })
  },
)

test.describe(
  "Fuse border numbers (Randnummern)",
  {
    annotation: [
      {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4973",
      },
    ],
    tag: ["@RISDEV-4973"],
  },
  () => {
    test("fuse two border Numbers (Randnummern)", async ({
      page,
      documentNumber,
    }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Gründe", page)
      const editor = page.locator("[data-testid='Gründe']")

      await test.step("Add three paragraphs into Gründe", async () => {
        await page.keyboard.insertText(firstParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(secondParagraph)
        await page.keyboard.press("Enter")
        await page.keyboard.insertText(thirdParagraph)
        await page.keyboard.press("Enter")
      })

      await checkAllParagraphsAreVisible(editor)

      await test.step("Select all text", async () => {
        await page.keyboard.press(`ControlOrMeta+A`)
      })

      await clickAddBorderNumberButton(page)

      await checkAllBorderNumbersAreVisible(editor)

      await test.step("Fuse the second with the first border number", async () => {
        await editor.getByText(secondParagraph).selectText()
        await page.keyboard.press("ArrowLeft")
        await clickBackspace(page)
        await clickBackspace(page)
        await page.keyboard.press("Enter")
      })

      await test.step("Check the second border number is gone and third border number is recalculated", async () => {
        await expect(editor.getByText(`1${firstParagraph}`)).toBeVisible()
        await expect(editor.getByText(`2${secondParagraph}`)).toBeHidden()
        await expect(editor.getByText(secondParagraph)).toBeVisible()
        await expect(editor.getByText(`2${thirdParagraph}`)).toBeVisible()
      })
    })
  },
)

async function clickBackspace(page: Page) {
  await test.step("Press Backspace", async () => {
    await page.keyboard.press("Backspace")
  })
}

async function clickRemoveBorderNumberButton(page: Page) {
  await test.step("Click remove border number button to delete border numbers from selection", async () => {
    await page
      .locator(`[aria-label='Randnummern entfernen']:not([disabled])`)
      .click()
  })
}

async function clickAddBorderNumberButton(page: Page) {
  await test.step("Click add number button to add border numbers to selection", async () => {
    await page
      .locator(`[aria-label='Randnummern neu erstellen']:not([disabled])`)
      .click()
  })
}

async function reinsertAllBorderNumbers(page: Page) {
  await test.step("Reinsert all border numbers", async () => {
    await page.keyboard.press(`ControlOrMeta+A`)
    await page.keyboard.press(`Delete`)
    await page.keyboard.press(`ControlOrMeta+V`)
  })
}

async function checkAllParagraphsAreVisible(editor: Locator) {
  await test.step("Check all paragraphs are visible and have correct sequence", async () => {
    await expect(editor.getByText(firstParagraph)).toBeVisible()
    await expect(editor.getByText(secondParagraph)).toBeVisible()
    await expect(editor.getByText(thirdParagraph)).toBeVisible()
  })
}

async function checkAllBorderNumbersAreVisible(editor: Locator) {
  await test.step("Check all border numbers are visible and have correct sequence", async () => {
    await expect(editor.getByText(`1${firstParagraph}`)).toBeVisible()
    await expect(editor.getByText(`2${secondParagraph}`)).toBeVisible()
    await expect(editor.getByText(`3${thirdParagraph}`)).toBeVisible()
  })
}

async function checkStyleOfFirstParagraph(editor: Locator) {
  const inputFieldInnerHtml = await editor.innerHTML()
  expect(inputFieldInnerHtml).toContain(firstParagraphHtml)
}

async function checkFirstBorderNumberIsRemoved(editor: Locator) {
  await test.step("Check the first border Number is removed", async () => {
    await expect(editor.getByText(`1${firstParagraph}`)).toBeHidden()
    await expect(editor.getByText(`${firstParagraph}`)).toBeVisible()
    expect(await editor.innerHTML()).toContain(firstParagraphHtml)
  })
}

async function checkOtherBorderNumbersAreRecalculated(editor: Locator) {
  await test.step("Check the other border numbers are recalculated", async () => {
    await expect(editor.getByText(`1${secondParagraph}`)).toBeVisible()
    await expect(editor.getByText(`2${thirdParagraph}`)).toBeVisible()
  })
}

async function checkOtherBorderNumbersAreNotRecalculated(editor: Locator) {
  await test.step("Check the other border numbers are not recalculated", async () => {
    await expect(editor.getByText(`2${secondParagraph}`)).toBeVisible()
    await expect(editor.getByText(`3${thirdParagraph}`)).toBeVisible()
  })
}

async function checkAllBorderNumbersAreRemoved(editor: Locator) {
  await test.step("Check all border Numbers have gone", async () => {
    await expect(editor.getByText(`1${firstParagraph}`)).toBeHidden()
    await expect(editor.getByText(`2${secondParagraph}`)).toBeHidden()
    await expect(editor.getByText(`3${thirdParagraph}`)).toBeHidden()
  })
}

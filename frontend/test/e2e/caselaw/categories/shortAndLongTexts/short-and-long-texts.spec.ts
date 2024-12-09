import { expect, Page } from "@playwright/test"
import {
  clickCategoryButton,
  navigateToCategories,
  navigateToHandover,
  navigateToPreview,
  save,
  waitForInputValue,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"
import { DocumentUnitCatagoriesEnum } from "@/components/enumDocumentUnitCatagories"
import DocumentUnit from "@/domain/documentUnit"

test.describe("short and long texts", () => {
  test(
    "decision name should update",
    {
      annotation: {
        type: "issue",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4733",
      },
      tag: ["@RISDEV-4733"],
    },
    async ({ page, documentNumber }) => {
      await navigateToCategories(page, documentNumber, {
        category: DocumentUnitCatagoriesEnum.TEXTS,
      })
      await clickCategoryButton("Entscheidungsname", page)
      const inputText = "Family-Karte"
      const selector = "[aria-label='Entscheidungsname']"
      await page.locator(selector).fill(inputText)
      await waitForInputValue(page, selector, inputText)
      await save(page)
      await expect(page.locator(selector)).toHaveValue(inputText)
    },
  )

  test("text editor fields should have predefined height", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    // small
    await clickCategoryButton("Titelzeile", page)
    const smallEditor = page.locator("[data-testid='Titelzeile']")
    const smallEditorHeight = await smallEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height"),
    )
    expect(parseInt(smallEditorHeight)).toBeGreaterThanOrEqual(60)

    //medium
    await clickCategoryButton("Leitsatz", page)
    const mediumEditor = page.locator("[data-testid='Leitsatz']")
    const mediumEditorHeight = await mediumEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height"),
    )
    expect(parseInt(mediumEditorHeight)).toBeGreaterThanOrEqual(120)

    //large
    await clickCategoryButton("Gründe", page)
    const largeEditor = page.locator("[data-testid='Gründe']")
    const largeEditorHeight = await largeEditor.evaluate((element) =>
      window.getComputedStyle(element).getPropertyValue("height"),
    )
    expect(parseInt(largeEditorHeight)).toBeGreaterThanOrEqual(320)
  })

  test("toggle invisible characters", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)

    await clickCategoryButton("Leitsatz", page)
    const guidingPrincipleInput = page.locator("[data-testid='Leitsatz']")
    await guidingPrincipleInput.click()
    await page.keyboard.type(`this is a test paragraph`)

    await expect(
      guidingPrincipleInput.locator("[class='ProseMirror-trailingBreak']"),
    ).toHaveCount(1)
    await page
      .locator(`[aria-label='Nicht-druckbare Zeichen']:not([disabled])`)
      .click()
    await expect(
      guidingPrincipleInput.locator("[class='ProseMirror-trailingBreak']"),
    ).toHaveCount(0)
  })

  test(
    "text editor keyboard navigation",
    {
      annotation: {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4432",
      },
      tag: ["@RISDEV-3682", "@RISDEV-4432"],
    },
    async ({ page, documentNumber }) => {
      await navigateToCategories(page, documentNumber)

      await clickCategoryButton("Leitsatz", page)
      const guidingPrincipleInput = page.locator(
        "[data-testid='Leitsatz'] > div.tiptap",
      )
      await guidingPrincipleInput.click()
      await expect(guidingPrincipleInput).toBeFocused()

      // Write text and select all
      await page.keyboard.type("Text input")

      // Wait for text to be typed in the editor and selected via keyboard
      // eslint-disable-next-line playwright/no-wait-for-timeout
      await page.waitForTimeout(100)
      await page.keyboard.press("ControlOrMeta+A")
      // eslint-disable-next-line playwright/no-wait-for-timeout
      await page.waitForTimeout(100)

      // Navigate to toolbar -> first button is focused
      await page.keyboard.press("Shift+Tab")
      const firstButton = page
        .getByLabel("Leitsatz Button Leiste")
        .getByLabel("Erweitern")
      await expect(firstButton).toBeFocused()

      // Navigate to bold button with arrow keys
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowLeft")
      const boldButton = page
        .getByLabel("Leitsatz Button Leiste")
        .getByLabel("Fett")
      await expect(boldButton).toBeFocused()

      // Pressing enter moves focus to the editor
      await page.keyboard.press("Enter")
      await expect(guidingPrincipleInput).toBeFocused()

      // The timeout is needed for the focus, otherwise tabbing back to the menu is flaky. It's unclear why.
      // eslint-disable-next-line playwright/no-wait-for-timeout
      await page.waitForTimeout(300)

      // Tiptap inserts invisible characters -> input is split into two parts
      expect(await page.getByText("Text input").innerHTML()).toContain(
        "<strong>Text</strong>",
      )
      expect(await page.getByText("Text input").innerHTML()).toContain(
        "<strong> input</strong>",
      )

      // The timeout is needed for the focus, otherwise tabbing back to the menu is flaky. It's unclear why.
      // eslint-disable-next-line playwright/no-wait-for-timeout
      await page.waitForTimeout(100)

      // Tabbing back into the toolbar sets focus to last active button
      await page.keyboard.press("Shift+Tab")
      await expect(boldButton).toBeFocused()

      // Move to alignment submenu and open it with Enter
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("ArrowRight")
      await page.keyboard.press("Enter")

      // Navigate to submenu button
      await page.keyboard.press("ArrowRight")
      const leftButton = page
        .getByLabel("Leitsatz Button Leiste")
        .getByLabel("Linksbündig")
      await expect(leftButton).toBeFocused()

      // Close submenu with ESC
      await page.keyboard.press("Escape")
      await expect(leftButton).toBeHidden()
    },
  )

  test(
    "long texts are hidden for external user",
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

      await test.step("Tenor ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Tenor",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Tatbestand ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Tatbestand",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Entscheidungsgründe sind nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Entscheidungsgründe",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Gründe sind nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Gründe",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Abweichende Meinung ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Abweichende Meinung",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Mitwirkende Richter ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Mitwirkende Richter",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Sonstiger Langtext ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Sonstiger Langtext",
          exact: true,
        })
        await expect(button).toBeHidden()
      })

      await test.step("Gliederung ist nicht sichtbar", async () => {
        const button = pageWithExternalUser.getByRole("button", {
          name: "Gliederung",
          exact: true,
        })
        await expect(button).toBeHidden()
      })
    },
  )

  test(
    "long texts are editable for internal user",
    {
      annotation: {
        type: "story",
        description:
          "https://digitalservicebund.atlassian.net/browse/RISDEV-4523",
      },
      tag: ["@RISDEV-4523"],
    },
    async ({ page, documentNumber }) => {
      const editable = 'contenteditable="true"'
      await test.step("Navigiere zu Rubriken als external Nutzer", async () => {
        await navigateToCategories(page, documentNumber)
      })

      await test.step("Tenor ist bearbeitbar", async () => {
        await clickCategoryButton("Tenor", page)
        const tenor = page.getByTestId("Tenor")
        const inputFieldInnerHTML = await tenor.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Tatbestand ist bearbeitbar", async () => {
        await clickCategoryButton("Tatbestand", page)
        const caseFacts = page.getByTestId("Tatbestand")
        const inputFieldInnerHTML = await caseFacts.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Entscheidungsgründe sind bearbeitbar", async () => {
        await clickCategoryButton("Entscheidungsgründe", page)
        const decisionReasons = page.getByTestId("Entscheidungsgründe")
        const inputFieldInnerHTML = await decisionReasons.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Gründe sind bearbeitbar", async () => {
        await clickCategoryButton("Gründe", page)
        const reasons = page.getByTestId("Gründe")
        const inputFieldInnerHTML = await reasons.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Sonstiger Langtext ist bearbeitbar", async () => {
        await clickCategoryButton("Sonstiger Langtext", page)
        const otherLongText = page.getByTestId("Sonstiger Langtext")
        const inputFieldInnerHTML = await otherLongText.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Gliederung ist bearbeitbar", async () => {
        await clickCategoryButton("Gliederung", page)
        const outline = page.getByTestId("Gliederung")
        const inputFieldInnerHTML = await outline.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Abweichende Meinung ist bearbeitbar", async () => {
        await clickCategoryButton("Abweichende Meinung", page)
        const dissentingOpinion = page.getByTestId("Abweichende Meinung")
        const inputFieldInnerHTML = await dissentingOpinion.innerHTML()
        expect(inputFieldInnerHTML).toContain(editable)
      })

      await test.step("Mitwirkende Richter ist bearbeitbar", async () => {
        await clickCategoryButton("Mitwirkende Richter", page)
        const nameInput = page.getByTestId("participating-judge-name")
        const referenceOpinionsInput = page.getByTestId(
          "participating-judge-reference-opinions",
        )

        await expect(nameInput).toBeEditable()
        await expect(referenceOpinionsInput).toBeEditable()
      })
    },
  )

  // expects are in nested functions, which eslint does not recognise
  // eslint-disable-next-line playwright/expect-expect
  test("Titelzeile should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    const testId = "Titelzeile"
    const value = "Titelzeile Test Text"
    const selector = `[data-testid='${testId}']`

    await runTextCategoryTest(
      page,
      prefilledDocumentUnit,
      testId,
      value,
      selector,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test("Leitsatz should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    secondPrefilledDocumentUnit,
  }) => {
    const testId = "Leitsatz"
    const value = "Leitsatz Test Text"
    const selector = `[data-testid='${testId}']`

    await runTextCategoryTest(
      page,
      secondPrefilledDocumentUnit,
      testId,
      value,
      selector,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test("Orientierungssatz should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    secondPrefilledDocumentUnit,
  }) => {
    const testId = "Orientierungssatz"
    const value = "Orientierungssatz Test Text"
    const selector = `[data-testid='${testId}']`

    await runTextCategoryTest(
      page,
      secondPrefilledDocumentUnit,
      testId,
      value,
      selector,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test(
    "Sonstiger Orientierungssatz should be saved and displayed in preview and in 'XML-Vorschau'",
    {
      tag: ["@RISDEV-4572"],
    },
    async ({ page, prefilledDocumentUnit }) => {
      const testId = "Sonstiger Orientierungssatz"
      const value = "Sonstiger Orientierungssatz Test Text"
      const selector = `[data-testid='${testId}']`

      await runTextCategoryTest(
        page,
        prefilledDocumentUnit,
        testId,
        value,
        selector,
      )
    },
  )

  // eslint-disable-next-line playwright/expect-expect
  test("Tenor should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    const testId = "Tenor"
    const value = "Tenor Test Text"
    const selector = `[data-testid='${testId}']`

    await runTextCategoryTest(
      page,
      prefilledDocumentUnit,
      testId,
      value,
      selector,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test("Gründe should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    const testId = "Gründe"
    const value = "Gründe Test Text"
    const selector = `[data-testid='${testId}']`

    await runTextCategoryTest(
      page,
      prefilledDocumentUnit,
      testId,
      value,
      selector,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test("Tatbestand should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    secondPrefilledDocumentUnit,
  }) => {
    const testId = "Tatbestand"
    const value = "Tatbestand Test Text"
    const selector = `[data-testid='${testId}']`

    await navigateToCategories(
      page,
      secondPrefilledDocumentUnit.documentNumber!,
    )
    await clickCategoryButton(testId, page)
    await fillTextField(testId, value, page, selector)

    //required for xml preview
    await clickCategoryButton("Entscheidungsgründe", page)
    await fillTextField(
      "Entscheidungsgründe",
      "Test",
      page,
      "[data-testid='Entscheidungsgründe']",
    )

    await save(page)
    await checkWasSaved(value, page, selector)
    await checkVisibleInPreview(
      testId,
      value,
      page,
      secondPrefilledDocumentUnit,
    )
    await checkVisibleInXMLPreview(
      testId,
      value,
      page,
      secondPrefilledDocumentUnit,
    )
  })

  // eslint-disable-next-line playwright/expect-expect
  test("Entscheidungsgründe should be saved and displayed in preview and in 'XML-Vorschau'", async ({
    page,
    prefilledDocumentUnit,
  }) => {
    const testId = "Entscheidungsgründe"
    const value = "Entscheidungsgründe Test Text"
    const selector = `[data-testid='${testId}']`

    await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
    await clickCategoryButton(testId, page)
    await fillTextField(testId, value, page, selector)

    //required for xml preview
    await clickCategoryButton("Tatbestand", page)
    await fillTextField(
      "Tatbestand",
      "Test",
      page,
      "[data-testid='Tatbestand']",
    )

    await save(page)
    await checkWasSaved(value, page, selector)
    await checkVisibleInPreview(testId, value, page, prefilledDocumentUnit)
    await checkVisibleInXMLPreview(testId, value, page, prefilledDocumentUnit)
  })

  // eslint-disable-next-line playwright/expect-expect
  test(
    "Abweichende Meinung should be saved and displayed in preview and in 'XML-Vorschau'",
    {
      tag: ["@RISDEV-4570"],
    },
    async ({ page, secondPrefilledDocumentUnit }) => {
      const testId = "Abweichende Meinung"
      const value = "Abweichende Meinung Test Text"
      const selector = `[data-testid='${testId}']`

      await runTextCategoryTest(
        page,
        secondPrefilledDocumentUnit,
        testId,
        value,
        selector,
      )
    },
  )

  // eslint-disable-next-line playwright/expect-expect
  test(
    "Sonstiger Langtext should be saved and displayed in preview and in 'XML-Vorschau'",
    {
      tag: ["@RISDEV-4573"],
    },
    async ({ page, prefilledDocumentUnit }) => {
      const testId = "Sonstiger Langtext"
      const value = "Sonstiger Langtext Test Text"
      const selector = `[data-testid='${testId}']`

      await runTextCategoryTest(
        page,
        prefilledDocumentUnit,
        testId,
        value,
        selector,
      )
    },
  )

  // eslint-disable-next-line playwright/expect-expect
  test(
    "Gliederung should be saved and displayed in preview and in 'XML-Vorschau'",
    {
      tag: ["@RISDEV-4576"],
    },
    async ({ page, prefilledDocumentUnit }) => {
      const testId = "Gliederung"
      const value = "Gliederung Test Text"
      const selector = `[data-testid='${testId}']`

      await runTextCategoryTest(
        page,
        prefilledDocumentUnit,
        testId,
        value,
        selector,
      )
    },
  )

  async function runTextCategoryTest(
    page: Page,
    prefilledDocumentUnit: DocumentUnit,
    testId: string,
    value: string,
    selector: string,
  ) {
    await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
    await clickCategoryButton(testId, page)
    await fillTextField(testId, value, page, selector)
    await save(page)
    await checkWasSaved(value, page, selector)
    await checkVisibleInPreview(testId, value, page, prefilledDocumentUnit)
    await checkVisibleInXMLPreview(testId, value, page, prefilledDocumentUnit)
  }

  async function fillTextField(
    testId: string,
    value: string,
    page: Page,
    selector: string,
  ) {
    await test.step(`text field '${testId}' should be filled with value '${value}'`, async () => {
      const textField = page.locator(selector)
      await textField.click()
      await page.keyboard.type(value)
      const innerText = await textField.innerText()
      expect(innerText).toContain(value)
    })
  }

  async function checkWasSaved(value: string, page: Page, selector: string) {
    await test.step(`value '${value}' should be saved (be present after reload)`, async () => {
      await page.reload()
      const textField = page.locator(selector)
      const innerText = await textField.innerText()
      expect(innerText).toContain(value)
    })
  }
  async function checkVisibleInPreview(
    testId: string,
    value: string,
    page: Page,
    prefilledDocumentUnit: DocumentUnit,
  ) {
    await test.step(`text field '${testId}' and value '${value}' should be visible in preview`, async () => {
      await navigateToPreview(
        page,
        prefilledDocumentUnit.documentNumber as string,
      )

      await expect(page.getByText(testId, { exact: true })).toBeVisible()
      await expect(
        page.getByText(value, {
          exact: true,
        }),
      ).toBeVisible()
    })
  }

  async function checkVisibleInXMLPreview(
    testId: string,
    value: string,
    page: Page,
    prefilledDocumentUnit: DocumentUnit,
  ) {
    await test.step(`text field '${testId}' and value '${value}' should be visible in 'XML-Vorschau'`, async () => {
      await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
      const xmlPreview = page.getByTitle("XML Vorschau")
      await expect(xmlPreview).toBeVisible()
      const button = page.getByRole("button", { name: "aufklappen" })
      await expect(button).toBeVisible()
      await button.click()
      const innerText = await xmlPreview.innerText()
      expect(innerText).toContain(value)
    })
  }
})

import { expect } from "@playwright/test"
import { navigateToCategories } from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"
import { navigateToHandover } from "~/e2e/caselaw/e2e-utils"

test.describe(
  "Blockquote (Zitierung)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4253",
    },
  },
  () => {
    test("Mark text as blockquote via button", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const blockquote = `<blockquote><p>Abschnitt</p></blockquote>`
      const inputField = page.locator("[data-testid='Gründe']")

      await test.step("open document categories", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
      })

      await test.step("add text to 'Gründe'", async () => {
        await inputField.click()
        await page.keyboard.type("Abschnitt")
      })

      await test.step("mark text as blockquote via button", async () => {
        await page.getByLabel("blockquote").click()
      })

      await test.step("check blockquote has been added to text", async () => {
        await page.getByLabel("invisible-characters").click()
        const inputFieldInnerHTML = await inputField.innerHTML()
        expect(inputFieldInnerHTML.includes(blockquote)).toBeTruthy()
      })

      await test.step("save document", async () => {
        await page.getByText("Speichern").click()
        await page.waitForEvent("requestfinished")
      })

      await test.step("navigate to 'XML Vorschau' in 'Übergabe an jDV'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await expect(page.getByText("XML Vorschau")).toBeVisible()
        await page.getByText("XML Vorschau").click()
      })

      await test.step("check if text is exported with blockquote", async () => {
        const exportedBlockquote = `<blockquote>34                        <p>Abschnitt</p>35                    </blockquote>`
        expect(await page.getByTestId("code-snippet").textContent()).toContain(
          exportedBlockquote,
        )
      })
    })

    test("Remove blockquote from text via button", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const blockquote = `<blockquote><p>Abschnitt</p></blockquote>`
      const noBlockquote = `<p>Abschnitt</p>`
      const inputField = page.locator("[data-testid='Gründe']")

      await test.step("open document categories", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
      })

      await test.step("add text to 'Gründe'", async () => {
        await inputField.click()
        await page.keyboard.type("Abschnitt")
      })

      await test.step("mark text as blockquote via button", async () => {
        await page.getByLabel("blockquote").click()
      })

      await test.step("remove blockquote from text via button", async () => {
        await page.getByLabel("blockquote").click()
      })

      await test.step("check blockquote has been removed from text", async () => {
        await page.getByLabel("invisible-characters").click()
        const inputFieldInnerHTML = await inputField.innerHTML()
        expect(inputFieldInnerHTML.includes(blockquote)).toBeFalsy()
        expect(inputFieldInnerHTML.includes(noBlockquote)).toBeTruthy()
      })

      await test.step("save document", async () => {
        await page.getByText("Speichern").click()
        await page.waitForEvent("requestfinished")
      })

      await test.step("navigate to 'XML Vorschau' in 'Übergabe an jDV'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await expect(page.getByText("XML Vorschau")).toBeVisible()
        await page.getByText("XML Vorschau").click()
      })

      await test.step("check if text is exported without blockquote", async () => {
        const exportedBlockquote = `<blockquote>34                        <p>Abschnitt</p>35                    </blockquote>`
        expect(
          await page.getByTestId("code-snippet").textContent(),
        ).not.toContain(exportedBlockquote)
        expect(await page.getByTestId("code-snippet").textContent()).toContain(
          `<p>Abschnitt</p>`,
        )
      })
    })

    test("Mark text as blockquote via keyboard shortcut", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const blockquote = `<blockquote><p>Abschnitt</p></blockquote>`
      const inputField = page.locator("[data-testid='Gründe']")

      await test.step("open document categories", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
      })

      await test.step("add text to 'Gründe'", async () => {
        await inputField.click()
        await page.keyboard.type("Abschnitt")
      })

      await test.step("mark text as blockquote via keyboard shortcut", async () => {
        // eslint-disable-next-line playwright/no-conditional-in-test
        const modifier = (await page.evaluate(() => navigator.platform))
          .toLowerCase()
          .includes("mac")
          ? "Meta"
          : "Control"
        await page.keyboard.press(`Shift+${modifier}+KeyB`)
      })

      await test.step("check blockquote has been added to text", async () => {
        await page.getByLabel("invisible-characters").click()
        const inputFieldInnerHTML = await inputField.innerHTML()
        expect(inputFieldInnerHTML.includes(blockquote)).toBeTruthy()
      })

      await test.step("save document", async () => {
        await page.getByText("Speichern").click()
        await page.waitForEvent("requestfinished")
      })

      await test.step("navigate to 'XML Vorschau' in 'Übergabe an jDV'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await expect(page.getByText("XML Vorschau")).toBeVisible()
        await page.getByText("XML Vorschau").click()
      })

      await test.step("check if text is exported with blockquote", async () => {
        const exportedBlockquote = `<blockquote>34                        <p>Abschnitt</p>35                    </blockquote>`
        expect(await page.getByTestId("code-snippet").textContent()).toContain(
          exportedBlockquote,
        )
      })
    })

    test("Remove blockquote from text via keyboard shortcut", async ({
      page,
      prefilledDocumentUnit,
    }) => {
      const blockquote = `<blockquote><p>Abschnitt</p></blockquote>`
      const noBlockquote = `<p>Abschnitt</p>`
      const inputField = page.locator("[data-testid='Gründe']")

      await test.step("open document categories", async () => {
        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
      })

      await test.step("add text to 'Gründe'", async () => {
        await inputField.click()
        await page.keyboard.type("Abschnitt")
      })

      await test.step("mark text as blockquote via keyboard shortcut", async () => {
        // eslint-disable-next-line playwright/no-conditional-in-test
        const modifier = (await page.evaluate(() => navigator.platform))
          .toLowerCase()
          .includes("mac")
          ? "Meta"
          : "Control"
        await page.keyboard.press(`Shift+${modifier}+KeyB`)
      })

      await test.step("remove blockquote from text via keyboard shortcut", async () => {
        // eslint-disable-next-line playwright/no-conditional-in-test
        const modifier = (await page.evaluate(() => navigator.platform))
          .toLowerCase()
          .includes("mac")
          ? "Meta"
          : "Control"
        await page.keyboard.press(`Shift+${modifier}+KeyB`)
      })

      await test.step("check blockquote has been removed from text", async () => {
        await page.getByLabel("invisible-characters").click()
        const inputFieldInnerHTML = await inputField.innerHTML()
        expect(inputFieldInnerHTML.includes(blockquote)).toBeFalsy()
        expect(inputFieldInnerHTML.includes(noBlockquote)).toBeTruthy()
      })

      await test.step("save document", async () => {
        await page.getByText("Speichern").click()
        await page.waitForEvent("requestfinished")
      })

      await test.step("navigate to 'XML Vorschau' in 'Übergabe an jDV'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await expect(page.getByText("XML Vorschau")).toBeVisible()
        await page.getByText("XML Vorschau").click()
      })

      await test.step("check if text is exported without blockquote", async () => {
        const exportedBlockquote = `<blockquote>34                        <p>Abschnitt</p>35                    </blockquote>`
        expect(
          await page.getByTestId("code-snippet").textContent(),
        ).not.toContain(exportedBlockquote)
        expect(await page.getByTestId("code-snippet").textContent()).toContain(
          `<p>Abschnitt</p>`,
        )
      })
    })
  },
)

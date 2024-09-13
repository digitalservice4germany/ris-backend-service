import { expect } from "@playwright/test"
import {
  copyPasteTextFromAttachmentIntoEditor,
  navigateToAttachments,
  navigateToCategories,
  save,
  uploadTestfile,
} from "../../e2e-utils"
import { caselawTest as test } from "../../fixtures"
import { navigateToHandover } from "~/e2e/caselaw/e2e-utils"

test.describe(
  "transform special hyphen characters correctly",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4111",
    },
  },
  () => {
    const testCases = [
      {
        name: "BFH",
        fileName: "special_hyphens_bfh.docx",
        textWithSpecialHyphen:
          "Urteil des Bundesfinanzhofs ‑‑BFH‑‑ vom 14.06.2017",
      },
      {
        name: "BGH",
        fileName: "special_hyphens_bgh.docx",
        textWithSpecialHyphen:
          "Kinder - abhängig vom Dienstplan des Vaters - an",
      },
    ]

    testCases.forEach(({ name, fileName, textWithSpecialHyphen }) => {
      test(`copy-paste text with special hyphens from a ${name} docx`, async ({
        page,
        prefilledDocumentUnit,
      }) => {
        await test.step(`upload document with ${name} special characters and verify hyphens are shown in attachments`, async () => {
          await navigateToAttachments(
            page,
            prefilledDocumentUnit.documentNumber!,
          )
          await uploadTestfile(page, fileName)
          await expect(page.getByText(fileName)).toBeVisible()
          await expect(page.getByLabel("Datei löschen")).toBeVisible()
          await expect(page.getByText(textWithSpecialHyphen)).toBeVisible()
        })

        await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)
        await page.getByLabel("Navigation schließen").click()
        const editor = page.locator("[data-testid='Gründe']")

        await test.step("copy text with special hyphen from side panel into reasons", async () => {
          const attachmentLocator = page
            .getByText(textWithSpecialHyphen)
            .locator("..")
          await copyPasteTextFromAttachmentIntoEditor(
            page,
            attachmentLocator,
            editor,
          )
        })

        await test.step("check if special hyphens are visible in editor", async () => {
          const inputFieldAllText = await editor.allTextContents()
          expect(inputFieldAllText[0]).toContain(textWithSpecialHyphen)
        })

        await save(page)

        await test.step("check if special hyphens are visible in jDV export", async () => {
          await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
          await expect(page.getByText("XML Vorschau")).toBeVisible()
          await page.getByText("XML Vorschau").click()
          await expect(page.getByText(textWithSpecialHyphen)).toBeVisible()
        })
      })
    })
  },
)

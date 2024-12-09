import { expect } from "@playwright/test"
import dayjs from "dayjs"
import {
  navigateToAttachments,
  navigateToCategories,
  uploadTestfile,
  waitForInputValue,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "metadata is extracted from uploaded docx files",
  {
    annotation: {
      type: "epic",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4423",
    },
  },
  () => {
    test(
      "upload file with metadata into empty document unit should add all properties",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4423",
        },
      },
      async ({ page, documentNumber }) => {
        await navigateToAttachments(page, documentNumber)

        await test.step("upload file with Aktenzeichen, Rechtskraft, Gericht and Spruchkoerper metadata", async () => {
          await uploadTestfile(page, "with_metadata.docx")
          await expect(page.getByText("Hochgeladen am")).toBeVisible()
        })

        await test.step("open preview in sidepanel and check if metadata is filled", async () => {
          await page.getByLabel("Vorschau anzeigen").click()
          await expect(page.getByText("AktenzeichenII B 29/24")).toBeVisible()
          await expect(page.getByText("RechtskraftJa")).toBeVisible()
          await expect(
            page.getByText("GerichtAG Berlin-Pankow", { exact: true }),
          ).toBeVisible()
          await expect(page.getByText("Spruchkörper2. Senat")).toBeVisible()
          await expect(
            page.getByText("Entscheidungsdatum01.11.2011"),
          ).toBeVisible()
          await expect(
            page.getByText("ECLIECLI:DE:BGH:2023:210423UVZR86.22.0"),
          ).toBeVisible()
          await expect(page.getByText("DokumenttypUrteil")).toBeVisible()
        })

        // procedure is only displayed in categories
        await test.step("open categories and check if procedure is filled", async () => {
          await navigateToCategories(page, documentNumber)
          await waitForInputValue(
            page,
            "[aria-label='Vorgang']",
            "metadata-vorgang",
          )
        })
      },
    )

    test(
      "upload file with metadata into prefilled document unit should add unset properties",
      {
        annotation: {
          type: "story",
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4423",
        },
      },
      async ({ page, prefilledDocumentUnit }) => {
        await navigateToAttachments(
          page,
          prefilledDocumentUnit.documentNumber || "",
        )

        await test.step("upload file with all metadata", async () => {
          await uploadTestfile(page, "with_metadata.docx")
          await expect(page.getByText("Hochgeladen am")).toBeVisible()
        })

        await test.step("check in preview if previously empty fields ECLI and legal effect have been added", async () => {
          await page.getByLabel("Vorschau anzeigen").click()
          await expect(
            page.getByText("ECLIECLI:DE:BGH:2023:210423UVZR86.22.0"),
          ).toBeVisible()
          await expect(page.getByText("RechtskraftJa")).toBeVisible()
        })

        await test.step("check in preview if all preexisting fields are unchanged", async () => {
          await expect(
            page.getByText(
              "Aktenzeichen" +
                prefilledDocumentUnit.coreData.fileNumbers?.at(0),
            ),
          ).toBeVisible()
          await expect(
            page.getByText(
              "Rechtskraft" + prefilledDocumentUnit.coreData.legalEffect,
            ),
          ).toBeHidden()
          await expect(
            page.getByText(
              "Gericht" + prefilledDocumentUnit.coreData.court?.label,
            ),
          ).toBeVisible()
          await expect(
            page.getByText(
              "Spruchkörper" + prefilledDocumentUnit.coreData.appraisalBody,
            ),
          ).toBeVisible()
          await expect(
            page.getByText(
              "Entscheidungsdatum" +
                dayjs(prefilledDocumentUnit.coreData.decisionDate).format(
                  "DD.MM.YYYY",
                ),
            ),
          ).toBeVisible()
          await expect(
            page.getByText(
              "Dokumenttyp" +
                prefilledDocumentUnit.coreData.documentType?.label,
            ),
          ).toBeVisible()
        })
      },
    )
  },
)

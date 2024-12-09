import { expect } from "@playwright/test"
import dayjs from "dayjs"
import {
  navigateToPeriodicalHandover,
  navigateToPeriodicalReferences,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

const formattedDate = dayjs().format("YYYY-MM-DD")

test.describe(
  "hand over edition to jdv",
  {
    annotation: {
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4557",
      type: "epic",
    },
    tag: ["@RISDEV-4557"],
  },
  () => {
    test(
      "periodical evaluation can be handed over",
      {
        annotation: {
          description:
            "https://digitalservicebund.atlassian.net/browse/RISDEV-4709",
          type: "story",
        },
        tag: ["@RISDEV-4709"],
      },
      async ({ page, edition, editionWithReferences }) => {
        await test.step("navigate to periodical references", async () => {
          await navigateToPeriodicalReferences(page, edition.id ?? "")

          await expect(
            page.getByRole("link", { name: "Übergabe an jDV" }),
          ).toBeVisible()

          await page.getByRole("link", { name: "Übergabe an jDV" }).click()
          await expect(page.getByTestId("handover-title")).toBeVisible()
        })

        await test.step("handover not possible if references are empty", async () => {
          await expect(
            page.getByText("Es wurden noch keine Fundstellen hinzugefügt"),
          ).toBeVisible()
          await expect(
            page.getByLabel("Fundstellen der Ausgabe an jDV übergeben"),
          ).toBeDisabled()
        })

        await test.step("handover possible when all required fields filled", async () => {
          await navigateToPeriodicalHandover(
            page,
            editionWithReferences.id ?? "",
          )

          await expect(
            page.getByText("Die Ausgabe enthält 2 Fundstellen"),
          ).toBeVisible()

          await expect(page.getByText("XML Vorschau")).toBeVisible()

          await page.getByText("XML Vorschau").click()

          await expect(
            page.locator(
              "text='            <zitstelle>2024, 1-11, Heft 1</zitstelle>'",
            ),
          ).toBeVisible()

          await expect(
            page.locator(
              "text='            <zitstelle>2024, 12-22, Heft 1 (L)</zitstelle>'",
            ),
          ).toBeVisible()

          await page
            .locator("[aria-label='Fundstellen der Ausgabe an jDV übergeben']")
            .click()

          await expect(page.getByText("Email wurde versendet")).toBeVisible()

          await expect(page.getByText("Xml Email Abgabe -")).toBeVisible()

          await expect(
            page.getByText(
              "id=juris name=invalid-user da=R df=X dt=F mod=T ld=" +
                formattedDate +
                " vg=edition-" +
                editionWithReferences.id,
            ),
          ).toBeVisible()

          await expect(
            page.getByText(
              editionWithReferences.references?.[0]?.documentationUnit
                ?.documentNumber + ".XML",
            ),
          ).toBeVisible()
          await expect(
            page.getByText(
              editionWithReferences.references?.[1]?.documentationUnit
                ?.documentNumber + ".XML",
            ),
          ).toBeVisible()
        })
      },
    )
  },
)

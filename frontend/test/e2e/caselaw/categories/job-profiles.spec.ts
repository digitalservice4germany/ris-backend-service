import { expect, Page } from "@playwright/test"
import {
  clickCategoryButton,
  navigateToCategories,
  navigateToHandover,
  save,
} from "../e2e-utils"
import { caselawTest as test } from "../fixtures"

test.describe(
  "job profiles (Berufsbild)",
  {
    annotation: {
      type: "story",
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-4571",
    },
    tag: ["@RISDEV-4571"],
  },
  () => {
    const firstJobProfile = "Handwerker"
    const secondJobProfile = "Elektriker"

    test("rendering logic", async ({ page, prefilledDocumentUnit }) => {
      await navigateToCategories(page, prefilledDocumentUnit.documentNumber!)

      await test.step("check button is displayed when field is empty", async () => {
        await expect(
          page.getByRole("button", { name: "Berufsbild" }),
        ).toBeVisible()
      })
      await clickCategoryButton("Berufsbild", page)

      await addFirstJobProfile(page)

      await test.step("reload page and check that input field and content are visible", async () => {
        await page.reload()
        await expect(page.getByTestId("Berufsbild")).toBeVisible()
        await expect(page.getByText(firstJobProfile)).toBeVisible()
      })

      await removeFirstJobProfile(page)

      await save(page)

      await test.step("reload page and check that button is visible", async () => {
        await page.reload()

        await expect(
          page.getByRole("button", { name: "Berufsbild" }),
        ).toBeVisible()
      })
      await clickCategoryButton("Berufsbild", page)
      await addFirstJobProfile(page)

      await test.step("XML preview should display 'Berufsbild' in 'paratrubriken'", async () => {
        await navigateToHandover(page, prefilledDocumentUnit.documentNumber!)
        await page.getByText("XML Vorschau").click()
        const xmlPreview = page.getByTitle("XML Vorschau")
        const innerText = await xmlPreview.innerText()
        expect(innerText).toContain(
          "<paratrubriken>\n" +
            "14\n" +
            "        <zuordnung>\n" +
            "15\n" +
            "            <aspekt>Berufsbild</aspekt>\n" +
            "16\n" +
            "            <begriff>Handwerker</begriff>\n" +
            "17\n" +
            "        </zuordnung>",
        )
      })
    })

    test("add multiple job profiles", async ({ page, documentNumber }) => {
      await navigateToCategories(page, documentNumber)
      await clickCategoryButton("Berufsbild", page)

      await addTwoJobProfiles(page)

      await test.step("reload page and check that both job profiles are visible", async () => {
        await page.reload()
        await expect(page.getByTestId("Berufsbild")).toBeVisible()
        await expect(page.getByText(firstJobProfile)).toBeVisible()
        await expect(page.getByText(secondJobProfile)).toBeVisible()
      })
    })

    test("delete job profile", async ({ page, documentNumber }) => {
      await navigateToCategories(page, documentNumber)
      await clickCategoryButton("Berufsbild", page)
      await addTwoJobProfiles(page)

      await test.step("delete first entry", async () => {
        await page
          .locator("[data-testid='chip']", { hasText: firstJobProfile })
          .getByLabel("Löschen")
          .click()
      })

      await save(page)

      await test.step("reload page and check that input field and content are correct", async () => {
        await page.reload()
        await expect(page.getByTestId("Berufsbild")).toBeVisible()
        await expect(page.getByText(firstJobProfile)).toBeHidden()
        await expect(page.getByText(secondJobProfile)).toBeVisible()
      })
    })

    async function addTwoJobProfiles(page: Page) {
      await addFirstJobProfile(page)

      await test.step("Enter second job profile", async () => {
        await page.locator("[aria-label='Berufsbild']").fill(secondJobProfile)
        await page.keyboard.press("Enter")
      })

      await save(page)
    }

    async function addFirstJobProfile(page: Page) {
      await test.step("enter job profile", async () => {
        await page.locator("[aria-label='Berufsbild']").fill(firstJobProfile)
        await page.keyboard.press("Enter")
        await expect(page.getByText(firstJobProfile)).toBeVisible()
        await save(page)
      })
    }

    async function removeFirstJobProfile(page: Page) {
      await test.step("remove job profile", async () => {
        await page
          .locator("[data-testid='chip']", { hasText: firstJobProfile })
          .getByLabel("Löschen")
          .click()

        await expect(page.getByText(firstJobProfile)).toBeHidden()
      })
    }
  },
)

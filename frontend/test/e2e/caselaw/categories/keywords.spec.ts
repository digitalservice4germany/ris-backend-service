import { expect } from "@playwright/test"
import { navigateToCategories } from "../e2e-utils"
import { caselawTest as test } from "../fixtures"
import { generateString } from "~/test-helper/dataGenerators"

test.describe("keywords", () => {
  test("renders category wrapper button when no keywords", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)

    const button = page
      .getByTestId("category-wrapper-button")
      .getByText(/Schlagwörter/)
    await expect(button).toBeVisible()
    await button.click()
    await expect(
      page.getByRole("heading", { name: "Schlagwörter" }),
    ).toBeVisible()
  })

  test("add keywords", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const button = page
      .getByTestId("category-wrapper-button")
      .getByText(/Schlagwörter/)
    await expect(button).toBeVisible()
    await button.click()

    const firstKeyword = generateString()
    const keywordWithSpecialCharacters = generateString() + "%&/"

    await page.getByLabel("Schlagwörter Input").type(firstKeyword)
    await page.keyboard.press("Enter")
    await page
      .getByLabel("Schlagwörter Input")
      .type(keywordWithSpecialCharacters)
    await page.keyboard.press("Enter")

    await page.getByLabel("Schlagwörter übernehmen").click()
    // Entered display mode
    await expect(page.getByLabel("Schlagwörter bearbeiten")).toBeVisible()
    await expect(page.getByTestId("chip")).toHaveCount(2)
    await expect(page.getByText(firstKeyword)).toBeVisible()
    await expect(page.getByText(keywordWithSpecialCharacters)).toBeVisible()
  })

  test("add same keyword not working", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const button = page
      .getByTestId("category-wrapper-button")
      .getByText(/Schlagwörter/)
    await expect(button).toBeVisible()
    await button.click()

    const firstKeyword = generateString()
    const secondKeyword = generateString()
    await page.getByLabel("Schlagwörter Input").type(firstKeyword)
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter Input").type(secondKeyword)
    await page.keyboard.press("Enter")
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter Input").type(secondKeyword)
    await page.keyboard.press("Enter")

    await page.getByLabel("Schlagwörter übernehmen").click()
    // Entered display mode
    await expect(page.getByLabel("Schlagwörter bearbeiten")).toBeVisible()
    await expect(page.getByTestId("chip")).toHaveCount(2)
    await expect(page.getByText(firstKeyword)).toBeVisible()
    await expect(page.getByText(secondKeyword)).toBeVisible()
  })

  test("sort keywords in chronological order", async ({
    page,
    documentNumber,
  }) => {
    await navigateToCategories(page, documentNumber)
    const button = page
      .getByTestId("category-wrapper-button")
      .getByText(/Schlagwörter/)
    await expect(button).toBeVisible()
    await button.click()

    await page.getByLabel("Schlagwörter Input").type("c")
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter Input").type("b")
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter Input").type("a")
    await page.keyboard.press("Enter")

    await expect(page.getByLabel("Schlagwörter Input")).toHaveValue(`c\nb\na\n`)
    await page.getByLabel("Alphabetisch sortieren").click()
    await page.getByLabel("Schlagwörter übernehmen").click()

    // Entered display mode
    await expect(page.getByLabel("Schlagwörter bearbeiten")).toBeVisible()
    const chips = await page.getByTestId("chip").all()
    await expect(page.getByTestId("chip")).toHaveCount(3)
    await expect(chips[0].getByText("a")).toBeVisible()
    await expect(chips[1].getByText("b")).toBeVisible()
    await expect(chips[2].getByText("c")).toBeVisible()

    // Entered edit mode
    await page.getByLabel("Schlagwörter bearbeiten").click()
    await expect(page.getByLabel("Alphabetisch sortieren")).not.toBeChecked()
    await expect(page.getByLabel("Schlagwörter Input")).toHaveValue(`a\nb\nc`)
  })

  test("delete keywords", async ({ page, documentNumber }) => {
    await navigateToCategories(page, documentNumber)
    const button = page
      .getByTestId("category-wrapper-button")
      .getByText(/Schlagwörter/)
    await expect(button).toBeVisible()
    await button.click()

    const firstKeyword = generateString()
    const secondKeyword = generateString()

    await page.getByLabel("Schlagwörter Input").type(firstKeyword)
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter Input").type(secondKeyword)
    await page.keyboard.press("Enter")
    await page.getByLabel("Schlagwörter übernehmen").click()

    // Entered display mode
    await expect(page.getByLabel("Schlagwörter bearbeiten")).toBeVisible()
    await expect(page.getByTestId("chip")).toHaveCount(2)
    await expect(page.getByText(firstKeyword)).toBeVisible()
    await expect(page.getByText(secondKeyword)).toBeVisible()

    // Entered edit mode
    await page.getByLabel("Schlagwörter bearbeiten").click()
    await page.getByLabel("Schlagwörter Input").clear()
    await page.getByLabel("Schlagwörter übernehmen").click()

    // When no data, we stay in edit mode
    await expect(page.getByLabel("Schlagwörter bearbeiten")).toBeHidden()

    // On page reload, category wrapper button is visible again (because no data)
    await page.reload()
    await expect(
      page.getByTestId("category-wrapper-button").getByText(/Schlagwörter/),
    ).toBeVisible()
  })
})

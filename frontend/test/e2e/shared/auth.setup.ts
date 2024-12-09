import { expect, test as setup } from "@playwright/test"

function authenticateUser(user: {
  name: string
  email: string
  password: string
}) {
  setup(`authenticate ${user.name}`, async ({ page }) => {
    await page.goto("/")
    await page.getByLabel("E-Mailadresse").fill(user.email)
    await page.getByLabel("Passwort", { exact: true }).fill(user.password)
    await page.locator("input#kc-login").click()

    await page.goto(process.env.E2E_BASE_URL ?? "http://127.0.0.1")
    await expect(page.getByText("Übersicht Rechtsprechung")).toBeVisible()

    await page
      .context()
      .storageState({ path: `test/e2e/shared/.auth/${user.name}.json` })
  })
}

;[
  {
    name: "user",
    email: process.env.E2E_TEST_USER as string,
    password: process.env.E2E_TEST_PASSWORD as string,
  },
  {
    name: "user_bgh",
    email: process.env.E2E_TEST_USER_BGH as string,
    password: process.env.E2E_TEST_PASSWORD_BGH as string,
  },
  {
    name: "user_external",
    email: process.env.E2E_TEST_USER_EXTERNAL as string,
    password: process.env.E2E_TEST_PASSWORD_EXTERNAL as string,
  },
].forEach(authenticateUser)

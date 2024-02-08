import { devices, PlaywrightTestConfig } from "@playwright/test"

const config: PlaywrightTestConfig = {
  testDir: "./test/e2e",
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 4 : undefined,
  fullyParallel: true,
  timeout: 120000,
  expect: { timeout: 5000 },
  use: {
    viewport: { width: 1280, height: 720 },
    acceptDownloads: true,
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1",
    screenshot: { mode: "only-on-failure", fullPage: true },
    timezoneId: "Europe/Berlin",
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "setup",
      testMatch: /.*\.setup\.ts/,
    },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        storageState: "test/e2e/.auth/user.json",
      },
      dependencies: ["setup"],
    },
    {
      name: "firefox",
      use: {
        ...devices["Desktop Firefox"],
        storageState: "test/e2e/.auth/user.json",
      },
      dependencies: ["setup"],
    },
    {
      name: "a11y",
      testDir: "./test/a11y",
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        storageState: "test/e2e/.auth/user.json",
      },
      dependencies: ["setup"],
    },
    {
      name: "queries",
      testDir: "./test/queries",
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        storageState: "test/e2e/.auth/user.json",
      },
      dependencies: ["setup"],
    },
  ],
}

export default config

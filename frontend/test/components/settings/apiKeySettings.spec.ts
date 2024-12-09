import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ApiKeySettings from "@/components/settings/ApiKeySettings.vue"
import authService from "@/services/authService"

describe("api key settings", () => {
  test("displays api key", async () => {
    vi.spyOn(authService, "getImportApiKey").mockResolvedValue({
      status: 200,
      data: {
        apiKey: "fooKey",
        validUntil: new Date("2032-12-30T01:23:45"),
        valid: true,
      },
    })

    render(ApiKeySettings)
    expect(screen.getByText("API Key")).toBeInTheDocument()
    await screen.findByText("fooKey")
    expect(
      screen.getByText("gültig bis: 30.12.2032 01:23:45"),
    ).toBeInTheDocument()
  })

  test("copies api key", async () => {
    vi.spyOn(authService, "getImportApiKey").mockResolvedValue({
      status: 200,
      data: { apiKey: "fooKey", validUntil: new Date(), valid: true },
    })

    render(ApiKeySettings)
    expect(screen.getByText("API Key")).toBeInTheDocument()
    await screen.findByText("fooKey")
    const clickableLabel = await screen.findByLabelText(
      "API Key in die Zwischenablage kopieren",
    )

    const user = userEvent.setup()
    await user.click(clickableLabel)

    const clipboardText = await navigator.clipboard.readText()
    expect(clipboardText).toBe("fooKey")
  })

  test("creates api key", async () => {
    vi.spyOn(authService, "generateImportApiKey").mockResolvedValue({
      status: 200,
      data: { apiKey: "fooKey", validUntil: new Date(), valid: true },
    })

    render(ApiKeySettings)
    expect(screen.getByText("API Key")).toBeInTheDocument()
    expect(screen.queryByText("fooKey")).not.toBeInTheDocument()
    const createButton = await screen.findByRole("button", {
      name: "Neuen API-Schlüssel erstellen",
    })
    expect(createButton).toBeInTheDocument()
    createButton.click()
    await screen.findByText("fooKey")
  })

  test("invalidate api key", async () => {
    vi.spyOn(authService, "getImportApiKey").mockResolvedValue({
      status: 200,
      data: { apiKey: "fooKey", validUntil: new Date(), valid: true },
    })
    vi.spyOn(authService, "invalidateImportApiKey").mockResolvedValue({
      status: 200,
      data: { apiKey: "fooKey", validUntil: new Date(), valid: false },
    })

    render(ApiKeySettings)
    await screen.findByText("fooKey")
    const invalidateButton = await screen.findByRole("button", {
      name: "Sperren",
    })
    invalidateButton.click()
    await screen.findByText("API-Key ist abgelaufen!")
    expect(
      screen.getByText("Neuen API-Schlüssel erstellen"),
    ).toBeInTheDocument()
  })
})

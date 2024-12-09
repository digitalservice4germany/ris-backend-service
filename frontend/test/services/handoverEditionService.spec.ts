import { HandoverMail } from "@/domain/eventRecord"
import service from "@/services/handoverEditionService"

describe("handoverDocumentationUnitService", () => {
  it("returns error message if event report contains error but status is success", async () => {
    vi.mock("@/services/httpClient", () => {
      const testXml: HandoverMail = new HandoverMail({
        success: false,
        date: "2021-01-01",
      })
      return {
        default: {
          put: vi.fn().mockReturnValue({ status: 422, data: testXml }),
        },
      }
    })

    const result = await service.handoverEdition("123")
    expect(result.error?.title).toEqual("Leider ist ein Fehler aufgetreten.")
    expect(result.error?.description).toContain(
      "Die Ausgabe kann nicht übergeben werden.",
    )
  })

  it("returns correct error message if preview contains error", async () => {
    vi.mock("@/services/httpClient", () => {
      return {
        default: {
          get: vi.fn().mockReturnValue({ status: 400, data: [{}] }),
          put: vi.fn().mockReturnValue({ status: 400, data: [{}] }),
        },
      }
    })

    const result = await service.getPreview("123")
    expect(result.error?.title).toEqual("Fehler beim Laden der XML-Vorschau")
  })

  it("returns correct error message if preview contains XML error", async () => {
    vi.mock("@/services/httpClient", () => {
      return {
        default: {
          get: vi.fn().mockReturnValue({
            status: 200,
            data: [
              {
                success: false,
                statusMessages: ["Fehler 1", "Fehler 2"],
              },
            ],
          }),
          put: vi.fn().mockReturnValue({
            status: 200,
            data: [
              {
                success: false,
                statusMessages: ["Fehler 1", "Fehler 2"],
              },
            ],
          }),
        },
      }
    })

    const result = await service.getPreview("123")
    expect(result.error?.title).toEqual("Fehler beim Laden der XML-Vorschau")
    expect(result.error?.description).toContain("Fehler 2")
  })
})

import { describe, test } from "vitest"
import { Metadata, UndefinedDate } from "@/domain/Norm"
import { generalSummarizer } from "@/helpers/generalSummarizer"

describe("Entry into force Summarizer", () => {
  test("summarizes the entry into force Date", () => {
    const data: [Metadata, string][] = [
      [
        {
          DATE: ["01.01.2020"],
        },
        "01.01.2020",
      ],
    ]

    data.forEach(([input, expected]) => {
      expect(generalSummarizer(input)).toBe(expected)
    })
  })

  test("summarizes the entry into force Undefined date", () => {
    const data: [Metadata, string][] = [
      [
        {
          UNDEFINED_DATE: [UndefinedDate.UNDEFINED_FUTURE],
        },
        "unbestimmt (zukünftig)",
      ],
      [
        {
          UNDEFINED_DATE: [UndefinedDate.UNDEFINED_NOT_PRESENT],
        },
        "nicht vorhanden",
      ],
      [
        {
          UNDEFINED_DATE: [UndefinedDate.UNDEFINED_UNKNOWN],
        },
        "unbestimmt (unbekannt)",
      ],
    ]

    data.forEach(([input, expected]) => {
      expect(generalSummarizer(input)).toBe(expected)
    })
  })

  test("fails gracefully when no data is provided", () => {
    // @ts-expect-error Not allowed by TypeScript but just to make sure
    expect(generalSummarizer(undefined)).toBe("")
  })

  test("fails gracefully when trying to summarize unsupported data", () => {
    expect(generalSummarizer({ LEAD_JURISDICTION: [] })).toBe("")
  })
})

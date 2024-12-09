import { expect, test, Page, TestInfo } from "@playwright/test"
import DocumentUnit from "../../src/domain/documentUnit"
import { DocumentUnitSearchParameter } from "@/components/DocumentUnitSearchEntryForm.vue"
import { getRequest } from "~/e2e/caselaw/e2e-utils"

// This is a performance test for the backend search endpoint
// We run it sequentially not to skew the results
test.describe("document unit search queries", () => {
  const testConfigurations: {
    title: string
    parameter: { [K in DocumentUnitSearchParameter]?: string }
    maxDuration: number
    minResults?: number
  }[] = [
    {
      title: "documentNumber and courtType",
      parameter: {
        documentNumber: "BVRE",
        courtType: "VerfGH",
      },
      maxDuration: 500, // last max 776, average 365, min 259
      minResults: 5,
    },
    {
      title: "vague documentNumber",
      parameter: {
        documentNumber: "BV",
      },
      maxDuration: 3800, // last max 3516, average 2140, min 1505
      minResults: 5,
    },
    {
      title: "not existing documentNumber",
      parameter: {
        documentNumber: "notExistingFoo",
      },
      maxDuration: 400, // last max 488, average 231, min 166
    },
    {
      title: "vague fileNumber",
      parameter: {
        fileNumber: "Bv",
      },
      maxDuration: 2000, // last max 3546, average 1003, min 352
      minResults: 5,
    },
    {
      title: "not existing fileNumber",
      parameter: {
        fileNumber: "notExistingFoo",
      },
      maxDuration: 400, // last max 477, average 229, min 166
    },
    {
      title: "only unpublished",
      parameter: {
        publicationStatus: "UNPUBLISHED",
      },
      maxDuration: 1300, // last max 1400, average 1151, min 1075
      minResults: 5,
    },
    {
      title: "of all time",
      parameter: {
        decisionDate: "1900-01-01",
        decisionDateEnd: "2024-01-15",
      },
      maxDuration: 500, // last max 590, average 336, min 270
      minResults: 5,
    },
    {
      title: "one day",
      parameter: {
        decisionDate: "1975-06-16",
      },
      maxDuration: 350, // last max 249, average 243, min 179
      minResults: 1,
    },
    {
      title: "only court location",
      parameter: {
        courtLocation: "München",
      },
      maxDuration: 1600, // last max 1548, average 1306, min 1236
      minResults: 5,
    },
    {
      title: "only court type",
      parameter: {
        courtType: "VerfGH",
      },
      maxDuration: 500, // last max 609, average 356, min 289
      minResults: 5,
    },
    {
      title: "only my doc office",
      parameter: {
        myDocOfficeOnly: "true",
      },
      maxDuration: 450, // last max 556, average 304, min 241
      minResults: 5,
    },
    {
      title: "scheduled only",
      parameter: {
        myDocOfficeOnly: "true",
        scheduledOnly: "true",
      },
      maxDuration: 500,
      minResults: 3,
    },
    {
      title: "publication date",
      parameter: {
        myDocOfficeOnly: "true",
        publicationDate: "2100-11-21",
      },
      maxDuration: 500,
      minResults: 3,
    },
  ]

  testConfigurations.forEach((search) =>
    test(search.title, async ({ page }, testInfo) =>
      runTestMultipleTimes(10, search, page, testInfo),
    ),
  )
})

async function runTestMultipleTimes(
  runs: number,
  search: {
    title: string
    parameter: { [K in DocumentUnitSearchParameter]?: string }
    maxDuration: number
    minResults?: number
  },
  page: Page,
  testInfo: TestInfo,
  durations: number[] = [],
) {
  if (runs === 0) {
    await testInfo.attach("durations", {
      body: Buffer.from(JSON.stringify(durations)),
      contentType: "application/json",
    })
    await testInfo.attach("maxDuration", {
      body: search.maxDuration.toString(),
      contentType: "application/text",
    })
    const meanDuration = durations.reduce((a, b) => a + b, 0) / durations.length
    expect(meanDuration).toBeLessThan(search.maxDuration)
    return
  }

  const url =
    "/api/v1/caselaw/documentunits/search?pg=0&sz=100" +
    getUrlParams(search.parameter)
  const request = await getRequest(url, page)

  const duration = request.timing().responseStart
  expect(duration).not.toBe(-1)
  if (search.minResults) {
    const documentUnits =
      ((await (await request.response())?.json())?.content as DocumentUnit[]) ||
      []
    expect(documentUnits.length).toBeGreaterThanOrEqual(search.minResults)
  }

  await runTestMultipleTimes(runs - 1, search, page, testInfo, [
    ...durations,
    duration,
  ])
}

function getUrlParams(parameter: {
  [K in DocumentUnitSearchParameter]?: string
}): string {
  return (
    parameter &&
    "&" +
      Object.entries(parameter)
        .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
        .join("&")
  )
}

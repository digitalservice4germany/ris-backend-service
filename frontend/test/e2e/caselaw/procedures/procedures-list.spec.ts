import { expect, BrowserContext, APIRequestContext } from "@playwright/test"
import {
  assignProcedureToDocUnit,
  deleteAllProcedures,
  navigateToProcedures,
} from "../e2e-utils"

import { caselawTest as test } from "../fixtures"
import { generateString } from "~/test-helper/dataGenerators"

// Because of the procedures performance problem, the requests might take more than 10s
const timeout = 15_000

test.describe("Procedures search list (Vorgänge)", () => {
  // If tests run in parallel, we do not want to delete other procedures -> random prefix
  const procedurePrefix = `test_${generateString({ length: 10 })}`
  const createdDocUnitUuids: string[] = []

  test("Search for procedures by name", async ({
    page,
    prefilledDocumentUnit,
    secondPrefilledDocumentUnit,
    documentNumber,
  }) => {
    const testcasePrefix = procedurePrefix + generateString({ length: 5 })
    const procedureName1 = await assignProcedureToDocUnit(
      page,
      documentNumber,
      testcasePrefix,
    )
    const procedureName2 = await assignProcedureToDocUnit(
      page,
      prefilledDocumentUnit.documentNumber!,
      testcasePrefix,
    )
    const procedureName3 = await assignProcedureToDocUnit(
      page,
      secondPrefilledDocumentUnit.documentNumber!,
      testcasePrefix,
    )

    await navigateToProcedures(page)

    await test.step("Initially, without a search query all procedures are visible", async () => {
      await expect(
        page.getByLabel("Vorgang Listenelement").first(),
      ).toBeVisible({ timeout })
      // There might be other pre-existing procedures
      expect(
        await page.getByLabel("Vorgang Listenelement").count(),
      ).toBeGreaterThanOrEqual(3)
    })

    await test.step("Search for all 3 newly created procedures", async () => {
      await page.getByPlaceholder("Nach Vorgängen suchen").fill(testcasePrefix)

      await expect(page.getByLabel("Vorgang Listenelement")).toHaveCount(3, {
        timeout,
      })
      await expect(page.getByText(procedureName1)).toBeVisible()
      await expect(page.getByText(procedureName2)).toBeVisible()
      await expect(page.getByText(procedureName3)).toBeVisible()
      await expect(page.getByText("3 Ergebnisse gefunden")).toBeVisible()
    })

    await test.step("Search for one specific procedure", async () => {
      await page.getByPlaceholder("Nach Vorgängen suchen").fill(procedureName1)

      await expect(page.getByLabel("Vorgang Listenelement")).toHaveCount(1, {
        timeout,
      })
      await expect(page.getByText("1 Ergebnis gefunden")).toBeVisible()
      await expect(page.getByText(procedureName1)).toBeVisible()
    })

    await test.step("Search with non-existing name", async () => {
      await page
        .getByPlaceholder("Nach Vorgängen suchen")
        .fill(procedureName1 + "Random")

      await expect(page.getByLabel("Vorgang Listenelement")).toBeHidden()
    })
  })

  test("Load doc unit of a procedure and delete it", async ({
    page,
    context,
    request,
  }) => {
    const { documentNumber, uuid } = await createNewDocUnit(context, request)
    createdDocUnitUuids.push(uuid)
    const procedureName = await assignProcedureToDocUnit(
      page,
      documentNumber,
      procedurePrefix,
    )

    await test.step("Search for procedure", async () => {
      await navigateToProcedures(page)
      await page.getByPlaceholder("Nach Vorgängen suchen").fill(procedureName)
      await expect(page.getByLabel("Vorgang Listenelement")).toHaveCount(1)
      await expect(page.getByText(procedureName)).toBeVisible()
    })

    await test.step("Load document units of procedure", async () => {
      await page.getByTestId("icons-open-close").click()
      await expect(page.getByText(documentNumber)).toBeVisible()
    })

    await test.step("Delete document unit and confirm dialog", async () => {
      await page.getByLabel("Dokumentationseinheit löschen").click()
      await expect(
        page.getByText("Dokumentationseinheit löschen"),
      ).toBeVisible()
      await page.getByLabel("Löschen", { exact: true }).click()
    })

    await test.step("Show message for procedure without doc units", async () => {
      await expect(
        page.getByText("Keine Dokeinheiten sind zugewiesen."),
      ).toBeVisible()
    })
  })

  test("View, navigate and reset paged results", async ({
    page,
    context,
    request,
  }) => {
    await test.step("Create 12 doc units with procedure", async () => {
      await Promise.all(
        Array.from({ length: 12 }).map(async () => {
          const { uuid } = await createNewDocUnit(context, request)
          createdDocUnitUuids.push(uuid)
          await assignDocUnitToNewProcedure(context, request, uuid)
        }),
      )
    })

    await test.step("Show pages without query", async () => {
      await navigateToProcedures(page)
      await expect(page.getByLabel("Vorgang Listenelement")).toHaveCount(10, {
        timeout,
      })
      await expect(page.getByText("Seite 1:")).toBeVisible()
    })

    await test.step("Go to next page", async () => {
      await page.getByLabel("nächste Ergebnisse").click()
      await expect(page.getByText("Seite 2:")).toBeVisible({ timeout })
    })

    await test.step("Search for prefix resets page", async () => {
      await page.getByPlaceholder("Nach Vorgängen suchen").fill(procedurePrefix)
      await expect(page.getByLabel("Vorgang Listenelement")).toHaveCount(10, {
        timeout,
      })
      await expect(page.getByText("Seite 1:")).toBeVisible()
    })
  })

  test.afterAll(async ({ browser }) => {
    await deleteAllProcedures(browser, procedurePrefix)
  })

  test.afterEach(async ({ context, request }) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    for (const uuid of createdDocUnitUuids) {
      try {
        await request.delete(`/api/v1/caselaw/documentunits/${uuid}`, {
          headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
        })
      } finally {
        // Ignore errors as the doc unit might already be deleted.
      }
    }
  })

  async function createNewDocUnit(
    context: BrowserContext,
    request: APIRequestContext,
  ): Promise<{ documentNumber: string; uuid: string }> {
    return await test.step("Create new doc unit", async () => {
      const cookies = await context.cookies()
      const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
      const response = await request.put(`/api/v1/caselaw/documentunits/new`, {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      })

      const { documentNumber, uuid } = await response.json()
      return { documentNumber, uuid }
    })
  }

  async function assignDocUnitToNewProcedure(
    context: BrowserContext,
    request: APIRequestContext,
    uuid: string,
  ): Promise<string> {
    return await test.step("Assign doc unit to new procedure", async () => {
      const cookies = await context.cookies()
      const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
      const procedureName = procedurePrefix + generateString({ length: 10 })

      await request.patch(`/api/v1/caselaw/documentunits/${uuid}`, {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
        data: {
          documentationUnitVersion: 1,
          patch: [
            {
              op: "add",
              path: "/coreData/procedure",
              value: { label: procedureName },
            },
          ],
          errorPaths: [],
        },
      })

      return procedureName
    })
  }
})

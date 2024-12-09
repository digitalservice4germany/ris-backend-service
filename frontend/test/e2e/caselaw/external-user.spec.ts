import { expect, Page } from "@playwright/test"
import {
  assignProcedureToDocUnit,
  clickCategoryButton,
  deleteAllProcedures,
  fillInput,
  navigateToCategories,
  navigateToProcedures,
  navigateToSearch,
  save,
} from "~/e2e/caselaw/e2e-utils"
import { caselawTest as test } from "~/e2e/caselaw/fixtures"
import { generateString } from "~/test-helper/dataGenerators"

test.describe(
  "external user",
  {
    annotation: {
      description:
        "https://digitalservicebund.atlassian.net/browse/RISDEV-2075",
      type: "epic",
    },
    tag: ["@RISDEV-2075"],
  },
  () => {
    // If tests run in parallel, we do not want to delete other procedures -> random prefix
    const procedurePrefix = `test_${generateString({ length: 10 })}`

    test(
      "In doc unit search, external user can not edit/delete unassigned doc units, but preview them",
      { tag: ["@RISDEV-4518", "@RISDEV-4519"] },
      async ({ pageWithExternalUser, documentNumber }) => {
        await test.step("Filter for unassigned doc unit and view in search results as external", async () => {
          await navigateToSearch(pageWithExternalUser)
          await pageWithExternalUser
            .getByLabel("Dokumentnummer Suche")
            .fill(documentNumber)
          await pageWithExternalUser
            .getByLabel("Nach Dokumentationseinheiten suchen")
            .click()
          await expect(
            pageWithExternalUser.locator(".table-row", {
              hasText: documentNumber,
            }),
          ).toBeVisible()
        })

        await test.step("Check edit/delete/preview buttons for doc unit in search result", async () => {
          // Preview is allowed
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit ansehen"),
          ).toHaveRole("link")

          // Edit/delete is not allowed
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit bearbeiten"),
          ).not.toHaveRole("link")
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit löschen"),
          ).not.toHaveRole("link")
        })
      },
    )

    test(
      "In doc unit search, external user can select edit for doc units, that were previously assigned to their user group by an internal user",
      { tag: ["@RISDEV-4518", "@RISDEV-4519"] },
      async ({ page, pageWithExternalUser, documentNumber }) => {
        const procedureName = await assignProcedureToDocUnit(
          page,
          documentNumber,
          procedurePrefix,
        )

        await assignUserGroupToProcedure(page, procedureName)

        await test.step("Filter for assigned doc unit and view in search results as external", async () => {
          await navigateToSearch(pageWithExternalUser)
          await pageWithExternalUser
            .getByLabel("Dokumentnummer Suche")
            .fill(documentNumber)
          await pageWithExternalUser
            .getByLabel("Nach Dokumentationseinheiten suchen")
            .click()
          await expect(
            pageWithExternalUser.locator(".table-row", {
              hasText: documentNumber,
            }),
          ).toBeVisible()
        })

        await test.step("Check edit/delete/preview buttons for doc unit in search result", async () => {
          // Edit/Preview is allowed
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit bearbeiten"),
          ).toHaveRole("link")
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit ansehen"),
          ).toHaveRole("link")

          // Delete is not allowed
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit löschen"),
          ).not.toHaveRole("link")
        })

        await unassignUserGroupFromProcedure(page, procedureName)
      },
    )

    test(
      "External user can only see procedures they are assigned to",
      { tag: ["@RISDEV-4519"] },
      async ({
        page,
        pageWithExternalUser,
        documentNumber,
        prefilledDocumentUnit,
        secondPrefilledDocumentUnit,
      }) => {
        const testCaseInfix = `_${generateString({ length: 10 })}_`

        const procedureName = await assignProcedureToDocUnit(
          page,
          documentNumber,
          procedurePrefix + testCaseInfix,
        )
        await assignUserGroupToProcedure(page, procedureName)

        // Procedures are created but not assigned
        await assignProcedureToDocUnit(
          page,
          prefilledDocumentUnit.documentNumber!,
          procedurePrefix + testCaseInfix,
        )
        await assignProcedureToDocUnit(
          page,
          secondPrefilledDocumentUnit.documentNumber!,
          procedurePrefix + testCaseInfix,
        )

        await test.step("External user sees only the single procedure they are assigned to", async () => {
          await navigateToProcedures(
            pageWithExternalUser,
            procedurePrefix + testCaseInfix,
          )
          await expect(
            pageWithExternalUser.getByLabel("Vorgang Listenelement"),
          ).toHaveCount(1)
          await expect(
            pageWithExternalUser.getByLabel("Vorgang Listenelement"),
          ).toContainText(procedureName)
        })

        await test.step("External user can edit but not delete doc units from procedure view", async () => {
          await pageWithExternalUser.getByLabel("Vorgang Listenelement").click()
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit bearbeiten"),
          ).toHaveRole("link")
          await expect(
            pageWithExternalUser.getByLabel("Dokumentationseinheit löschen"),
          ).not.toHaveRole("link")
        })
      },
    )

    test(
      "External user cannot edit a doc unit after they are unassigned",
      { tag: ["@RISDEV-4519"] },
      async ({ pageWithExternalUser, page, documentNumber }) => {
        await navigateToCategories(pageWithExternalUser, documentNumber)

        const procedureName = await assignProcedureToDocUnit(
          page,
          documentNumber,
          procedurePrefix,
        )
        await assignUserGroupToProcedure(page, procedureName)

        await test.step("Assigned external user can edit and save Entscheidungsname", async () => {
          await clickCategoryButton("Entscheidungsname", pageWithExternalUser)
          await pageWithExternalUser
            .getByLabel("Entscheidungsname")
            .fill("ein Name")
          await save(pageWithExternalUser)
        })

        await unassignUserGroupFromProcedure(page, procedureName)

        await test.step("Unassigned external user gets error when editing Entscheidungsname", async () => {
          await pageWithExternalUser
            .getByLabel("Entscheidungsname")
            .fill("ein anderer Name")
          await pageWithExternalUser
            .locator("[aria-label='Speichern Button']")
            .click()
          await expect(
            pageWithExternalUser
              .getByTestId("document-unit-save-button")
              .locator("p"),
          ).toHaveText(`Fehler beim Speichern: Keine Berechtigung`)
        })
      },
    )

    test(
      "External user cannot edit a doc unit after the doc unit is unassigned",
      { tag: ["@RISDEV-4519", "@RISDEV-4523"] },
      async ({ pageWithExternalUser, page, documentNumber }) => {
        await navigateToCategories(pageWithExternalUser, documentNumber)

        const procedureName = await assignProcedureToDocUnit(
          page,
          documentNumber,
          procedurePrefix,
        )
        await assignUserGroupToProcedure(page, procedureName)

        await test.step("Assigned external user can edit and save Notiz", async () => {
          await pageWithExternalUser.getByLabel("Seitenpanel öffnen").click()
          await fillInput(
            pageWithExternalUser,
            "Notiz Eingabefeld",
            "some text",
          )
          await save(pageWithExternalUser)
        })

        await test.step("Assigned external user cannot access Dokumente or Fundstellen", async () => {
          await expect(
            pageWithExternalUser.getByTestId(
              "caselaw-documentUnit-documentNumber-attachments",
            ),
          ).toBeHidden()
          await expect(
            pageWithExternalUser.getByTestId(
              "caselaw-documentUnit-documentNumber-references",
            ),
          ).toBeHidden()
        })

        // Assignment to previous procedure is overwritten -> user (group) is not assigned to doc unit anymore
        await assignProcedureToDocUnit(page, documentNumber, procedurePrefix)

        await test.step("External user gets error when editing Notiz of unassigned doc unit", async () => {
          await fillInput(
            pageWithExternalUser,
            "Notiz Eingabefeld",
            "some other text",
          )
          await pageWithExternalUser
            .locator("[aria-label='Speichern Button']")
            .click()
          await expect(
            pageWithExternalUser
              .getByTestId("document-unit-save-button")
              .locator("p"),
          ).toHaveText(`Fehler beim Speichern: Keine Berechtigung`)
        })
      },
    )

    test(
      "Assigned external user cannot edit core data of a doc unit via API",
      { tag: ["@RISDEV-4519"] },
      async ({ page, pageWithExternalUser, prefilledDocumentUnit }) => {
        const procedureName = await assignProcedureToDocUnit(
          page,
          prefilledDocumentUnit.documentNumber!,
          procedurePrefix,
        )
        await assignUserGroupToProcedure(page, procedureName)

        const cookies = await pageWithExternalUser.context().cookies()
        const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")

        await test.step("Assigned external user can edit allowed categories via API", async () => {
          const allowedApiResponse = await pageWithExternalUser
            .context()
            .request.patch(
              "api/v1/caselaw/documentunits/" + prefilledDocumentUnit.uuid,
              {
                data: {
                  documentationUnitVersion: 500,
                  patch: [],
                  errorPaths: [],
                },
                headers: { "X-XSRF-TOKEN": csrfToken!.value },
              },
            )
          await expect(allowedApiResponse).toBeOK()
        })

        await test.step("Assigned external user cannot edit forbidden categories via API", async () => {
          const forbiddenApiResponse = await page
            .context()
            .request.patch(
              "api/v1/caselaw/documentunits/" + prefilledDocumentUnit.uuid,
              {
                data: {
                  documentationUnitVersion: 501,
                  patch: [
                    {
                      op: "replace",
                      path: "/coreData/appraisalBody",
                      value: "321321easdasdqweqwe",
                    },
                  ],
                  errorPaths: [],
                },
                headers: { "X-XSRF-TOKEN": csrfToken!.value },
              },
            )
          expect(forbiddenApiResponse.status()).toBe(403)
        })
      },
    )

    test.afterAll(async ({ browser }) => {
      await deleteAllProcedures(browser, procedurePrefix)
    })

    async function assignUserGroupToProcedure(
      page: Page,
      procedureName: string,
    ) {
      await test.step("Internal user assigns a user group to the given procedure", async () => {
        await navigateToProcedures(page, procedureName)

        const assignRequest = page.waitForRequest(
          "**/api/v1/caselaw/procedure/*/assign/*",
          { timeout: 5_000 },
        )

        await page
          .getByLabel("Vorgang Listenelement")
          .getByLabel("dropdown input")
          .selectOption("Extern", { timeout: 5_000 })

        await assignRequest
        await page.reload()

        await expect(
          page.getByLabel("Vorgang Listenelement").getByLabel("dropdown input"),
          // The id of the user group "Extern"
        ).toHaveValue(/.+/, { timeout: 5_000 })
      })
    }

    async function unassignUserGroupFromProcedure(
      page: Page,
      procedureName: string,
    ) {
      await test.step("Internal user unassigns a user group from the given procedure", async () => {
        await navigateToProcedures(page, procedureName)

        const unassignRequest = page.waitForRequest(
          "**/api/v1/caselaw/procedure/*/unassign",
          { timeout: 5_000 },
        )

        await page
          .getByLabel("Vorgang Listenelement")
          .getByLabel("dropdown input")
          .selectOption("Nicht zugewiesen", { timeout: 5_000 })

        await unassignRequest
        await page.reload()

        await expect(
          page.getByLabel("Vorgang Listenelement").getByLabel("dropdown input"),
          // The unassigned option has an empty value
        ).toHaveValue(/^$/, { timeout: 5_000 })
      })
    }
  },
)

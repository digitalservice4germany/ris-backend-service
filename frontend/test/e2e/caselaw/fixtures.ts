import { Locator, Page, test } from "@playwright/test"
import dayjs from "dayjs"
import utc from "dayjs/plugin/utc"
import DocumentUnit from "../../../src/domain/documentUnit"
import { navigateToCategories } from "./e2e-utils"
import { Page as Pagination } from "@/components/Pagination.vue"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import { generateString } from "~/test-helper/dataGenerators"

dayjs.extend(utc)

// Declare the types of your fixtures.
type MyFixtures = {
  documentNumber: string
  prefilledDocumentUnit: DocumentUnit
  secondPrefilledDocumentUnit: DocumentUnit
  linkedDocumentNumber: string
  editorField: Locator
  pageWithBghUser: Page
  pageWithExternalUser: Page
  prefilledDocumentUnitBgh: DocumentUnit
  edition: LegalPeriodicalEdition
  editionWithReferences: LegalPeriodicalEdition
  foreignDocumentationUnit: DocumentUnitListEntry
}

export const caselawTest = test.extend<MyFixtures>({
  documentNumber: async ({ request, context }, use) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const response = await request.put(`/api/v1/caselaw/documentunits/new`, {
      headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
    })

    const { uuid, documentNumber } = await response.json()

    await use(documentNumber)
    const deleteResponse = await request.delete(
      `/api/v1/caselaw/documentunits/${uuid}`,
      {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    if (!deleteResponse.ok()) {
      throw Error(`DocumentUnit with number ${documentNumber} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  prefilledDocumentUnit: async ({ request, context }, use) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const response = await request.put(`/api/v1/caselaw/documentunits/new`, {
      headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
    })
    const prefilledDocumentUnit = await response.json()

    const courtResponse = await request.get(`api/v1/caselaw/courts?q=AG+Aachen`)
    const court = await courtResponse.json()

    const documentTypeResponse = await request.get(
      `api/v1/caselaw/documenttypes?q=Anerkenntnisurteil`,
    )
    const documentType = await documentTypeResponse.json()

    const updateResponse = await request.put(
      `/api/v1/caselaw/documentunits/${prefilledDocumentUnit.uuid}`,
      {
        data: {
          ...prefilledDocumentUnit,
          coreData: {
            ...prefilledDocumentUnit.coreData,
            court: court?.[0],
            documentType: documentType?.[0],
            fileNumbers: [generateString()],
            decisionDate: "2019-12-31",
            appraisalBody: "1. Senat, 2. Kammer",
          },
          shortTexts: {
            headnote: "testHeadnote",
            guidingPrinciple: "guidingPrinciple",
          },
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    await use(await updateResponse.json())

    const deleteResponse = await request.delete(
      `/api/v1/caselaw/documentunits/${prefilledDocumentUnit.uuid}`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    if (!deleteResponse.ok()) {
      throw Error(`DocumentUnit with number ${prefilledDocumentUnit.documentNumber} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  secondPrefilledDocumentUnit: async ({ request, context }, use) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const response = await context.request.put(
      `/api/v1/caselaw/documentunits/new`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    const secondPrefilledDocumentUnit = await response.json()

    const courtResponse = await request.get(`api/v1/caselaw/courts?q=AG+Aachen`)
    const court = await courtResponse.json()

    const documentTypeResponse = await request.get(
      `api/v1/caselaw/documenttypes?q=Anerkenntnisurteil`,
    )
    const documentType = await documentTypeResponse.json()

    const updateResponse = await request.put(
      `/api/v1/caselaw/documentunits/${secondPrefilledDocumentUnit.uuid}`,
      {
        data: {
          ...secondPrefilledDocumentUnit,
          coreData: {
            ...secondPrefilledDocumentUnit.coreData,
            court: court?.[0],
            documentType: documentType?.[0],
            fileNumbers: [generateString()],
            decisionDate: "2020-01-01",
          },
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    await use(await updateResponse.json())

    const deleteResponse = await request.delete(
      `/api/v1/caselaw/documentunits/${secondPrefilledDocumentUnit.uuid}`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    if (!deleteResponse.ok()) {
      throw Error(`DocumentUnit with number ${secondPrefilledDocumentUnit.documentNumber} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  // The prefilledDocumentUnit fixture is a dependant worker fixture, because it nees to be setup before and teared down after this function (in order to be deletable).
  linkedDocumentNumber: async (
    { request, context, prefilledDocumentUnit },
    use,
  ) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const response = await context.request.put(
      `/api/v1/caselaw/documentunits/new`,
      {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )
    const { uuid, documentNumber } = await response.json()

    await use(documentNumber)

    const deleteResponse = await request.delete(
      `/api/v1/caselaw/documentunits/${uuid}`,
      {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    if (!deleteResponse.ok()) {
      throw Error(`DocumentUnit with number ${documentNumber}, linked to ${prefilledDocumentUnit} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  editorField: async ({ page, documentNumber }, use) => {
    await navigateToCategories(page, documentNumber)
    const editorField = page.locator("[data-testid='Entscheidungsname'] >> div")
    await editorField.click()
    await editorField.type("this is text")

    await use(editorField)
  },

  pageWithBghUser: async ({ browser }, use) => {
    const bghContext = await browser.newContext({
      storageState: `test/e2e/shared/.auth/user_bgh.json`,
    })
    const pageWithBghUser = await bghContext.newPage()

    await use(pageWithBghUser)

    await pageWithBghUser.close()
    await bghContext.close()
  },

  pageWithExternalUser: async ({ browser }, use) => {
    const externalContext = await browser.newContext({
      storageState: `test/e2e/shared/.auth/user_external.json`,
    })
    const pageWithExternalUser = await externalContext.newPage()

    await use(pageWithExternalUser)

    await pageWithExternalUser.close()
    await externalContext.close()
  },

  prefilledDocumentUnitBgh: async ({ request, browser }, use) => {
    const context = await browser.newContext({
      storageState: `test/e2e/shared/.auth/user_bgh.json`,
    })
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")
    const response = await context.request.put(
      `/api/v1/caselaw/documentunits/new`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    const prefilledDocumentUnit = await response.json()

    const courtResponse = await request.get(`api/v1/caselaw/courts?q=AG+Aachen`)
    const court = await courtResponse.json()

    const documentTypeResponse = await request.get(
      `api/v1/caselaw/documenttypes?q=Anerkenntnisurteil`,
    )
    const documentType = await documentTypeResponse.json()

    const updateResponse = await context.request.put(
      `/api/v1/caselaw/documentunits/${prefilledDocumentUnit.uuid}`,
      {
        data: {
          ...prefilledDocumentUnit,
          coreData: {
            ...prefilledDocumentUnit.coreData,
            court: court?.[0],
            documentType: documentType?.[0],
            fileNumbers: [generateString()],
            decisionDate: "2019-12-31",
            appraisalBody: "1. Senat, 2. Kammer",
          },
          shortTexts: {
            headnote: "testHeadnote",
            guidingPrinciple: "guidingPrinciple",
          },
          note: "example note",
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    await use(await updateResponse.json())

    const deleteResponse = await context.request.delete(
      `/api/v1/caselaw/documentunits/${prefilledDocumentUnit.uuid}`,
      { headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" } },
    )
    if (!deleteResponse.ok()) {
      throw Error(`DocumentUnit with number ${prefilledDocumentUnit.documentNumber} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  edition: async ({ request, context }, use) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")

    const legalPeriodicalSearchResponse = await request.get(
      `api/v1/caselaw/legalperiodicals?q=MMG`,
    )

    const legalPeriodical = (
      (await legalPeriodicalSearchResponse.json()) as LegalPeriodicalEdition[]
    ).at(0)

    const editionResponse = await request.put(
      `api/v1/caselaw/legalperiodicaledition`,
      {
        data: {
          legalPeriodical: legalPeriodical,
          id: crypto.randomUUID(),
          prefix: "2024, ",
          suffix: ", Heft " + generateString(),
          name: "2024, " + generateString(),
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    const edition = await editionResponse.json()
    await use(edition)

    const deleteResponse = await request.delete(
      `/api/v1/caselaw/legalperiodicaledition/${edition.id}`,
      {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    if (!deleteResponse.ok()) {
      throw Error(`Edition with number ${edition.id} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },

  foreignDocumentationUnit: async ({ request }, use) => {
    const foreignDocumentUnitSearchResponse = await request.get(
      `api/v1/caselaw/documentunits/search?pg=0&sz=100&documentNumber=YYTestDoc0001`,
    )

    const foreignDocumentUnitPage =
      (await foreignDocumentUnitSearchResponse.json()) as Pagination<DocumentUnitListEntry>

    const foreignDocumentUnit = (
      foreignDocumentUnitPage.content as DocumentUnitListEntry[]
    ).at(0)

    await use(foreignDocumentUnit!)
  },

  editionWithReferences: async (
    { request, context, prefilledDocumentUnit },
    use,
  ) => {
    const cookies = await context.cookies()
    const csrfToken = cookies.find((cookie) => cookie.name === "XSRF-TOKEN")

    const legalPeriodicalSearchResponse = await request.get(
      `api/v1/caselaw/legalperiodicals?q=MMG`,
    )

    const foreignDocumentUnitSearchResponse = await request.get(
      `api/v1/caselaw/documentunits/search?pg=0&sz=100&documentNumber=YYTestDoc0001`,
    )

    const foreignDocumentUnitPage =
      (await foreignDocumentUnitSearchResponse.json()) as Pagination<DocumentUnitListEntry>

    const foreignDocumentUnit = (
      foreignDocumentUnitPage.content as DocumentUnitListEntry[]
    ).at(0)

    const legalPeriodical = (
      (await legalPeriodicalSearchResponse.json()) as LegalPeriodicalEdition[]
    ).at(0)

    const editionResponse = await request.put(
      `api/v1/caselaw/legalperiodicaledition`,
      {
        data: {
          legalPeriodical: legalPeriodical,
          id: crypto.randomUUID(),
          prefix: "2024, ",
          suffix: ", Heft 1",
          name: "2024, " + generateString(),
          references: [
            {
              id: crypto.randomUUID(),
              referenceType: "caselaw",
              citation: "2024, 12-22, Heft 1",
              referenceSupplement: "L",
              legalPeriodicalRawValue: "MMG",
              legalPeriodical: legalPeriodical,
              documentationUnit: new RelatedDocumentation({
                documentNumber: prefilledDocumentUnit.documentNumber,
                uuid: prefilledDocumentUnit.uuid,
              }),
            },
            {
              id: crypto.randomUUID(),
              referenceType: "caselaw",
              citation: "2024, 1-11, Heft 1",
              legalPeriodicalRawValue: "MMG",
              legalPeriodical: legalPeriodical,
              // Published foreign BAG docunit
              documentationUnit: new RelatedDocumentation({
                documentNumber: foreignDocumentUnit?.documentNumber,
                uuid: foreignDocumentUnit?.uuid,
              }),
            },
          ],
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    const edition = await editionResponse.json()
    if (!editionResponse.ok()) {
      throw Error(`Edition couldn't be created:
      ${editionResponse.status()} ${editionResponse.statusText()}`)
    }
    await use(edition)

    // delete references to be able to delete
    const response = await request.put(
      `api/v1/caselaw/legalperiodicaledition`,
      {
        data: {
          legalPeriodical: edition.legalPeriodical,
          id: edition.id,
          prefix: edition.prefix,
          suffix: edition.suffix,
          name: "NAME",
          references: [],
        },
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )
    await response.json()
    if (!response.ok()) {
      throw Error(`References in Edition with number ${edition.id} couldn't be deleted:
      ${response.status()} ${response.statusText()}`)
    }

    const deleteResponse = await request.delete(
      `/api/v1/caselaw/legalperiodicaledition/${edition.id}`,
      {
        headers: { "X-XSRF-TOKEN": csrfToken?.value ?? "" },
      },
    )

    if (!deleteResponse.ok()) {
      throw Error(`Edition with number ${edition.id} couldn't be deleted:
      ${deleteResponse.status()} ${deleteResponse.statusText()}`)
    }
  },
})

import * as jsonpatch from "fast-json-patch"
import { defineStore } from "pinia"
import { ref } from "vue"
import fields from "@/data/fieldNames.json"
import DocumentUnit from "@/domain/documentUnit"
import { RisJsonPatch } from "@/domain/risJsonPatch"
import errorMessages from "@/i18n/errors.json"
import documentUnitService from "@/services/documentUnitService"
import {
  FailedValidationServerResponse,
  ServiceResponse,
} from "@/services/httpClient"

export const useDocumentUnitStore = defineStore("docunitStore", () => {
  const documentUnit = ref<DocumentUnit | undefined>(undefined)
  const originalDocumentUnit = ref<DocumentUnit | undefined>(undefined)

  async function loadDocumentUnit(
    documentNumber: string,
  ): Promise<ServiceResponse<DocumentUnit>> {
    const response =
      await documentUnitService.getByDocumentNumber(documentNumber)
    if (response.data) {
      documentUnit.value = response.data
      originalDocumentUnit.value = JSON.parse(JSON.stringify(response.data)) // Deep copy for tracking changes
    } else {
      documentUnit.value = undefined
    }
    return response
  }

  async function updateDocumentUnit(): Promise<
    ServiceResponse<RisJsonPatch | FailedValidationServerResponse>
  > {
    if (!documentUnit.value || !originalDocumentUnit.value) {
      return {
        status: 404,
        data: undefined,
        error: errorMessages.DOCUMENT_UNIT_COULD_NOT_BE_LOADED,
      }
    }

    // Create JSON Patch
    const patch = jsonpatch.compare(
      originalDocumentUnit.value,
      documentUnit.value,
    )

    // If there are no updates in the client, get the current version from backend
    // if (patch.length === 0 && documentUnit.value.documentNumber) {
    //   const response = await loadDocumentUnit(documentUnit.value.documentNumber)
    //   if (response.data) {
    //     return {
    //       status: response.status,
    //       data: {
    //         documentationUnitVersion: response.data.version,
    //         patch: [],
    //         errorPaths: [],
    //       },
    //     }
    //   } else {
    //     return {
    //       status: 404,
    //       data: undefined,
    //       error: errorMessages.DOCUMENT_UNIT_COULD_NOT_BE_LOADED,
    //     }
    //   }
    // }

    const response = await documentUnitService.update(documentUnit.value.uuid, {
      documentationUnitVersion: documentUnit.value.version,
      patch,
      errorPaths: [],
    })

    if (response.status === 200) {
      //Apply backend patch to original documentunit reference, with updated version
      const backendPatch = response.data as RisJsonPatch

      try {
        applyPatch(backendPatch)
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
      } catch (error) {
        if (documentUnit.value.documentNumber) {
          const response = await loadDocumentUnit(
            documentUnit.value.documentNumber,
          )
          if (response.data) {
            return {
              status: response.status,
              data: {
                documentationUnitVersion: response.data.version,
                patch: [],
                errorPaths: [],
              },
            }
          } else {
            return {
              status: 404,
              data: undefined,
              error: errorMessages.DOCUMENT_UNIT_COULD_NOT_BE_LOADED,
            }
          }
        }
      }
      documentUnit.value.version = backendPatch.documentationUnitVersion

      // Deep copy
      originalDocumentUnit.value = new DocumentUnit(documentUnit.value.uuid, {
        ...JSON.parse(JSON.stringify(documentUnit.value)),
      })

      if (
        backendPatch.errorPaths != undefined &&
        backendPatch.errorPaths.length > 0
      ) {
        // ^\/ matches a leading slash.
        // \/$ matches a trailing slash.
        // \/\d+$ matches a trailing slash followed by one or more digits at the end of the string, to collect list items.
        const path = backendPatch.errorPaths[0].replace(/(^\/|\/$|\/\d+$)/g, "")

        const mappedValue = fields[path as keyof typeof fields]

        return {
          status: 207,
          data: undefined,
          error: {
            title: mappedValue,
          },
        }
      }
    } else {
      return {
        status: response.status,
        data: undefined,
        error:
          response.status === 403
            ? errorMessages.NOT_ALLOWED
            : errorMessages.DOCUMENT_UNIT_UPDATE_FAILED,
      }
    }
    return response
  }

  function applyPatch(backendPatch: RisJsonPatch) {
    if (!documentUnit.value?.uuid) {
      throw new Error("Can't apply patch on an empty uuid")
    }
    jsonpatch.applyPatch(documentUnit.value, backendPatch.patch)
    documentUnit.value = new DocumentUnit(documentUnit.value.uuid, {
      ...JSON.parse(JSON.stringify(documentUnit.value)),
    })
  }

  return {
    documentUnit,
    loadDocumentUnit,
    updateDocumentUnit,
  }
})

import httpClient, { ServiceResponse } from "./httpClient"
import { Docx2HTML, ConvertedDocumentElement } from "@/domain/docx2html"
import errorMessages from "@/i18n/errors.json"

interface AttachmentService {
  upload(
    documentUnitUuid: string,
    file: File,
  ): Promise<ServiceResponse<Docx2HTML>>

  delete(
    documentUnitUuid: string,
    s3path: string,
  ): Promise<ServiceResponse<unknown>>

  getAttachmentAsHtml(
    uuid: string,
    s3path: string,
  ): Promise<ServiceResponse<Docx2HTML>>

  reconvertDocument(
    uuid: string,
    s3path: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>

  getConvertedElementList(
    uuid: string,
    s3path: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>

  removeBorderNumbers(
    uuid: string,
    s3path: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>

  addBorderNumbers(
    uuid: string,
    s3path: string,
    elementId?: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>

  removeSingleBorderNumber(
    uuid: string,
    s3path: string,
    elementId: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>

  joinBorderNumbers(
    uuid: string,
    s3path: string,
    elementId: string,
  ): Promise<ServiceResponse<ConvertedDocumentElement[]>>
}

const service: AttachmentService = {
  async upload(documentUnitUuid: string, file: File) {
    const extension = file.name?.split(".").pop()
    if (!extension || extension.toLowerCase() !== "docx") {
      return {
        status: 415,
        error: {
          title: errorMessages.WRONG_MEDIA_TYPE_DOCX_REQUIRED.title,
          description: errorMessages.WRONG_MEDIA_TYPE_DOCX_REQUIRED.description,
        },
      }
    }

    const response = await httpClient.put<File, Docx2HTML>(
      `caselaw/documentunits/${documentUnitUuid}/file`,
      {
        headers: {
          "Content-Type":
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "X-Filename": file.name,
        },
      },
      file,
    )
    if (response.status === 413) {
      response.error = {
        title: errorMessages.FILE_TOO_LARGE_CASELAW.title,
        description: errorMessages.FILE_TOO_LARGE_CASELAW.description,
      }
    } else if (response.status === 415) {
      response.error = {
        title: errorMessages.WRONG_MEDIA_TYPE_DOCX_REQUIRED.title,
        description: errorMessages.WRONG_MEDIA_TYPE_DOCX_REQUIRED.description,
      }
    } else if (response.status === 422) {
      response.error = {
        title: errorMessages.DOCX_PARSING_ERROR.title,
        description: errorMessages.DOCX_PARSING_ERROR.description,
      }
    } else if (response.status >= 300) {
      response.error = {
        title: errorMessages.SERVER_ERROR.title,
        description: errorMessages.SERVER_ERROR.description,
      }
    } else {
      response.error = undefined
    }

    return response
  },

  async delete(documentUnitUuid: string, s3path: string) {
    const response = await httpClient.delete(
      `caselaw/documentunits/${documentUnitUuid}/file/${s3path}`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.FILE_DELETE_FAILED.title }
        : undefined

    return response
  },

  async getAttachmentAsHtml(uuid: string, s3path: string) {
    const response = await httpClient.get<Docx2HTML>(
      `caselaw/documentunits/${uuid}/docx/${s3path}`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async getConvertedElementList(uuid: string, s3path: string) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/converted`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async reconvertDocument(uuid: string, s3path: string) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/reconvert`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async removeBorderNumbers(uuid: string, s3path: string) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/removebordernumbers`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async addBorderNumbers(uuid: string, s3path: string, elementId?: string) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/addbordernumbers` +
        (elementId ? "?startAt=" + elementId : ""),
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async removeSingleBorderNumber(
    uuid: string,
    s3path: string,
    elementId: string,
  ) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/removebordernumber/${elementId}`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },

  async joinBorderNumbers(uuid: string, s3path: string, elementId: string) {
    const response = await httpClient.get<ConvertedDocumentElement[]>(
      `caselaw/documentunits/${uuid}/docx/${s3path}/joinbordernumbers/${elementId}`,
    )
    response.error =
      response.status >= 300
        ? { title: errorMessages.DOCX_COULD_NOT_BE_LOADED.title }
        : undefined

    return response
  },
}

export default service

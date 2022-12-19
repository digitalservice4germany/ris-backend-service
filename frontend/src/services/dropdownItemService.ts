import httpClient, { ServiceResponse } from "./httpClient"
import { ComboboxItem } from "@/domain"
import { Court } from "@/domain/documentUnit"

enum Endpoint {
  documentTypes = "lookuptable/documentTypes",
  courts = "lookuptable/courts",
}

type DocumentType = {
  id: number
  jurisShortcut: string
  label: string
}

type DropdownType = DocumentType[] | Court[]

function formatDropdownItems(
  responseData: DropdownType,
  endpoint: Endpoint
): ComboboxItem[] {
  switch (endpoint) {
    case Endpoint.documentTypes: {
      return (responseData as DocumentType[]).map((item) => ({
        text: item.jurisShortcut + " - " + item.label,
        value: item.label,
      }))
    }
    case Endpoint.courts: {
      return (responseData as Court[]).map((item) => ({
        text: item.label,
        value: item,
      }))
    }
  }
}

async function fetchFromEndpoint(endpoint: Endpoint, filter?: string) {
  const response = await httpClient.get<DropdownType>(
    `caselaw/${endpoint}`,
    filter ? { params: { searchStr: filter } } : undefined
  )
  if (response.data) {
    return {
      status: response.status,
      data: formatDropdownItems(response.data, endpoint),
    }
  } else {
    return {
      status: response.status,
      error: {
        title: "Serverfehler.",
        description: "Dropdown Items konnten nicht geladen werden.",
      },
    }
  }
}

type ComboboxItemService = {
  [key in keyof typeof Endpoint as `get${Capitalize<key>}`]: (
    filter?: string
  ) => Promise<ServiceResponse<ComboboxItem[]>>
} & {
  filterItems: (
    items: ComboboxItem[]
  ) => (filter?: string) => Promise<ServiceResponse<ComboboxItem[]>>
}

const service: ComboboxItemService = {
  filterItems: (items: ComboboxItem[]) => (filter?: string) => {
    const filteredItems = filter
      ? items.filter((item) => item.text.includes(filter))
      : items
    return Promise.resolve({ status: 200, data: filteredItems })
  },
  getCourts: (filter?: string) => fetchFromEndpoint(Endpoint.courts, filter),
  getDocumentTypes: (filter?: string) =>
    fetchFromEndpoint(Endpoint.documentTypes, filter),
}

export default service
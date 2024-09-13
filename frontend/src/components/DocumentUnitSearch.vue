<script lang="ts" setup>
import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import DateUtil from "../utils/dateUtil"
import DocumentUnitList from "@/components/DocumentUnitList.vue"
import DocumentUnitSearchEntryForm, {
  DocumentUnitSearchParameter,
} from "@/components/DocumentUnitSearchEntryForm.vue"
import InfoModal from "@/components/InfoModal.vue"
import TextButton from "@/components/input/TextButton.vue"
import Pagination, { Page } from "@/components/Pagination.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import { Query } from "@/composables/useQueryFromRoute"
import { Court } from "@/domain/documentUnit"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import errorMessages from "@/i18n/errors.json"
import ComboboxItemService from "@/services/comboboxItemService"
import service from "@/services/documentUnitService"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const router = useRouter()
const store = useDocumentUnitStore()

const currentPage = ref<Page<DocumentUnitListEntry>>()
const documentUnitListEntries = computed(() => currentPage.value?.content)

const itemsPerPage = 100
const searchResponseError = ref()
const isLoading = ref(false)
const searchQuery = ref<Query<DocumentUnitSearchParameter>>()
const pageNumber = ref<number>(0)
const isInternalUser = useInternalUser()

const emptyStateLabel = computed(() => {
  if (!documentUnitListEntries.value) {
    if (isInternalUser) {
      return "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit."
    } else {
      return "Starten Sie die Suche."
    }
  } else if (documentUnitListEntries.value.length === 0) {
    return errorMessages.SEARCH_RESULTS_NOT_FOUND.title
  }
  return undefined
})

/**
 * Searches all documentation units by given input and updates the local
 * documentunit list entries, the currentPage for pagination and catches errors
 * if they happen. When no results found, but a court was given, the court input
 * is validated against the court table, in order to be able to create a new
 * documentation unit from search the given search input, if the user wants to.
 */
async function search() {
  isLoading.value = true

  const response = await service.searchByDocumentUnitSearchInput({
    ...(pageNumber.value != undefined
      ? { pg: pageNumber.value.toString() }
      : {}),
    ...(itemsPerPage != undefined ? { sz: itemsPerPage.toString() } : {}),
    ...searchQuery.value,
  })
  if (response.data) {
    currentPage.value = response.data
  }
  if (response.error) {
    searchResponseError.value = response.error
  }

  if (
    isSearchCompletedWithNoResults.value &&
    !hasFilters.value &&
    searchQuery.value?.courtType
  ) {
    const courtFilter =
      searchQuery.value.courtType +
      (searchQuery.value.courtLocation
        ? ` ${searchQuery.value.courtLocation}`
        : "")

    const courtResponse = (await ComboboxItemService.getCourts(courtFilter))
      .data

    //filter for exact matches
    const matches = courtResponse
      ? courtResponse.filter((item) => item.label === courtFilter)
      : []

    // add as court query only if 1 exact match
    courtFromQuery.value =
      matches.length === 1 ? (matches[0].value as Court) : undefined
  } else courtFromQuery.value = undefined
  isLoading.value = false
}

/**
 * Deletes a documentation unit
 * @param {DocumentUnitListEntry} documentUnitListEntry - The entry in the list to be removed
 */
async function handleDelete(documentUnitListEntry: DocumentUnitListEntry) {
  if (documentUnitListEntries.value && currentPage.value) {
    const response = await service.delete(documentUnitListEntry.uuid as string)
    if (response.status === 200) {
      if (documentUnitListEntries.value.length === 1) {
        await search()
        return
      }
      const newEntries = documentUnitListEntries.value.filter(
        (item) => item != documentUnitListEntry,
      )
      currentPage.value.content = newEntries
      currentPage.value.numberOfElements = newEntries.length
      currentPage.value.empty = newEntries.length == 0
    } else {
      alert("Fehler beim Löschen der Dokumentationseinheit: " + response.data)
    }
  }
}

/**
 * When using the navigation a new page number is set, the search is triggered,
 * with the given page number.
 * @param {number} page - The page to be updated
 */
async function updatePage(page: number) {
  pageNumber.value = page
  search()
}

/**
 * The search form emits an event, when clicking the search button and is
 * triggering the search with the updated query here.
 * It will always reset the pagination to the first page.
 * @param {Query<DocumentUnitSearchParameter>} value - The page to be updated
 */
async function updateQuery(value: Query<DocumentUnitSearchParameter>) {
  searchQuery.value = value
  pageNumber.value = 0
  search()
}

/**
 * The search form emits an event, when resetting the search,
 * which empties the document unit list and resets the pagination.
 */
async function handleReset() {
  currentPage.value = undefined
}

const createFromSearchQueryResponseError = ref<ResponseError | undefined>()

/**
 * When a search returns no results and at least one valid search parameter
 * is given and no filters (documentNumber, myDocofficeOnly, status) are set,
 * a new documentation unit can be created with fileNumber, court and/ or date
 * from the current search query.
 */
async function createFromSearchQuery() {
  isLoading.value = true
  createFromSearchQueryResponseError.value = undefined
  const createResponse = await service.createNew()
  if (createResponse.error) {
    createFromSearchQueryResponseError.value = createResponse.error
    isLoading.value = false
    return
  }

  const docUnit = createResponse.data
  docUnit.coreData.fileNumbers = fileNumberFromQuery.value
    ? [fileNumberFromQuery.value]
    : []
  docUnit.coreData.decisionDate = dateFromQuery.value
  docUnit.coreData.court = courtFromQuery.value
  await store.loadDocumentUnit(docUnit.documentNumber!)
  store.documentUnit = docUnit

  const updateResponse = await store.updateDocumentUnit()

  if (updateResponse.error) {
    createFromSearchQueryResponseError.value = updateResponse.error
    if (docUnit?.uuid) await service.delete(docUnit.uuid)
    isLoading.value = false
    return
  }

  await router.push({
    name: "caselaw-documentUnit-documentNumber-categories",
    params: { documentNumber: createResponse.data.documentNumber },
  })
  isLoading.value = false
}

const fileNumberFromQuery = computed(() => {
  return searchQuery.value?.fileNumber?.trim()
})

const courtFromQuery = ref<Court | undefined>()
const dateFromQuery = computed(() => {
  if (
    (searchQuery.value?.decisionDateEnd != undefined &&
      searchQuery.value?.decisionDateEnd != searchQuery.value?.decisionDate) ||
    searchQuery.value?.decisionDate == undefined
  )
    return undefined
  return searchQuery.value?.decisionDate
})

const isSearchCompletedWithNoResults = computed(
  () =>
    documentUnitListEntries.value != undefined &&
    documentUnitListEntries.value.length === 0,
)
const hasFilters = computed(
  () =>
    searchQuery.value != undefined &&
    (searchQuery.value.documentNumber ||
      searchQuery.value.publicationStatus ||
      searchQuery.value.myDocOfficeOnly),
)
const hasInvalidOrNoQueryData = computed(
  () =>
    searchQuery.value != undefined &&
    !fileNumberFromQuery.value &&
    !courtFromQuery.value &&
    !dateFromQuery.value,
)

const showDefaultLink = computed(() => {
  return (
    !isSearchCompletedWithNoResults.value ||
    hasFilters.value ||
    hasInvalidOrNoQueryData.value
  )
})
</script>

<template>
  <div>
    <DocumentUnitSearchEntryForm
      :is-loading="isLoading"
      @reset-search-results="handleReset"
      @search="updateQuery"
    />
    <Pagination
      :is-loading="isLoading"
      navigation-position="bottom"
      :page="currentPage"
      @update-page="updatePage"
    >
      <DocumentUnitList
        class="grow"
        :document-unit-list-entries="documentUnitListEntries"
        :empty-state="emptyStateLabel"
        :is-loading="isLoading"
        :search-response-error="searchResponseError"
        @delete-document-unit="handleDelete"
      >
        <template v-if="isInternalUser" #newlink>
          <TextButton
            v-if="showDefaultLink"
            aria-label="Neue Dokumentationseinheit erstellen"
            button-type="ghost"
            label="Neue Dokumentationseinheit erstellen"
            @click="router.push({ name: 'caselaw-documentUnit-new' })"
          />
          <div v-else class="space-y-16 text-center">
            <div v-if="createFromSearchQueryResponseError" class="mb-24">
              <InfoModal
                :description="createFromSearchQueryResponseError.description"
                :title="createFromSearchQueryResponseError.title"
              />
            </div>
            <template v-else>
              <p>
                Sie können die folgenden Stammdaten übernehmen und eine neue<br />Dokumentationseinheit
                erstellen:
              </p>
              <p class="ds-label-01-bold mb-16 text-center">
                <span :class="{ 'text-gray-800': !fileNumberFromQuery }">{{
                  `${fileNumberFromQuery ?? "Aktenzeichen unbekannt"}, `
                }}</span>
                <span :class="{ 'text-gray-800': !courtFromQuery }">{{
                  `${courtFromQuery?.label ?? "Gericht unbekannt"}, `
                }}</span>
                <span :class="{ 'text-gray-800': !dateFromQuery }">
                  {{ DateUtil.formatDate(dateFromQuery) || "Datum unbekannt" }}
                </span>
              </p>
              <TextButton
                button-type="tertiary"
                label="Übernehmen und fortfahren"
                size="small"
                @click="createFromSearchQuery"
              />
            </template>
          </div>
        </template>
      </DocumentUnitList>
    </Pagination>
  </div>
</template>

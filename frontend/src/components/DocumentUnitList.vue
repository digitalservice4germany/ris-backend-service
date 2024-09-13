<script lang="ts" setup>
import dayjs from "dayjs"
import { computed, ref } from "vue"
import DocumentUnitListEntry from "../domain/documentUnitListEntry"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import IconBadge from "@/components/IconBadge.vue"
import InfoModal from "@/components/InfoModal.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import PopupModal from "@/components/PopupModal.vue"
import SearchResultStatus from "@/components/SearchResultStatus.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import { useStatusBadge } from "@/composables/useStatusBadge"
import { ResponseError } from "@/services/httpClient"
import IconAttachedFile from "~icons/ic/baseline-attach-file"
import IconDelete from "~icons/ic/baseline-close"
import IconError from "~icons/ic/baseline-error"
import IconSubject from "~icons/ic/baseline-subject"
import IconNote from "~icons/ic/outline-comment-bank"
import IconEdit from "~icons/ic/outline-edit"
import IconView from "~icons/ic/outline-remove-red-eye"

const props = defineProps<{
  documentUnitListEntries?: DocumentUnitListEntry[]
  searchResponseError?: ResponseError
  isLoading?: boolean
  emptyState?: string
}>()
const emit = defineEmits<{
  deleteDocumentUnit: [documentUnitListEntry: DocumentUnitListEntry]
}>()

const emptyStatus = computed(() => props.emptyState)

const listEntries = computed(() => {
  return props.documentUnitListEntries && !props.isLoading
    ? props.documentUnitListEntries
    : []
})

const showModal = ref(false)
const selectedDocumentUnitListEntry = ref<DocumentUnitListEntry>()
const popupModalText = computed(
  () =>
    `Möchten Sie die Dokumentationseinheit ${selectedDocumentUnitListEntry?.value?.documentNumber} wirklich dauerhaft löschen?`,
)

/**
 * Stops propagation of scrolling event, and toggles the showModal value
 */
function toggleModal() {
  showModal.value = !showModal.value
  if (showModal.value) {
    const scrollLeft = document.documentElement.scrollLeft
    const scrollTop = document.documentElement.scrollTop
    window.onscroll = () => {
      window.scrollTo(scrollLeft, scrollTop)
    }
  } else {
    window.onscroll = () => {
      return
    }
  }
}

/**
 * Clicking on a delete icon of a list entry triggers toggleModal() and asks for user input to proceed
 * @param {DocumentUnitListEntry} documentUnitListEntry - The documentationunit list entry to be deleted
 */
function setSelectedDocumentUnitListEntry(
  documentUnitListEntry: DocumentUnitListEntry,
) {
  selectedDocumentUnitListEntry.value = documentUnitListEntry
  toggleModal()
}

/**
 * Propagates delete event to parent and closes modal again
 */
function onDelete() {
  if (selectedDocumentUnitListEntry.value) {
    emit("deleteDocumentUnit", selectedDocumentUnitListEntry.value)
    toggleModal()
  }
}
</script>

<template>
  <div>
    <PopupModal
      v-if="showModal"
      aria-label="Dokumentationseinheit löschen"
      cancel-button-type="tertiary"
      confirm-button-type="destructive"
      confirm-text="Löschen"
      :content-text="popupModalText"
      header-text="Dokumentationseinheit löschen"
      @close-modal="toggleModal"
      @confirm-action="onDelete"
    />
    <TableView class="relative table w-full border-separate">
      <TableHeader>
        <CellHeaderItem class="w-[1%]"> Dokumentnummer</CellHeaderItem>
        <CellHeaderItem> Gerichtstyp</CellHeaderItem>
        <CellHeaderItem> Ort</CellHeaderItem>
        <CellHeaderItem> Datum</CellHeaderItem>
        <CellHeaderItem> Aktenzeichen</CellHeaderItem>
        <CellHeaderItem> Spruchkörper</CellHeaderItem>
        <CellHeaderItem> Typ</CellHeaderItem>
        <CellHeaderItem> Status</CellHeaderItem>
        <CellHeaderItem> Fehler</CellHeaderItem>
        <CellHeaderItem />
      </TableHeader>
      <TableRow
        v-for="(listEntry, id) in listEntries"
        :key="id"
        data-testid="listEntry"
      >
        <CellItem>
          <FlexContainer align-items="items-center" class="space-x-8">
            <FlexItem class="flex-grow"
              >{{ listEntry.documentNumber }}
            </FlexItem>
            <FlexItem
              v-if="listEntry.hasAttachments"
              class="flex-end text-blue-800"
              data-testid="file-attached-icon"
            >
              <IconAttachedFile
                aria-label="Anhang vorhanden"
                class="h-20 w-20"
              />
            </FlexItem>
            <FlexItem v-else class="flex-end text-gray-500">
              <IconAttachedFile
                aria-label="Kein Anhang vorhanden"
                class="h-20 w-20"
              />
            </FlexItem>
            <FlexItem
              v-if="listEntry.hasHeadnoteOrPrinciple"
              class="flex-end text-blue-800"
              data-testid="headnote-principle-icon"
            >
              <IconSubject aria-label="Langtext vorhanden" class="h-20 w-20" />
            </FlexItem>
            <span v-else class="text-gray-500"
              ><IconSubject
                aria-label="Kein Langtext vorhanden"
                class="h-20 w-20"
            /></span>
            <FlexItem
              v-if="listEntry.hasNote"
              class="flex-end text-blue-800"
              data-testid="note-icon"
            >
              <IconNote aria-label="Notiz vorhanden" class="h-20 w-20" />
            </FlexItem>
            <span v-else class="text-gray-500"
              ><IconNote aria-label="Keine Notiz vorhanden" class="h-20 w-20"
            /></span>
          </FlexContainer>
        </CellItem>
        <CellItem>
          {{ listEntry.court?.type ?? "-" }}
        </CellItem>
        <CellItem>
          {{ listEntry.court?.location ?? "-" }}
        </CellItem>
        <CellItem>
          {{
            listEntry.decisionDate
              ? dayjs(listEntry.decisionDate).format("DD.MM.YYYY")
              : "-"
          }}
        </CellItem>
        <CellItem>
          {{ listEntry.fileNumber ? listEntry.fileNumber : "-" }}
        </CellItem>
        <CellItem>
          {{ listEntry.appraisalBody ?? "-" }}
        </CellItem>
        <CellItem>
          {{
            listEntry.documentType ? listEntry.documentType.jurisShortcut : "-"
          }}
        </CellItem>
        <CellItem class="min-w-176">
          <IconBadge
            v-if="listEntry.status?.publicationStatus"
            v-bind="useStatusBadge(listEntry.status).value"
            data-testid="publication-status"
          />
        </CellItem>
        <CellItem>
          <IconBadge
            v-if="listEntry.status?.withError"
            background-color="bg-red-300"
            color="text-red-900"
            :icon="IconError"
            label="Fehler"
          />
          <span v-else>-</span>
        </CellItem>
        <CellItem class="flex">
          <div class="float-end flex">
            <router-link
              v-if="listEntry.isEditable"
              aria-label="Dokumentationseinheit bearbeiten"
              class="cursor-pointer border-2 border-r-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
              target="_blank"
              :to="{
                name: 'caselaw-documentUnit-documentNumber-categories',
                params: { documentNumber: listEntry.documentNumber },
              }"
            >
              <IconEdit />
            </router-link>
            <div
              v-else
              aria-label="Dokumentationseinheit bearbeiten"
              class="border-2 border-r-0 border-solid border-gray-600 p-4 text-gray-600"
            >
              <IconEdit />
            </div>

            <router-link
              aria-label="Dokumentationseinheit ansehen"
              class="cursor-pointer border-2 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
              target="_blank"
              :to="{
                name: 'caselaw-documentUnit-documentNumber-preview',
                params: { documentNumber: listEntry.documentNumber },
              }"
            >
              <IconView />
            </router-link>
            <button
              v-if="listEntry.isDeletable"
              aria-label="Dokumentationseinheit löschen"
              class="cursor-pointer border-2 border-l-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
              @click="
                setSelectedDocumentUnitListEntry(
                  documentUnitListEntries?.find(
                    (entry) => entry.uuid == listEntry.uuid,
                  ) as DocumentUnitListEntry,
                )
              "
              @keyup.enter="
                setSelectedDocumentUnitListEntry(
                  documentUnitListEntries?.find(
                    (entry) => entry.uuid == listEntry.uuid,
                  ) as DocumentUnitListEntry,
                )
              "
            >
              <IconDelete />
            </button>
            <div
              v-else
              aria-label="Dokumentationseinheit löschen"
              class="border-2 border-l-0 border-solid border-gray-600 p-4 text-gray-600"
            >
              <IconDelete />
            </div>
          </div>
        </CellItem>
      </TableRow>
    </TableView>
    <!-- Loading State -->
    <div
      v-if="isLoading"
      class="my-112 grid justify-items-center bg-white bg-opacity-60"
    >
      <LoadingSpinner />
    </div>
    <!-- Error State -->
    <div v-if="searchResponseError" class="mt-24">
      <InfoModal
        :description="searchResponseError.description"
        :title="searchResponseError.title"
      />
    </div>

    <SearchResultStatus
      v-if="emptyStatus && !searchResponseError && !isLoading"
      id="emptySearchResult"
      :response-error="{ title: emptyStatus }"
    >
      <slot name="newlink" />
    </SearchResultStatus>
  </div>
</template>

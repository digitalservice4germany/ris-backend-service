<script lang="ts" setup>
import dayjs from "dayjs"
import customParseFormat from "dayjs/plugin/customParseFormat"
import dayjsTimezone from "dayjs/plugin/timezone"
import dayjsUtc from "dayjs/plugin/utc"
import { computed, ref } from "vue"
import DocumentUnitListEntry from "../domain/documentUnitListEntry"
import Tooltip from "./Tooltip.vue"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import IconBadge from "@/components/IconBadge.vue"
import InfoModal from "@/components/InfoModal.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import PopupModal from "@/components/PopupModal.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import { useFeatureToggle } from "@/composables/useFeatureToggle"
import { useStatusBadge } from "@/composables/useStatusBadge"
import { PublicationState } from "@/domain/publicationStatus"
import { ResponseError } from "@/services/httpClient"
import IconAttachedFile from "~icons/ic/baseline-attach-file"
import IconCheck from "~icons/ic/baseline-check"
import IconDelete from "~icons/ic/baseline-close"
import IconError from "~icons/ic/baseline-error"
import IconSubject from "~icons/ic/baseline-subject"
import IconNote from "~icons/ic/outline-comment-bank"
import IconEdit from "~icons/ic/outline-edit"
import IconView from "~icons/ic/outline-remove-red-eye"
import IconClock from "~icons/ic/outline-watch-later"
import IconArrowDown from "~icons/mdi/arrow-down-drop"

const props = defineProps<{
  documentUnitListEntries?: DocumentUnitListEntry[]
  searchResponseError?: ResponseError
  isLoading?: boolean
  emptyState?: string
  showPublicationDate?: boolean
}>()
const emit = defineEmits<{
  deleteDocumentationUnit: [documentUnitListEntry: DocumentUnitListEntry]
  takeOverDocumentationUnit: [documentUnitListEntry: DocumentUnitListEntry]
}>()

dayjs.extend(dayjsUtc)
dayjs.extend(dayjsTimezone)
dayjs.extend(customParseFormat)

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

const attachmentText = (listEntry: DocumentUnitListEntry) =>
  listEntry.hasAttachments ? "Anhang vorhanden" : "Kein Anhang vorhanden"

const headNoteOrPrincipleText = (listEntry: DocumentUnitListEntry) =>
  listEntry.hasHeadnoteOrPrinciple
    ? "Kurztext vorhanden"
    : "Kein Kurztext vorhanden"

const trimText = (text: string, length: number = 50) =>
  text.length > length ? `${text.slice(0, length)}...` : text

const noteTooltip = (listEntry: DocumentUnitListEntry) =>
  listEntry.note ? trimText(listEntry.note) : "Keine Notiz vorhanden"

const schedulingTooltip = (publicationDate?: string) =>
  publicationDate
    ? `Terminierte Übergabe am\n${dayjs.utc(publicationDate).tz("Europe/Berlin").format("DD.MM.YYYY HH:mm")}`
    : "Keine Übergabe terminiert"

const publicationDate = (listEntry: DocumentUnitListEntry) => {
  const date =
    listEntry.scheduledPublicationDateTime ?? listEntry.lastPublicationDateTime
  if (date) {
    return dayjs.utc(date).tz("Europe/Berlin").format("DD.MM.YYYY HH:mm")
  } else {
    return "-"
  }
}

const schedulingFeatureToggle = useFeatureToggle("neuris.scheduledPublishing")

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
    emit("deleteDocumentationUnit", selectedDocumentUnitListEntry.value)
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
    <TableView
      class="relative table w-full border-separate"
      data-testid="documentUnitList"
    >
      <TableHeader>
        <CellHeaderItem class="w-[1%]"> Dokumentnummer</CellHeaderItem>
        <CellHeaderItem> Gerichtstyp</CellHeaderItem>
        <CellHeaderItem> Ort</CellHeaderItem>
        <CellHeaderItem>
          <div class="flex flex-row">
            Datum
            <IconArrowDown
              v-if="!showPublicationDate && schedulingFeatureToggle"
            />
          </div>
        </CellHeaderItem>
        <CellHeaderItem> Aktenzeichen</CellHeaderItem>
        <CellHeaderItem> Spruchkörper</CellHeaderItem>
        <CellHeaderItem> Typ</CellHeaderItem>
        <CellHeaderItem> Status</CellHeaderItem>
        <CellHeaderItem> Fehler</CellHeaderItem>
        <CellHeaderItem v-if="showPublicationDate && schedulingFeatureToggle">
          <div class="flex flex-row items-center">
            jDV Übergabe <IconArrowDown /></div
        ></CellHeaderItem>
        <CellHeaderItem />
      </TableHeader>
      <TableRow
        v-for="(listEntry, id) in listEntries"
        :key="id"
        :data-testid="`listEntry_${listEntry.documentNumber}`"
      >
        <CellItem>
          <FlexContainer align-items="items-center" class="space-x-8">
            <FlexItem class="flex-grow"
              >{{ listEntry.documentNumber }}
            </FlexItem>

            <Tooltip :text="attachmentText(listEntry)">
              <IconAttachedFile
                :aria-label="attachmentText(listEntry)"
                class="flex-end h-20 w-20"
                :class="
                  listEntry.hasAttachments ? 'text-blue-800' : 'text-gray-500'
                "
                data-testid="file-attached-icon"
              />
            </Tooltip>

            <Tooltip :text="headNoteOrPrincipleText(listEntry)">
              <IconSubject
                :aria-label="headNoteOrPrincipleText(listEntry)"
                class="flex-end flex h-20 w-20"
                :class="
                  listEntry.hasHeadnoteOrPrinciple
                    ? 'text-blue-800'
                    : 'text-gray-500'
                "
                data-testid="headnote-principle-icon"
              />
            </Tooltip>

            <Tooltip :text="noteTooltip(listEntry)">
              <IconNote
                :aria-label="noteTooltip(listEntry)"
                class="flex-end flex h-20 w-20"
                :class="!!listEntry.note ? 'text-blue-800' : 'text-gray-500'"
                data-testid="note-icon"
              />
            </Tooltip>

            <Tooltip
              v-if="schedulingFeatureToggle"
              :text="schedulingTooltip(listEntry.scheduledPublicationDateTime)"
            >
              <IconClock
                :aria-label="
                  schedulingTooltip(listEntry.scheduledPublicationDateTime)
                "
                class="flex-end flex h-20 w-20"
                :class="
                  listEntry.scheduledPublicationDateTime
                    ? 'text-blue-800'
                    : 'text-gray-500'
                "
                data-testid="scheduling-icon"
              />
            </Tooltip>
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
        <CellItem class="flex min-w-176 flex-row">
          <IconBadge
            v-if="listEntry.status?.publicationStatus"
            class="inline-flex"
            v-bind="useStatusBadge(listEntry.status).value"
            data-testid="publication-status"
          />
          <span
            v-if="
              listEntry.status?.publicationStatus ===
              PublicationState.EXTERNAL_HANDOVER_PENDING
            "
            class="ds-body-reg-02"
          >
            {{
              " aus " +
              listEntry.source +
              " (" +
              listEntry.creatingDocumentationOffice?.abbreviation +
              ")"
            }}
          </span>
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
        <CellItem
          v-if="showPublicationDate && schedulingFeatureToggle"
          data-testid="publicationDate"
        >
          {{ publicationDate(listEntry) }}
        </CellItem>
        <CellItem class="flex">
          <div class="float-end flex">
            <Tooltip
              v-if="
                listEntry.status?.publicationStatus ==
                'EXTERNAL_HANDOVER_PENDING'
              "
              text="Übernehmen"
            >
              <button
                aria-label="Dokumentationseinheit übernehmen"
                class="flex cursor-pointer border-2 border-r-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200 disabled:border-gray-600 disabled:text-gray-600"
                :disabled="!listEntry.isEditable"
                @click="emit('takeOverDocumentationUnit', listEntry)"
              >
                <IconCheck />
              </button>
            </Tooltip>
            <Tooltip v-else-if="listEntry.isEditable" text="Bearbeiten">
              <router-link
                aria-label="Dokumentationseinheit bearbeiten"
                class="flex cursor-pointer border-2 border-r-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                target="_blank"
                :to="{
                  name: 'caselaw-documentUnit-documentNumber-categories',
                  params: { documentNumber: listEntry.documentNumber },
                }"
              >
                <IconEdit />
              </router-link>
            </Tooltip>
            <div
              v-else
              aria-label="Dokumentationseinheit bearbeiten"
              class="border-2 border-r-0 border-solid border-gray-600 p-4 text-gray-600"
            >
              <IconEdit />
            </div>

            <Tooltip text="Vorschau">
              <router-link
                aria-label="Dokumentationseinheit ansehen"
                class="flex cursor-pointer border-2 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                target="_blank"
                :to="{
                  name: 'caselaw-documentUnit-documentNumber-preview',
                  params: { documentNumber: listEntry.documentNumber },
                }"
              >
                <IconView />
              </router-link>
            </Tooltip>
            <Tooltip v-if="listEntry.isDeletable" text="Löschen">
              <button
                aria-label="Dokumentationseinheit löschen"
                class="flex cursor-pointer border-2 border-l-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
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
            </Tooltip>
            <div
              v-else
              aria-label="Dokumentationseinheit löschen"
              class="flex border-2 border-l-0 border-solid border-gray-600 p-4 text-gray-600"
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

    <!-- Empty State -->
    <div
      v-if="emptyStatus && !searchResponseError && !isLoading"
      class="my-112 grid justify-items-center"
    >
      <span class="mb-16">{{ emptyStatus }}</span>
      <slot name="newlink" />
    </div>
  </div>
</template>

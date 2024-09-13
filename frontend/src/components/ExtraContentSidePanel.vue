<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRoute } from "vue-router"
import AttachmentView from "@/components/AttachmentView.vue"
import FileNavigator from "@/components/FileNavigator.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import InputField from "@/components/input/InputField.vue"
import TextAreaInput from "@/components/input/TextAreaInput.vue"
import TextButton from "@/components/input/TextButton.vue"
import DocumentUnitPreview from "@/components/preview/DocumentUnitPreview.vue"
import SideToggle, { OpeningDirection } from "@/components/SideToggle.vue"
import useQuery from "@/composables/useQueryFromRoute"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import IconAttachFile from "~icons/ic/baseline-attach-file"
import IconOpenInNewTab from "~icons/ic/outline-open-in-new"
import IconPreview from "~icons/ic/outline-remove-red-eye"
import IconStickyNote from "~icons/ic/outline-sticky-note-2"

const store = useDocumentUnitStore()

type SelectablePanelContent = "note" | "attachments" | "preview"
const selectedPanelContent = ref<SelectablePanelContent>(
  !store.documentUnit!.note && store.documentUnit!.hasAttachments
    ? "attachments"
    : "note",
)
const currentAttachmentIndex = ref(0)
const isExpanded = ref(false)

const route = useRoute()
const { pushQueryToRoute } = useQuery()

const hasNote = computed(() => {
  return !!store.documentUnit!.note && store.documentUnit!.note.length > 0
})

const hasAttachments = computed(() => {
  return (
    !!store.documentUnit!.attachments &&
    store.documentUnit!.attachments.length > 0
  )
})

/**
 * Updates the local attachment index reference, which is used to display the selected attachment in the panel,
 * if the panel content is set to "attachments".
 * @param index
 */
const handleOnSelectAttachment = (index: number) => {
  currentAttachmentIndex.value = index
}

/**
 * Sets the panel content to "note", so that the notes text input field is displayed in the panel.
 */
function selectNotes() {
  selectedPanelContent.value = "note"
}

/**
 * Sets the panel content to "attachments", so that the attachment view is displayed in the panel.
 * If a selected attachment index is provided, the local attachment index reference is updated accordingly,
 * so that the selected attachment is displayed in the attachment view.
 * @param selectedIndex (optional) selected attachment index
 */
function selectAttachments(selectedIndex?: number) {
  if (selectedIndex !== undefined) currentAttachmentIndex.value = selectedIndex
  selectedPanelContent.value = "attachments"
}

/**
 * Sets the panel content to "preview", so that the document preview is displayed in the panel.
 */
function selectPreview() {
  selectedPanelContent.value = "preview"
}

/**
 * Expands or collapses the panel.
 * Can be forced by passing a boolean parameter. Otherwise, it will collapse when expanded and expand when collapsed.
 * Pushes the state to the route as a query parameter.
 * @param expand optional boolean to enforce expanding or collapsing
 */
function togglePanel(expand?: boolean) {
  isExpanded.value = expand === undefined ? !isExpanded.value : expand
  pushQueryToRoute({
    ...route.query,
    showAttachmentPanel: isExpanded.value.toString(),
  })
}

/**
 * Adjusts the local attachment index reference if necessary.
 * If all attachments have been deleted, switches to display the note instead.
 * @param index the deleted attachment index
 */
function onAttachmentDeleted(index: number) {
  if (currentAttachmentIndex.value >= index) {
    currentAttachmentIndex.value = store.documentUnit!.attachments.length - 1
  }
  if (store.documentUnit!.attachments.length === 0) {
    selectNotes()
  }
}

/**
 * Exposes the functions "togglePanel", "selectAttachments" and "onAttachmentDeleted", so that they can be accessed from the parent component.
 * This is required to have smooth and explicit interactions between the DocumentUnitAttachments component and this component through their shared parent.
 */
defineExpose({ togglePanel, selectAttachments, onAttachmentDeleted })

/**
 * Checks whether the panel should be expanded when it is mounted.
 * If the showAttachmentPanel query parameter is present in the route, its value is taken. This parameter is only present,
 * after the user first interacts with the panel, by expanding or collapsing it manually.
 * This ensures that their selection does not get overridden.
 * If the query is not present, the panel is expanded by default if either a note, an attachment or both are present.
 * Otherwise, it is collapsed by default.
 */
onMounted(() => {
  if (route.query.showAttachmentPanel) {
    isExpanded.value = route.query.showAttachmentPanel === "true"
  } else {
    isExpanded.value = hasNote.value || hasAttachments.value
  }
})
</script>

<template>
  <FlexItem
    class="h-full flex-col border-l-1 border-solid border-gray-400 bg-white"
    :class="[isExpanded ? 'flex-1' : '', isExpanded ? 'w-1/2' : '']"
    data-testid="attachment-view-side-panel"
  >
    <SideToggle
      class="sticky top-[4rem] z-20 max-h-fit"
      :is-expanded="isExpanded"
      label="Seitenpanel"
      :opening-direction="OpeningDirection.LEFT"
      tabindex="0"
      @update:is-expanded="togglePanel"
    >
      <FlexContainer class="m-24 ml-16 items-center -space-x-2 px-8">
        <TextButton
          id="note"
          aria-label="Notiz anzeigen"
          button-type="tertiary"
          :class="selectedPanelContent === 'note' ? 'bg-blue-200' : ''"
          :icon="IconStickyNote"
          size="small"
          @click="() => selectNotes()"
        />

        <TextButton
          id="attachments"
          aria-label="Dokumente anzeigen"
          button-type="tertiary"
          :class="selectedPanelContent === 'attachments' ? 'bg-blue-200' : ''"
          :icon="IconAttachFile"
          size="small"
          @click="() => selectAttachments()"
        />

        <TextButton
          id="preview"
          aria-label="Vorschau anzeigen"
          button-type="tertiary"
          :class="selectedPanelContent === 'preview' ? 'bg-blue-200' : ''"
          :icon="IconPreview"
          size="small"
          @click="() => selectPreview()"
        />

        <div class="flex-grow" />

        <FileNavigator
          v-if="selectedPanelContent === 'attachments'"
          :attachments="store.documentUnit!.attachments"
          :current-index="currentAttachmentIndex"
          @select="handleOnSelectAttachment"
        ></FileNavigator>
        <router-link
          v-if="selectedPanelContent === 'preview'"
          aria-label="Vorschau in neuem Tab öffnen"
          target="_blank"
          :to="{
            name: 'caselaw-documentUnit-documentNumber-preview',
            params: { documentNumber: store.documentUnit!.documentNumber },
          }"
        >
          <TextButton
            button-type="ghost"
            :icon="IconOpenInNewTab"
            size="small"
          />
        </router-link>
      </FlexContainer>

      <div class="m-24">
        <div v-if="selectedPanelContent === 'note'">
          <InputField id="notesInput" v-slot="{ id }" label="Notiz">
            <TextAreaInput
              :id="id"
              v-model="store.documentUnit!.note"
              aria-label="Notiz Eingabefeld"
              autosize
              custom-classes="max-h-[65vh]"
            />
          </InputField>
        </div>
        <div v-if="selectedPanelContent === 'attachments'">
          <AttachmentView
            v-if="
              store.documentUnit!.uuid &&
              store.documentUnit!.attachments &&
              store.documentUnit!.attachments[currentAttachmentIndex]?.s3path
            "
            :document-unit-uuid="store.documentUnit!.uuid"
            :s3-path="
              store.documentUnit!.attachments[currentAttachmentIndex].s3path
            "
          />
          <div v-else class="ds-label-01-reg">
            Wenn eine Datei hochgeladen ist, können Sie die Datei hier sehen.
          </div>
        </div>
        <FlexContainer
          v-if="selectedPanelContent === 'preview'"
          class="max-h-[70vh] overflow-auto"
        >
          <DocumentUnitPreview
            :document-unit="store.documentUnit!"
            layout="narrow"
          />
        </FlexContainer>
      </div>
    </SideToggle>
  </FlexItem>
</template>

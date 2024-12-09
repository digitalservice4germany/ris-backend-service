<script setup lang="ts">
import { computed, onMounted, watch } from "vue"
import { useRoute } from "vue-router"
import Tooltip from "./Tooltip.vue"
import AttachmentView from "@/components/AttachmentView.vue"
import FileNavigator from "@/components/FileNavigator.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import InputField from "@/components/input/InputField.vue"
import TextAreaInput from "@/components/input/TextAreaInput.vue"
import TextButton from "@/components/input/TextButton.vue"
import DocumentUnitPreview from "@/components/preview/DocumentUnitPreview.vue"
import SideToggle, { OpeningDirection } from "@/components/SideToggle.vue"
import DocumentUnit from "@/domain/documentUnit"
import { useExtraContentSidePanelStore } from "@/stores/extraContentSidePanelStore"
import { SelectablePanelContent } from "@/types/panelContentMode"
import IconAttachFile from "~icons/ic/baseline-attach-file"
import IconEdit from "~icons/ic/outline-edit"
import IconOpenInNewTab from "~icons/ic/outline-open-in-new"
import IconPreview from "~icons/ic/outline-remove-red-eye"
import IconStickyNote from "~icons/ic/outline-sticky-note-2"

const props = defineProps<{
  enabledPanels?: SelectablePanelContent[]
  showEditButton?: boolean
  hidePanelModeBar?: boolean
  documentUnit?: DocumentUnit
}>()

const emit = defineEmits<{
  sidePanelIsExpanded: [boolean]
}>()

const store = useExtraContentSidePanelStore()

const route = useRoute()

const hasNote = computed(() => {
  return !!props.documentUnit!.note && props.documentUnit!.note.length > 0
})

const hasAttachments = computed(() => {
  return (
    !!props.documentUnit!.attachments &&
    props.documentUnit!.attachments.length > 0
  )
})

/**
 * Updates the local attachment index reference, which is used to display the selected attachment in the panel,
 * if the panel content is set to "attachments".
 * @param index
 */
const handleOnSelectAttachment = (index: number) => {
  store.currentAttachmentIndex = index
}

/**
 * Sets the panel content to "attachments", so that the attachment view is displayed in the panel.
 * If a selected attachment index is provided, the local attachment index reference is updated accordingly,
 * so that the selected attachment is displayed in the attachment view.
 * @param selectedIndex (optional) selected attachment index
 */
function selectAttachments(selectedIndex?: number) {
  store.selectAttachments(selectedIndex)
}

/**
 * Sets the panel content to "note", so that the notes text input field is displayed in the panel.
 */
function selectNotes() {
  store.setSidePanelMode("note")
}

/**
 * Sets the panel content to "preview", so that the document preview is displayed in the panel.
 */
function selectPreview() {
  store.setSidePanelMode("preview")
}

/**
 * Expands or collapses the panel.
 * Can be forced by passing a boolean parameter. Otherwise, it will collapse when expanded and expand when collapsed.
 * Pushes the state to the route as a query parameter.
 * @param expand optional boolean to enforce expanding or collapsing
 */
function togglePanel(expand?: boolean): boolean {
  return store.togglePanel(expand)
}

function setDefaultState() {
  if (!props.documentUnit!.note && props.documentUnit!.hasAttachments) {
    selectAttachments()
  } else {
    selectNotes()
  }
}

/**
 * Checks whether a selected side panel mode is excluded, and defaults to the first available panel if so.
 **/
watch(store, () => {
  if (props.enabledPanels) {
    if (!props.enabledPanels.includes(store.panelMode!)) {
      store.setSidePanelMode(props.enabledPanels[0])
    }
  }
  emit("sidePanelIsExpanded", store.isExpanded)
})

/**
 * Checks whether the panel should be expanded when it is mounted.
 * If the showAttachmentPanel query parameter is present in the route, its value is taken. This parameter is only present,
 * after the user first interacts with the panel, by expanding or collapsing it manually.
 * This ensures that their selection does not get overridden.
 * If the query is not present, the panel is expanded by default if either a note, an attachment or both are present.
 * Otherwise, it is collapsed by default.
 */
onMounted(() => {
  setDefaultState()
  if (route.query.showAttachmentPanel) {
    store.isExpanded = route.query.showAttachmentPanel === "true"
  } else {
    store.isExpanded = hasNote.value || hasAttachments.value
  }
})
</script>

<template>
  <FlexItem
    class="h-full flex-col border-l-1 border-solid border-gray-400 bg-white"
    :class="[store.isExpanded ? 'flex-1' : '', store.isExpanded ? 'w-1/2' : '']"
    data-testid="attachment-view-side-panel"
  >
    <SideToggle
      class="sticky top-[4rem] z-20 max-h-fit"
      :is-expanded="store.isExpanded"
      label="Seitenpanel"
      :opening-direction="OpeningDirection.LEFT"
      shortcut="<"
      tabindex="0"
      @update:is-expanded="togglePanel"
    >
      <FlexContainer class="m-24 ml-16 items-center -space-x-2 px-8">
        <div v-if="!enabledPanels || enabledPanels.includes('note')">
          <Tooltip v-if="!hidePanelModeBar" shortcut="n" text="Notiz">
            <TextButton
              id="note"
              aria-label="Notiz anzeigen"
              button-type="tertiary"
              class="flex"
              :class="store.panelMode === 'note' ? 'bg-blue-200' : ''"
              data-testid="note-button"
              :icon="IconStickyNote"
              size="small"
              @click="() => selectNotes()"
            />
          </Tooltip>
        </div>
        <div v-if="!enabledPanels || enabledPanels.includes('attachments')">
          <Tooltip v-if="!hidePanelModeBar" shortcut="d" text="Datei">
            <TextButton
              id="attachments"
              aria-label="Dokumente anzeigen"
              button-type="tertiary"
              :class="store.panelMode === 'attachments' ? 'bg-blue-200' : ''"
              data-testid="attachments-button"
              :icon="IconAttachFile"
              size="small"
              @click="() => selectAttachments()"
            />
          </Tooltip>
        </div>

        <div v-if="!enabledPanels || enabledPanels.includes('preview')">
          <Tooltip v-if="!hidePanelModeBar" shortcut="v" text="Vorschau">
            <TextButton
              id="preview"
              aria-label="Vorschau anzeigen"
              button-type="tertiary"
              :class="store.panelMode === 'preview' ? 'bg-blue-200' : ''"
              data-testid="preview-button"
              :icon="IconPreview"
              size="small"
              @click="() => selectPreview()"
            />
          </Tooltip>
        </div>

        <div class="flex-grow" />

        <FileNavigator
          v-if="store.panelMode === 'attachments'"
          :attachments="props.documentUnit!.attachments"
          :current-index="store.currentAttachmentIndex"
          @select="handleOnSelectAttachment"
        ></FileNavigator>
        <div v-if="showEditButton">
          <Tooltip v-if="props.documentUnit!.isEditable" text="Bearbeiten">
            <router-link
              aria-label="Dokumentationseinheit in einem neuen Tab bearbeiten"
              target="_blank"
              :to="{
                name: 'caselaw-documentUnit-documentNumber-categories',
                params: {
                  documentNumber: props.documentUnit!.documentNumber,
                },
              }"
            >
              <TextButton
                aria-label="Extra content side panel edit link button"
                button-type="ghost"
                :icon="IconEdit"
                size="small"
              />
            </router-link>
          </Tooltip>
          <div
            v-else
            aria-label="Dokumentationseinheit in einem neuen Tab bearbeiten"
          >
            <TextButton
              aria-label="Extra content side panel edit link button"
              button-type="ghost"
              disabled
              :icon="IconEdit"
              size="small"
            />
          </div>
        </div>

        <Tooltip text="In neuem Tab öffnen">
          <router-link
            v-if="store.panelMode === 'preview'"
            aria-label="Vorschau in neuem Tab öffnen"
            target="_blank"
            :to="{
              name: 'caselaw-documentUnit-documentNumber-preview',
              params: {
                documentNumber: props.documentUnit!.documentNumber,
              },
            }"
          >
            <TextButton
              button-type="ghost"
              :icon="IconOpenInNewTab"
              size="small"
            />
          </router-link>
        </Tooltip>
      </FlexContainer>

      <div class="m-24">
        <div v-if="store.panelMode === 'note'">
          <InputField id="notesInput" v-slot="{ id }" label="Notiz">
            <TextAreaInput
              :id="id"
              v-model="props.documentUnit!.note"
              aria-label="Notiz Eingabefeld"
              autosize
              custom-classes="max-h-[65vh]"
            />
          </InputField>
        </div>
        <div v-if="store.panelMode === 'attachments'">
          <AttachmentView
            v-if="
              props.documentUnit!.uuid &&
              props.documentUnit!.attachments &&
              props.documentUnit!.attachments[store.currentAttachmentIndex]
                ?.s3path
            "
            :document-unit-uuid="props.documentUnit!.uuid"
            :s3-path="
              props.documentUnit!.attachments[store.currentAttachmentIndex]
                .s3path
            "
          />
          <div v-else class="ds-label-01-reg">
            Wenn eine Datei hochgeladen ist, können Sie die Datei hier sehen.
          </div>
        </div>
        <FlexContainer
          v-if="store.panelMode === 'preview'"
          class="max-h-[70vh] overflow-auto"
        >
          <DocumentUnitPreview
            :document-unit="props.documentUnit!"
            layout="narrow"
          />
        </FlexContainer>
      </div>
    </SideToggle>
  </FlexItem>
</template>

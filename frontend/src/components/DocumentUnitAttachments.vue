<script lang="ts" setup>
import { useScrollLock } from "@vueuse/core"
import { storeToRefs } from "pinia"
import { computed, Ref, ref, watch } from "vue"
import AttachmentList from "@/components/AttachmentList.vue"
import FileUpload from "@/components/FileUpload.vue"
import FlexItem from "@/components/FlexItem.vue"
import InfoModal from "@/components/InfoModal.vue"
import PopupModal from "@/components/PopupModal.vue"
import TitleElement from "@/components/TitleElement.vue"
import attachmentService from "@/services/attachmentService"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const emit = defineEmits<{
  attachmentsUploaded: [boolean]
  attachmentIndexSelected: [number]
  attachmentIndexDeleted: [number]
}>()

const store = useDocumentUnitStore()
const { documentUnit } = storeToRefs(store)

const errors = ref<string[]>([])
const isLoading = ref(false)
const acceptedFileFormats = [".docx"]
const deletingAttachmentIndex: Ref<number> = ref(0)

const showDeleteModal = ref(false)
const deleteModalHeaderText = "Anhang löschen"

const errorTitle = computed(() => {
  if (errors.value.length === 1) {
    return "1 Datei konnte nicht hochgeladen werden."
  } else if (errors.value.length > 1) {
    return `${errors.value.length} Dateien konnten nicht hochgeladen werden.`
  } else return ""
})

const handleDeleteAttachment = async (index: number) => {
  const fileToDelete = attachments.value[index]
  if (fileToDelete.s3path == undefined) {
    console.error("file path is undefined", index)
    return
  }

  if (
    (
      await attachmentService.delete(
        documentUnit.value!.uuid,
        fileToDelete.s3path,
      )
    ).status < 300
  ) {
    emit("attachmentIndexDeleted", index)
    await store.loadDocumentUnit(store.documentUnit!.documentNumber!)
  }
}

const handleOnSelect = (index: number) => {
  if (index >= 0 && index < attachments.value.length) {
    emit("attachmentIndexSelected", index)
  }
}

const handleOnDelete = (index: number) => {
  errors.value = []
  deletingAttachmentIndex.value = index
  openDeleteModal()
}

const deleteFile = async (index: number) => {
  await handleDeleteAttachment(index)
  closeDeleteModal()
}

async function upload(files: FileList) {
  errors.value = []
  let anySuccessful = false
  try {
    for (const file of Array.from(files)) {
      isLoading.value = true
      const response = await attachmentService.upload(
        documentUnit.value!.uuid,
        file,
      )
      if (response.status === 200 && response.data) {
        anySuccessful = true
      } else if (response.error?.title) {
        errors.value.push(
          file.name +
            " " +
            response.error?.title +
            " " +
            response.error?.description,
        )
      }
    }
  } finally {
    isLoading.value = false
    emit("attachmentsUploaded", anySuccessful)
    await store.loadDocumentUnit(store.documentUnit!.documentNumber!)
  }
}

const scrollLock = useScrollLock(document)
watch(showDeleteModal, () => (scrollLock.value = showDeleteModal.value))

function openDeleteModal() {
  const attachmentToBeDeleted =
    attachments.value?.[deletingAttachmentIndex.value]?.name
  if (attachmentToBeDeleted != null) {
    showDeleteModal.value = true
  }
}

function closeDeleteModal() {
  showDeleteModal.value = false
}

const hasAttachments = computed<boolean>(() => {
  return store.documentUnit!.attachments.length > 0
})

const attachments = computed({
  get: () => store.documentUnit!.attachments,
  set: (newValues) => {
    store.documentUnit!.attachments = newValues
  },
})
</script>

<template>
  <FlexItem
    v-if="documentUnit"
    class="w-full flex-1 grow p-24"
    data-testid="document-unit-attachments"
  >
    <div class="flex w-full flex-1 grow flex-col gap-24">
      <PopupModal
        v-if="showDeleteModal"
        :aria-label="deleteModalHeaderText"
        cancel-button-type="tertiary"
        confirm-button-type="destructive"
        confirm-text="Löschen"
        :content-text="`Möchten Sie den Anhang ${attachments?.[deletingAttachmentIndex]?.name} wirklich dauerhaft löschen?`"
        :header-text="deleteModalHeaderText"
        @close-modal="closeDeleteModal"
        @confirm-action="deleteFile(deletingAttachmentIndex)"
      />
      <TitleElement>Dokumente</TitleElement>
      <AttachmentList
        v-if="hasAttachments"
        id="file-table"
        :files="attachments"
        @delete="handleOnDelete"
        @select="handleOnSelect"
      />
      <InfoModal
        v-if="errors.length > 0 && !isLoading"
        class="mt-8"
        :description="errors"
        :title="errorTitle"
      />
      <div class="flex-grow">
        <div class="flex h-full flex-col items-start">
          <FileUpload
            :accept="acceptedFileFormats.toString()"
            :is-loading="isLoading"
            @files-selected="(files) => upload(files)"
          />
        </div>
      </div>
      <div class="flex flex-row justify-between">
        <div class="ds-label-02-reg text-gray-900">
          Zulässige Dateiformate:
          {{ acceptedFileFormats.toString().replace(/\./g, " ") }}
        </div>
        <div class="ds-label-02-reg text-gray-900">
          Maximale Dateigröße: 20 MB
        </div>
      </div>
    </div>
  </FlexItem>
</template>

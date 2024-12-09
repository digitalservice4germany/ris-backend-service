<script setup lang="ts">
import { onBeforeMount, ref, watch } from "vue"
import LoadingSpinner from "./LoadingSpinner.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import TextEditor from "@/components/input/TextEditor.vue"
import { Docx2HTML } from "@/domain/docx2html"
import fileService from "@/services/attachmentService"

const props = defineProps<{
  documentUnitUuid?: string
  s3Path?: string
}>()

const isLoading = ref(false)

const fileAsHTML = ref<Docx2HTML>()

const getAttachmentHTML = async () => {
  isLoading.value = true
  if (props.documentUnitUuid && props.s3Path) {
    const htmlResponse = await fileService.getAttachmentAsHtml(
      props.documentUnitUuid,
      props.s3Path,
    )
    isLoading.value = false
    if (htmlResponse.error === undefined) fileAsHTML.value = htmlResponse.data
  }
}

onBeforeMount(async () => {
  await getAttachmentHTML()
})

watch(
  () => props.s3Path,
  async () => {
    await getAttachmentHTML()
  },
)
</script>

<template>
  <FlexContainer
    v-if="fileAsHTML?.html"
    v-bind="$attrs"
    id="attachment-view"
    class="sticky top-0 w-full gap-40 bg-white pb-0"
    flex-direction="flex-col"
  >
    <div
      v-if="isLoading"
      class="my-112 grid justify-items-center bg-white bg-opacity-60"
    >
      <LoadingSpinner />
    </div>
    <FlexItem v-else class="max-h-[70vh] min-h-[63vh] overflow-scroll">
      <TextEditor
        aria-label="Dokumentenvorschau"
        data-testid="text-editor"
        element-id="text-editor"
        field-size="max"
        plain-border-numbers
        :value="fileAsHTML?.html"
      />
    </FlexItem>
  </FlexContainer>
</template>

<style lang="scss" scoped>
.odoc-open {
  display: flex;
  height: 65px;
  align-items: center; // align vertical
  justify-content: center; // align horizontal
  border-radius: 10px;
  margin-right: 40px;
  transform: rotate(-90deg);
  transform-origin: right;
}

.odoc-open-text {
  margin-left: 30px;
}

.odoc-open-icon-background {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: white;
  transform: rotate(90deg) translateY(-25px);
}

.odoc-open-icon {
  margin-top: 8px;
  margin-right: 9px;
}
</style>

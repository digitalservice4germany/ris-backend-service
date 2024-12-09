<script lang="ts" setup>
import { onBeforeUnmount } from "vue"
import TextButton from "@/components/input/TextButton.vue"
import { useSaveToRemote } from "@/composables/useSaveToRemote"

const props = defineProps<{
  ariaLabel: string
}>()

const { saveIsInProgress, triggerSave, lastSaveError, formattedLastSavedOn } =
  useSaveToRemote(10_000)

const getErrorDetails = () =>
  lastSaveError.value?.title ? ": " + lastSaveError.value.title : ""

onBeforeUnmount(async function () {
  await triggerSave()
  window.onbeforeunload = null
})
window.onbeforeunload = function () {
  void triggerSave()
}
</script>

<template>
  <div class="ml-12 flex items-center space-x-[12px] whitespace-nowrap">
    <p v-if="lastSaveError !== undefined" class="ds-label-01-reg text-red-800">
      Fehler beim Speichern{{ getErrorDetails() }}
    </p>
    <p v-else-if="saveIsInProgress === true" class="ds-label-01-reg">
      speichern...
    </p>
    <p v-else-if="formattedLastSavedOn !== undefined" class="ds-label-01-reg">
      Zuletzt
      <span>{{ formattedLastSavedOn }}</span>
      Uhr
    </p>
    <TextButton
      :aria-label="props.ariaLabel"
      data-testid="save-button"
      label="Speichern"
      size="small"
      @click="triggerSave"
    />
  </div>
</template>

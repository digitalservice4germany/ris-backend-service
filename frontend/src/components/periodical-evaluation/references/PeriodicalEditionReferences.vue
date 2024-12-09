<script lang="ts" setup>
import { useInterval } from "@vueuse/core"
import { computed, ref, watch } from "vue"
import PeriodicalEditionReferenceInput from "./PeriodicalEditionReferenceInput.vue"
import PeriodicalEditionReferenceSummary from "./PeriodicalEditionReferenceSummary.vue"
import EditableList from "@/components/EditableList.vue"
import InfoModal from "@/components/InfoModal.vue"
import TitleElement from "@/components/TitleElement.vue"
import Reference from "@/domain/reference"
import { ResponseError } from "@/services/httpClient"
import { useEditionStore } from "@/stores/editionStore"

const store = useEditionStore()
const responseError = ref<ResponseError | undefined>()

const loadEditionIntervalCounter = useInterval(10_000, {})

const references = computed({
  get: () => store.edition?.references ?? [],
  set: async (newValues) => {
    await saveReferences(newValues)
  },
})

const defaultValue = new Reference() as Reference

async function saveReferences(references: Reference[]) {
  store.edition!.references = references
  const response = await store.saveEdition()
  if (response.error) {
    const message =
      "Fehler beim Speichern der Fundstellen. Bitte laden Sie die Seite neu."
    alert(message)
    responseError.value = {
      title: message,
    }
  }
}

/**
 * A watch to load document every x times, to make sure user has the latest version of references
 * which is critical for external changes
 */
watch(loadEditionIntervalCounter, async () => {
  await store.loadEdition()
})
</script>

<template>
  <div class="flex w-full p-24">
    <div class="flex w-full flex-col gap-24 bg-white p-24">
      <TitleElement data-testid="references-title">Fundstellen</TitleElement>
      <div v-if="responseError">
        <InfoModal
          :description="responseError.description"
          :title="responseError.title"
        />
      </div>
      <div aria-label="Fundstellen">
        <EditableList
          v-model="references"
          :default-value="defaultValue"
          :edit-component="PeriodicalEditionReferenceInput"
          :summary-component="PeriodicalEditionReferenceSummary"
        />
      </div>
    </div>
  </div>
</template>

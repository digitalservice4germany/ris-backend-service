<script lang="ts" setup>
import { ref, computed } from "vue"
import ChipsInput from "@/components/input/ChipsInput.vue"
import TextButton from "@/components/input/TextButton.vue"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import IconAdd from "~icons/material-symbols/add"

const errorMessage = ref<ResponseError>()

const store = useDocumentUnitStore()

const jobProfiles = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.jobProfiles,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.jobProfiles = newValues
  },
})

const shouldDisplay = ref<boolean>(
  store.documentUnit?.contentRelatedIndexing?.jobProfiles
    ? store.documentUnit?.contentRelatedIndexing?.jobProfiles?.length > 0
    : false,
)

function toggle() {
  shouldDisplay.value = !shouldDisplay.value
}
</script>

<template>
  <hr class="ml-32 mr-32 border-blue-300" />
  <div v-if="shouldDisplay" class="p-32">
    <h2 class="ds-heading-03-reg mb-24">Berufsbild</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <ChipsInput
          id="keywords"
          v-model="jobProfiles"
          aria-label="Berufsbild"
          :error="errorMessage"
        ></ChipsInput>
      </div>
    </div>
  </div>
  <TextButton
    v-else
    button-type="tertiary"
    class="m-32"
    :icon="IconAdd"
    label="Berufsbild anzeigen"
    size="small"
    @click="toggle"
  />
</template>

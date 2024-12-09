<script lang="ts" setup>
import { computed } from "vue"
import ChipsInput from "@/components/input/ChipsInput.vue"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const dismissalTypes = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.dismissalTypes,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.dismissalTypes = newValues
  },
})
const dismissalGrounds = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.dismissalGrounds,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.dismissalGrounds = newValues
  },
})
</script>

<template>
  <div class="flex flex-row gap-24">
    <div class="basis-1/2 gap-0">
      <div class="ds-label-02-reg mb-4" data-testid="dismissal-types">
        Kündigungsarten
      </div>
      <ChipsInput
        id="dismissalTypes"
        v-model="dismissalTypes"
        aria-label="Kündigungsarten"
      ></ChipsInput>
    </div>
    <div class="basis-1/2 gap-0">
      <div class="ds-label-02-reg mb-4" data-testid="dismissal-grounds">
        Kündigungsgründe
      </div>
      <ChipsInput
        id="dismissalGrounds"
        v-model="dismissalGrounds"
        aria-label="Kündigungsgründe"
      ></ChipsInput>
    </div>
  </div>
</template>

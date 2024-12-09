<script lang="ts" setup>
import ChipsInput from "@/components/input/ChipsInput.vue"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

defineProps<{
  label: string
}>()

const store = useDocumentUnitStore()
</script>

<template>
  <div class="gap-0">
    <div class="ds-label-02-reg mb-4" data-testid="Berufsbild">
      {{ label }}
    </div>
    <ChipsInput
      id="jobProfiles"
      v-model="store.documentUnit!.contentRelatedIndexing.jobProfiles"
      aria-label="Berufsbild"
    ></ChipsInput>
  </div>
</template>

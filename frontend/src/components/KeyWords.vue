<script lang="ts" setup>
import { computed } from "vue"
import ListInput from "@/components/input/listInput/ListInput.vue"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const emit = defineEmits<{
  reset: []
}>()

const store = useDocumentUnitStore()

const keywords = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.keywords ?? [],
  set: (newValues: string[]) => {
    store.documentUnit!.contentRelatedIndexing.keywords = newValues
  },
})
</script>

<template>
  <div>
    <h2 class="ds-label-01-bold mb-16">Schlagwörter</h2>
    <ListInput v-model="keywords" label="Schlagwörter" @reset="emit('reset')" />
  </div>
</template>

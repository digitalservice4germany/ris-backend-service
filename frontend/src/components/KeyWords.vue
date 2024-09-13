<script lang="ts" setup>
import { ref, computed } from "vue"
import KeywordsChipsInput from "@/components/input/KeywordsChipsInput.vue"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const errorMessage = ref<ResponseError>()

const store = useDocumentUnitStore()

const keywords = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.keywords,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.keywords = newValues
  },
})
</script>

<template>
  <div class="p-32">
    <h2 class="ds-heading-03-reg mb-24">Schlagwörter</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <KeywordsChipsInput
          id="keywords"
          v-model="keywords"
          aria-label="Schlagwörter"
          :error="errorMessage"
        ></KeywordsChipsInput>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from "vue"
import DocumentUnitList from "@/components/DocumentUnitList.vue"
import { Procedure } from "@/domain/documentUnit"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import { ResponseError } from "@/services/httpClient"

const props = defineProps<{
  procedure: Procedure
  responseError?: ResponseError
}>()

const emit = defineEmits<{
  deleteDocumentUnit: [
    documentUnitListEntry: DocumentUnitListEntry,
    procedure: Procedure,
  ]
}>()

/**
 * Sets loading state to true, when mismatch between the documentationUnitCount and the actual loaded documentUnits
 */
const isLoading = computed(
  () =>
    !props.procedure.documentUnits &&
    props.procedure.documentationUnitCount > 0,
)
</script>

<template>
  <div
    v-if="procedure.documentationUnitCount > 0"
    class="pb-12 pl-24 pr-48 pt-36"
  >
    <DocumentUnitList
      class="grow"
      :document-unit-list-entries="procedure.documentUnits"
      :is-loading="isLoading"
      :search-response-error="responseError"
      @delete-document-unit="emit('deleteDocumentUnit', $event, procedure)"
    >
    </DocumentUnitList>
  </div>
  <div v-else class="pb-12 pl-40 pr-48 pt-36">
    <div class="flex w-full items-center justify-center bg-blue-200 py-16">
      <span>Keine Dokeinheiten sind zugewiesen.</span>
    </div>
  </div>
</template>

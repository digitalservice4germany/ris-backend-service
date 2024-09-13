<script lang="ts" setup>
import { computed } from "vue"
import DocumentUnitReferenceInput from "@/components/DocumentUnitReferenceInput.vue"
import EditableList from "@/components/EditableList.vue"
import ReferenceSummary from "@/components/ReferenceSummary.vue"
import Reference from "@/domain/reference"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const references = computed({
  get: () => store.documentUnit!.references as Reference[],
  set: (newValues) => {
    store.documentUnit!.references = newValues
  },
})

const defaultValue = new Reference() as Reference
</script>

<template>
  <div aria-label="Fundstellen" class="flex-1 bg-white p-32">
    <h2 class="ds-heading-03-reg mb-24">Fundstellen bearbeiten</h2>
    <div class="flex flex-row">
      <EditableList
        v-model="references"
        :default-value="defaultValue"
        :edit-component="DocumentUnitReferenceInput"
        :summary-component="ReferenceSummary"
      />
    </div>
  </div>
</template>

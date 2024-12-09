<script lang="ts" setup>
import { computed } from "vue"
import ActiveCitationInput from "@/components/ActiveCitationInput.vue"
import DocumentUnitDecisionSummary from "@/components/DocumentUnitDecisionSummary.vue"
import EditableList from "@/components/EditableList.vue"
import ActiveCitation from "@/domain/activeCitation"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const activeCitations = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.activeCitations,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.activeCitations = newValues
  },
})

const defaultValue = new ActiveCitation() as ActiveCitation
</script>

<template>
  <div aria-label="Aktivzitierung">
    <h2 class="ds-label-01-bold mb-16">Aktivzitierung</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <EditableList
          v-model="activeCitations"
          :default-value="defaultValue"
          :edit-component="ActiveCitationInput"
          :summary-component="DocumentUnitDecisionSummary"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from "vue"
import EnsuingDecisionInputGroup from "./EnsuingDecisionInputGroup.vue"
import DocumentationUnitDecisionSummary from "@/components/DocumentUnitDecisionSummary.vue"
import EditableList from "@/components/EditableList.vue"
import EnsuingDecision from "@/domain/ensuingDecision"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const ensuingDecisions = computed({
  get: () => store.documentUnit!.ensuingDecisions as EnsuingDecision[],
  set: (newValues) => {
    store.documentUnit!.ensuingDecisions = newValues
  },
})

const defaultValue = new EnsuingDecision() as EnsuingDecision
</script>
<template>
  <div
    aria-label="Nachgehende Entscheidung"
    class="flex flex-col bg-white p-32"
  >
    <h2 class="ds-heading-03-reg mb-24">Nachgehende Entscheidungen</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <EditableList
          v-model="ensuingDecisions"
          :default-value="defaultValue"
          :edit-component="EnsuingDecisionInputGroup"
          :summary-component="DocumentationUnitDecisionSummary"
        />
      </div>
    </div>
  </div>
</template>

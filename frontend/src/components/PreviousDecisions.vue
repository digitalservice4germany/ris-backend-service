<script lang="ts" setup>
import { computed } from "vue"
import PreviousDecisionInputGroup from "./PreviousDecisionInputGroup.vue"
import DocumentUnitDecisionSummary from "@/components/DocumentUnitDecisionSummary.vue"
import EditableList from "@/components/EditableList.vue"
import PreviousDecision from "@/domain/previousDecision"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const previousDecisions = computed({
  get: () => store.documentUnit!.previousDecisions as PreviousDecision[],
  set: (newValues) => {
    store.documentUnit!.previousDecisions = newValues
  },
})

const defaultValue = new PreviousDecision() as PreviousDecision
</script>

<template>
  <div class="flex flex-col gap-24 bg-white p-32 pb-0">
    <h2 class="ds-heading-03-bold">Rechtszug</h2>
    <div aria-label="Vorgehende Entscheidung">
      <h2 class="ds-heading-03-reg mb-24">Vorgehende Entscheidungen</h2>
      <div class="flex flex-row">
        <div class="flex-1">
          <EditableList
            v-model="previousDecisions"
            :default-value="defaultValue"
            :edit-component="PreviousDecisionInputGroup"
            :summary-component="DocumentUnitDecisionSummary"
          />
        </div>
      </div>
    </div>
  </div>
</template>

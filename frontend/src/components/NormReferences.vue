<script lang="ts" setup>
import * as Sentry from "@sentry/vue"
import { computed } from "vue"
import EditableList from "@/components/EditableList.vue"
import NormReferenceInput from "@/components/NormReferenceInput.vue"
import NormReferenceSummary from "@/components/NormReferenceSummary.vue"
import NormReference from "@/domain/normReference"

import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const norms = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.norms,
  set: async (newValues) => {
    store.documentUnit!.contentRelatedIndexing.norms = newValues?.filter(
      (value) => {
        if (Object.keys(value).length === 0) {
          Sentry.captureMessage(
            "NormReference list contains empty objects",
            "error",
          )
          return false
        }
        // Remove duplicate singleNorms within the current value (norm)
        if (value.singleNorms && Array.isArray(value.singleNorms)) {
          const uniqueSingleNorms = new Set() // Use a Set to track unique serialized singleNorms

          value.singleNorms = value.singleNorms.filter((singleNorm) => {
            // Serialize singleNorm values into a unique string
            const uniqueKey = JSON.stringify({
              singleNorm: singleNorm.singleNorm,
              dateOfVersion: singleNorm.dateOfVersion,
              dateOfRelevance: singleNorm.dateOfRelevance,
            })

            // Add to the Set if it doesn't exist yet
            const isUnique = !uniqueSingleNorms.has(uniqueKey)
            if (isUnique) {
              uniqueSingleNorms.add(uniqueKey)
            }

            return isUnique // Filter out duplicates
          })
        }
        return true // Keep the value in the norms array
      },
    )
    await store.updateDocumentUnit()
  },
})

const defaultValue = new NormReference() as NormReference
</script>
<template>
  <div aria-label="Norm">
    <h2 class="ds-label-01-bold mb-16">Normen</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <EditableList
          v-model="norms"
          :default-value="defaultValue"
          :edit-component="NormReferenceInput"
          :summary-component="NormReferenceSummary"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from "pinia"
import { computed, ref, toRefs } from "vue"
import { useRoute } from "vue-router"
import DocumentUnitContentRelatedIndexing from "@/components/DocumentUnitContentRelatedIndexing.vue"
import DocumentUnitCoreData from "@/components/DocumentUnitCoreData.vue"
import { DocumentUnitCatagoriesEnum } from "@/components/enumDocumentUnitCatagories"
import FlexItem from "@/components/FlexItem.vue"
import ProceedingDecisions from "@/components/ProceedingDecisions.vue"
import DocumentUnitTexts from "@/components/texts/DocumentUnitTexts.vue"

import { useProvideCourtType } from "@/composables/useCourtType"
import { useInternalUser } from "@/composables/useInternalUser"
import { useScrollToHash } from "@/composables/useScrollToHash"
import constitutionalCourtTypes from "@/data/constitutionalCourtTypes.json"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const route = useRoute()
const store = useDocumentUnitStore()
const { documentUnit } = storeToRefs(store)
const courtTypeRef = ref<string>(documentUnit.value!.coreData.court?.type ?? "")

/**
 * Determines whether legal forces should be deleted based on the court type and presence of a selected court.
 * @returns boolean
 */
const shouldDeleteLegalForces = computed(() => {
  return (
    !constitutionalCourtTypes.items.includes(courtTypeRef.value) ||
    !documentUnit.value!.coreData.court
  )
})

/**
 * Deletes the legal forces from all single norms in the norms of the updated document unit.
 */
function deleteLegalForces() {
  const norms = documentUnit.value!.contentRelatedIndexing.norms

  norms?.forEach((norm) => {
    norm.singleNorms = norm.singleNorms?.filter((singleNorm) => {
      if (singleNorm.legalForce) {
        singleNorm.legalForce = undefined
      }
      return !singleNorm.isEmpty
    })
  })
}

const coreData = computed({
  get: () => store.documentUnit!.coreData,
  set: async (newValues) => {
    store.documentUnit!.coreData = newValues
    courtTypeRef.value = store.documentUnit!.coreData.court?.type ?? ""
    // When the user changes the court to one that doesn't allow "Gesetzeskraft" all existing legal forces are deleted
    if (shouldDeleteLegalForces.value) {
      deleteLegalForces()
    }
  },
})

const { hash: routeHash } = toRefs(route)
const headerOffset = 145
useScrollToHash(routeHash, headerOffset)

useProvideCourtType(courtTypeRef)

const isInternalUser = useInternalUser()
</script>

<template>
  <FlexItem class="w-full flex-1 grow flex-col gap-24 p-24">
    <DocumentUnitCoreData
      v-if="isInternalUser"
      :id="DocumentUnitCatagoriesEnum.CORE_DATA"
      v-model="coreData"
    />
    <ProceedingDecisions />
    <DocumentUnitContentRelatedIndexing
      :id="DocumentUnitCatagoriesEnum.CONTENT_RELATED_INDEXING"
    />
    <DocumentUnitTexts :id="DocumentUnitCatagoriesEnum.TEXTS" />
  </FlexItem>
</template>

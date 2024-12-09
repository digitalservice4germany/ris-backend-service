<script setup lang="ts">
import { ref } from "vue"
import ActiveCitations from "@/components/ActiveCitations.vue"
import CategoryWrapper from "@/components/CategoryWrapper.vue"
import FieldsOfLaw from "@/components/field-of-law/FieldsOfLaw.vue"
import KeyWords from "@/components/KeyWords.vue"
import Norms from "@/components/NormReferences.vue"
import OtherCategories from "@/components/OtherCategories.vue"
import TitleElement from "@/components/TitleElement.vue"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const hasKeywords = ref<boolean>(
  !!store.documentUnit?.contentRelatedIndexing?.keywords &&
    store.documentUnit?.contentRelatedIndexing?.keywords?.length > 0,
)
</script>

<template>
  <div class="flex flex-col gap-24 bg-white p-24">
    <TitleElement>Inhaltliche Erschließung</TitleElement>
    <CategoryWrapper
      v-slot="slotProps"
      label="Schlagwörter"
      :should-show-button="!hasKeywords"
    >
      <KeyWords @reset="slotProps.reset" />
    </CategoryWrapper>
    <FieldsOfLaw />
    <Norms />
    <ActiveCitations />
    <OtherCategories />
  </div>
</template>

<script setup lang="ts">
import dayjs from "dayjs"
import { storeToRefs } from "pinia"
import { provide } from "vue"
import FlexContainer from "@/components/FlexContainer.vue"
import {
  PreviewLayout,
  previewLayoutInjectionKey,
} from "@/components/preview/constants"
import PreviewContentRelatedIndexing from "@/components/preview/PreviewContentRelatedIndexing.vue"
import PreviewCoreData from "@/components/preview/PreviewCoreData.vue"
import PreviewLongTexts from "@/components/preview/PreviewLongTexts.vue"
import PreviewNote from "@/components/preview/PreviewNote.vue"
import PreviewProceedingDecisions from "@/components/preview/PreviewProceedingDecisions.vue"
import PreviewReferences from "@/components/preview/PreviewReferences.vue"
import PreviewShortTexts from "@/components/preview/PreviewShortTexts.vue"
import { LongTexts } from "@/domain/documentUnit"
import Reference from "@/domain/reference"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const props = defineProps<{
  layout?: PreviewLayout
}>()

const { documentUnit } = storeToRefs(useDocumentUnitStore())

provide(previewLayoutInjectionKey, props.layout || "wide")
</script>

<template>
  <FlexContainer
    v-if="documentUnit"
    class="max-w-screen-xl"
    data-testid="preview"
    flex-direction="flex-col"
  >
    <h1 class="ds-heading-03-bold mt-16 px-16">
      {{ documentUnit.documentNumber }}
    </h1>
    <p class="ds-label-03-reg mb-16 px-16">
      Vorschau erstellt am {{ dayjs(new Date()).format("DD.MM.YYYY") }} um
      {{ dayjs(new Date()).format("HH:mm:ss") }}
    </p>
    <PreviewCoreData :core-data="documentUnit.coreData" />
    <PreviewNote :note="documentUnit.note" />
    <PreviewProceedingDecisions />
    <PreviewContentRelatedIndexing />
    <PreviewReferences :references="documentUnit.references as Reference[]" />
    <PreviewShortTexts
      :short-texts="documentUnit.shortTexts"
      :valid-border-numbers="documentUnit.managementData.borderNumbers"
    />
    <PreviewLongTexts :long-texts="documentUnit.longTexts as LongTexts" />
  </FlexContainer>
</template>

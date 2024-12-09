<script setup lang="ts">
import { computed } from "vue"
import TextEditor from "@/components/input/TextEditor.vue"
import PreviewCategory from "@/components/preview/PreviewCategory.vue"
import PreviewContent from "@/components/preview/PreviewContent.vue"
import PreviewRow from "@/components/preview/PreviewRow.vue"
import { useValidBorderNumberLinks } from "@/composables/useValidBorderNumberLinks"
import { shortTextLabels, ShortTexts } from "@/domain/documentUnit"

const props = defineProps<{
  shortTexts: ShortTexts
  validBorderNumbers: string[]
}>()

const decisionName = computed(() =>
  props.shortTexts.decisionName
    ? useValidBorderNumberLinks(
        props.shortTexts.decisionName,
        props.validBorderNumbers,
      )
    : undefined,
)
const headline = computed(() =>
  props.shortTexts.headline
    ? useValidBorderNumberLinks(
        props.shortTexts.headline,
        props.validBorderNumbers,
      )
    : undefined,
)
const guidingPrinciple = computed(() =>
  props.shortTexts.guidingPrinciple
    ? useValidBorderNumberLinks(
        props.shortTexts.guidingPrinciple,
        props.validBorderNumbers,
      )
    : undefined,
)
const headnote = computed(() =>
  props.shortTexts.headnote
    ? useValidBorderNumberLinks(
        props.shortTexts.headnote,
        props.validBorderNumbers,
      )
    : undefined,
)
const otherHeadnote = computed(() =>
  props.shortTexts.otherHeadnote
    ? useValidBorderNumberLinks(
        props.shortTexts.otherHeadnote,
        props.validBorderNumbers,
      )
    : undefined,
)
</script>

<template>
  <PreviewRow v-if="decisionName">
    <PreviewCategory>{{ shortTextLabels.decisionName }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewDecisionName"
        :aria-label="shortTextLabels.decisionName"
        field-size="max"
        preview
        :value="decisionName"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="headline">
    <PreviewCategory>{{ shortTextLabels.headline }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewHeadline"
        :aria-label="shortTextLabels.headline"
        field-size="max"
        preview
        :value="headline"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="guidingPrinciple">
    <PreviewCategory>{{ shortTextLabels.guidingPrinciple }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewGuidingPrinciple"
        :aria-label="shortTextLabels.guidingPrinciple"
        field-size="max"
        preview
        :value="guidingPrinciple"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="headnote">
    <PreviewCategory>{{ shortTextLabels.headnote }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewHeadnote"
        :aria-label="shortTextLabels.headnote"
        field-size="max"
        preview
        :value="headnote"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="otherHeadnote">
    <PreviewCategory>{{ shortTextLabels.otherHeadnote }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewItherHeadnote"
        :aria-label="shortTextLabels.otherHeadnote"
        field-size="max"
        preview
        :value="otherHeadnote"
      />
    </PreviewContent>
  </PreviewRow>
</template>

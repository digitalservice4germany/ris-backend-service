<script setup lang="ts">
import TextEditor from "@/components/input/TextEditor.vue"
import PreviewCategory from "@/components/preview/PreviewCategory.vue"
import PreviewContent from "@/components/preview/PreviewContent.vue"
import PreviewRow from "@/components/preview/PreviewRow.vue"
import { longTextLabels, LongTexts } from "@/domain/documentUnit"

defineProps<{
  longTexts: LongTexts
}>()
</script>

<template>
  <PreviewRow v-if="longTexts.outline">
    <PreviewCategory>{{ longTextLabels.outline }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewOutline"
        :aria-label="longTextLabels.outline"
        field-size="max"
        preview
        :value="longTexts.outline"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.tenor">
    <PreviewCategory>{{ longTextLabels.tenor }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewTenor"
        :aria-label="longTextLabels.tenor"
        field-size="max"
        preview
        :value="longTexts.tenor"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.reasons">
    <PreviewCategory>{{ longTextLabels.reasons }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewReasons"
        :aria-label="longTextLabels.reasons"
        field-size="max"
        preview
        :value="longTexts.reasons"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.caseFacts">
    <PreviewCategory>{{ longTextLabels.caseFacts }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewCaseFacts"
        :aria-label="longTextLabels.caseFacts"
        field-size="max"
        preview
        :value="longTexts.caseFacts"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.decisionReasons">
    <PreviewCategory>{{ longTextLabels.decisionReasons }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewDecisionReasons"
        :aria-label="longTextLabels.decisionReasons"
        field-size="max"
        preview
        :value="longTexts.decisionReasons"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.otherLongText">
    <PreviewCategory>{{ longTextLabels.otherLongText }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewOtherLongText"
        :aria-label="longTextLabels.otherLongText"
        field-size="max"
        preview
        :value="longTexts.otherLongText"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.dissentingOpinion">
    <PreviewCategory>{{ longTextLabels.dissentingOpinion }}</PreviewCategory>
    <PreviewContent>
      <TextEditor
        id="previewDissentingOpinion"
        :aria-label="longTextLabels.dissentingOpinion"
        field-size="max"
        preview
        :value="longTexts.dissentingOpinion"
      />
    </PreviewContent>
  </PreviewRow>
  <PreviewRow v-if="longTexts.participatingJudges?.length">
    <PreviewCategory>{{ longTextLabels.participatingJudges }}</PreviewCategory>
    <PreviewContent>
      <div
        v-for="participatingJudge in longTexts.participatingJudges"
        :key="participatingJudge.id"
      >
        {{ participatingJudge.renderDecision }}
      </div>
    </PreviewContent>
  </PreviewRow>
</template>

<script setup lang="ts">
import { computed } from "vue"
import PreviewCategory from "@/components/preview/PreviewCategory.vue"
import PreviewContent from "@/components/preview/PreviewContent.vue"
import PreviewRow from "@/components/preview/PreviewRow.vue"
import Reference from "@/domain/reference"

const props = defineProps<{
  references: Reference[] | undefined
}>()

const primaryReferences = computed(() =>
  props.references?.filter(
    (reference) =>
      reference.referenceType === "caselaw" &&
      reference.legalPeriodical?.primaryReference,
  ),
)
const secondaryReferences = computed(() =>
  props.references?.filter(
    (reference) =>
      reference.referenceType === "caselaw" &&
      !reference.legalPeriodical?.primaryReference,
  ),
)

const literatureReferences = computed(() =>
  props.references?.filter(
    (reference) => reference.referenceType === "literature",
  ),
)
</script>

<template>
  <PreviewRow
    v-if="
      (primaryReferences && primaryReferences.length > 0) ||
      (secondaryReferences && secondaryReferences.length > 0)
    "
    data-testid="references-preview"
  >
    <PreviewCategory>Fundstellen</PreviewCategory>
    <PreviewContent>
      <div v-if="primaryReferences && primaryReferences.length > 0">
        <span class="ds-body-01-bold">Primäre Fundstellen</span>

        <div
          v-for="item in primaryReferences"
          :key="item.legalPeriodical?.uuid"
        >
          {{ item.renderDecision }}
        </div>
      </div>
      <div
        v-if="
          primaryReferences &&
          primaryReferences.length > 0 &&
          secondaryReferences &&
          secondaryReferences.length > 0
        "
      >
        <br />
      </div>
      <div
        v-if="secondaryReferences && secondaryReferences.length > 0"
        class="pt-4"
      >
        <span class="ds-body-01-bold">Sekundäre Fundstellen</span>
        <div
          v-for="item in secondaryReferences"
          :key="item.legalPeriodical?.uuid"
        >
          {{ item.renderDecision }}
        </div>
      </div>
    </PreviewContent>
  </PreviewRow>
  <PreviewRow
    v-if="literatureReferences && literatureReferences.length > 0"
    data-testid="literature-references-preview"
  >
    <PreviewCategory>Literaturfundstellen</PreviewCategory>

    <PreviewContent>
      <div v-for="item in literatureReferences" :key="item.id">
        {{ item.renderDecision }}
      </div>
    </PreviewContent>
  </PreviewRow>
</template>

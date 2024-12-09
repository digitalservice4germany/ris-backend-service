<script lang="ts" setup>
import DecisionSummary from "@/components/DecisionSummary.vue"
import { DisplayMode } from "@/components/enumDisplayMode"
import FlexContainer from "@/components/FlexContainer.vue"
import Reference from "@/domain/reference"

const props = defineProps<{
  data: Reference
}>()
</script>

<template>
  <div class="flex w-full justify-between" data-testid="reference-list-summary">
    <div
      v-if="props.data.documentationUnit?.documentNumber"
      class="flex flex-col"
    >
      <div class="ds-label-01-bold mr-8" data-testid="citation-summary">
        {{ props.data.renderDecision }}
      </div>
      <FlexContainer flex-direction="flex-row">
        <DecisionSummary
          :decision="props.data.documentationUnit"
          :display-mode="DisplayMode.SIDEPANEL"
        />
      </FlexContainer>
    </div>
  </div>
</template>

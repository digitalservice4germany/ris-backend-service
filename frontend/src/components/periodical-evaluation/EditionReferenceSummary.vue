<script lang="ts" setup>
import DecisionSummary from "@/components/DecisionSummary.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import Reference from "@/domain/reference"

const props = defineProps<{
  data: Reference
}>()
</script>

<template>
  <div class="flex w-full justify-between">
    <div class="flex flex-col">
      <RouterLink
        v-if="props.data.documentationUnit?.documentNumber"
        tabindex="-1"
        target="_blank"
        :to="{
          name: 'caselaw-documentUnit-documentNumber-preview',
          params: {
            documentNumber: props.data.documentationUnit?.documentNumber,
          },
        }"
      >
        <FlexContainer flex-direction="flex-row">
          <DecisionSummary :decision="props.data.documentationUnit" />
        </FlexContainer>
      </RouterLink>

      <div class="ds-label-01-reg mr-8">
        {{ props.data.renderDecision }}
      </div>
    </div>
  </div>
</template>

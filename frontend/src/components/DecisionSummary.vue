<script setup lang="ts">
import DocumentNumberPreviewLink from "@/components/DocumentNumberLink.vue"
import { DisplayMode } from "@/components/enumDisplayMode"
import FlexContainer from "@/components/FlexContainer.vue"
import RelatedDocumentation from "@/domain/relatedDocumentation"

type Props<T extends RelatedDocumentation> = {
  decision: T
  displayMode?: DisplayMode
}

const props = defineProps<Props<RelatedDocumentation>>()
</script>

<template>
  <FlexContainer
    :data-testid="'decision-summary-' + props.decision.documentNumber"
  >
    {{ decision.renderDecision }}
    <DocumentNumberPreviewLink
      :decision="props.decision"
      :display-mode="displayMode"
    />
  </FlexContainer>
</template>

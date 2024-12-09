<script setup lang="ts">
import { RouterLink } from "vue-router"
import { DisplayMode } from "@/components/enumDisplayMode"
import FlexContainer from "@/components/FlexContainer.vue"
import LinkElement from "@/components/LinkElement.vue"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import { useExtraContentSidePanelStore } from "@/stores/extraContentSidePanelStore"

interface Props {
  decision: RelatedDocumentation
  displayMode?: DisplayMode
}

const props = withDefaults(defineProps<Props>(), {
  displayMode: DisplayMode.TAB,
})

const documentUnitStore = useDocumentUnitStore()
const extraContentSidePanelStore = useExtraContentSidePanelStore()

async function openSidePanel(documentUnitNumber?: string) {
  if (documentUnitNumber) {
    await documentUnitStore.loadDocumentUnit(documentUnitNumber)
    extraContentSidePanelStore.togglePanel(true)
    extraContentSidePanelStore.setSidePanelMode("preview")
  }
}
</script>

<template>
  <FlexContainer
    align-items="items-center"
    :data-testid="'document-number-link-' + props.decision.fileNumber"
  >
    <span class="ds-label-01-reg ml-8 mr-8">|</span>
    <div v-if="decision.hasForeignSource" class="pt-3">
      <div v-if="props.displayMode === DisplayMode.TAB">
        <RouterLink
          v-if="props.decision.documentNumber"
          tabindex="-1"
          target="_blank"
          :to="{
            name: 'caselaw-documentUnit-documentNumber-preview',
            params: { documentNumber: props.decision.documentNumber },
          }"
        >
          <LinkElement :title="props.decision.documentNumber" />
        </RouterLink>
      </div>
      <div v-else-if="props.displayMode === DisplayMode.SIDEPANEL">
        <button @click="openSidePanel(props.decision.documentNumber)">
          <LinkElement :title="props.decision.documentNumber" />
        </button>
      </div>
    </div>

    <div v-else>
      <p>
        {{ props.decision.documentNumber }}
      </p>
    </div>
  </FlexContainer>
</template>

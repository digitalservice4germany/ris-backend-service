<script setup lang="ts">
import DecisionSummary from "@/components/DecisionSummary.vue"
import { DisplayMode } from "@/components/enumDisplayMode"
import FlexContainer from "@/components/FlexContainer.vue"
import TextButton from "@/components/input/TextButton.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import errorMessages from "@/i18n/errors.json"
import IconAdd from "~icons/ic/baseline-add"

const { allowMultipleLinks = false } = defineProps<{
  searchResults?: SearchResults<RelatedDocumentation>
  isLoading: boolean
  allowMultipleLinks?: boolean
  displayMode?: DisplayMode
}>()

const emits =
  defineEmits<(event: "linkDecision", decision: RelatedDocumentation) => void>()
</script>

<script lang="ts">
export type SearchResults<Type extends RelatedDocumentation> = {
  decision: Type
  isLinked: boolean
}[]
</script>

<template>
  <div>
    <FlexContainer
      v-if="isLoading"
      class="m-24"
      justify-content="justify-center"
    >
      <LoadingSpinner />
    </FlexContainer>

    <div
      v-else-if="searchResults?.length === 0"
      class="grid justify-items-center"
    >
      <FlexContainer class="m-24" justify-content="justify-center">
        <p>{{ errorMessages.SEARCH_RESULTS_NOT_FOUND.title }}</p>
      </FlexContainer>
    </div>

    <FlexContainer
      v-else-if="searchResults"
      class="p-24"
      flex-direction="flex-col"
    >
      <p class="ds-label-01-bold">Passende Suchergebnisse:</p>
      <FlexContainer
        v-for="searchResult in searchResults"
        :key="searchResult.decision.uuid"
        class="mt-16"
      >
        <FlexContainer align-items="items-center">
          <TextButton
            aria-label="Treffer übernehmen"
            class="mr-16"
            data-testid="add-decision-button"
            :disabled="!allowMultipleLinks && searchResult.isLinked"
            :icon="IconAdd"
            size="small"
            @click.stop="emits('linkDecision', searchResult.decision)"
          />
          <DecisionSummary
            :decision="searchResult.decision"
            :display-mode="displayMode"
          ></DecisionSummary>
          <span
            v-if="searchResult.isLinked"
            class="ds-label-02-reg ml-8 rounded-full bg-yellow-400 px-8 py-2"
            >Bereits hinzugefügt</span
          >
        </FlexContainer>
      </FlexContainer>
    </FlexContainer>
  </div>
</template>

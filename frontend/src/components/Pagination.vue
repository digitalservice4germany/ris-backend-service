<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
import TextButton from "./input/TextButton.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import { ServiceResponse } from "@/services/httpClient"
import IconArrowBack from "~icons/ic/baseline-arrow-back"
import IconArrowForward from "~icons/ic/baseline-arrow-forward"

const props = withDefaults(
  defineProps<{
    page?: Page<any> // eslint-disable-line @typescript-eslint/no-explicit-any
    navigationPosition?: "top" | "bottom"
    isLoading?: boolean
  }>(),
  { page: undefined, navigationPosition: "top", isLoading: false },
)

const emits = defineEmits<(e: "updatePage", page: number) => void>()

async function nextPage(): Promise<void> {
  if (props.page && !props.page.last) {
    emits("updatePage", props.page.number + 1)
  }
}

async function previousPage(): Promise<void> {
  if (props.page && !props.page?.first) {
    emits("updatePage", props.page.number - 1)
  }
}
</script>

<script lang="ts">
export type Page<T> = {
  content: T[]
  size: number
  number: number
  numberOfElements: number
  totalElements?: number
  first: boolean
  last: boolean
  empty: boolean
}

export type PageableService<TResult, TQuery = TResult> = (
  page: number,
  size: number,
  query?: TQuery,
) => Promise<ServiceResponse<Page<TResult>>>
</script>

<template>
  <slot v-if="props.navigationPosition == 'bottom'"></slot>
  <FlexContainer
    v-if="page?.content && !page?.empty && !isLoading"
    class="mb-24 mt-20 px-24"
    flex-direction="flex-row"
    justify-content="justify-between"
  >
    <TextButton
      v-if="!(page.first && page.last)"
      aria-label="vorherige Ergebnisse"
      button-type="tertiary"
      :disabled="page?.first"
      :icon="IconArrowBack"
      icon-position="left"
      label="Zurück"
      size="small"
      @click="previousPage"
      @keydown.enter="previousPage"
    >
    </TextButton>
    <FlexContainer
      class="w-full"
      flex-direction="flex-row"
      flex-wrap="flex-wrap"
      justify-content="justify-center"
    >
      <b v-if="!(page.first && page.last)">Seite {{ page.number + 1 }}: </b>
      <span>&nbsp;</span>
      <FlexContainer flex-direction="flex-row">
        {{
          page.numberOfElements == 1
            ? page.numberOfElements + " Ergebnis"
            : page.numberOfElements + " Ergebnisse"
        }}
        {{ page.first && page.last ? "gefunden" : "angezeigt" }}
      </FlexContainer>
    </FlexContainer>
    <TextButton
      v-if="!(page.first && page.last)"
      aria-label="nächste Ergebnisse"
      button-type="tertiary"
      :disabled="page?.last"
      :icon="IconArrowForward"
      icon-position="right"
      label="Weiter"
      size="small"
      @click="nextPage"
      @keydown.enter="nextPage"
    >
    </TextButton>
  </FlexContainer>
  <slot v-if="props.navigationPosition == 'top'"></slot>
</template>

<script setup lang="ts">
import FlexContainer from "@/components/FlexContainer.vue"
import BaselineArrowOutward from "~icons/ic/baseline-arrow-outward"

const props = defineProps<{
  title?: string
}>()
</script>

<template>
  <button
    class="ds-link-01-bold flex flex-row border-b-2 border-blue-800 leading-24 no-underline focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800"
  >
    <FlexContainer align-items="items-center" flex-direction="flex-row">
      <p>{{ props.title }}</p>
      <BaselineArrowOutward />
    </FlexContainer>
  </button>
</template>

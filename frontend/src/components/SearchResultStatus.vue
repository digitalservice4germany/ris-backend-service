<script setup lang="ts">
import FlexContainer from "@/components/FlexContainer.vue"

const props = defineProps<{ text?: string }>()
</script>
<template>
  <FlexContainer
    v-if="props.text"
    align-items="items-center"
    class="py-112"
    flex-direction="flex-col"
  >
    <span>{{ props.text }} </span>
    <slot />
  </FlexContainer>
</template>

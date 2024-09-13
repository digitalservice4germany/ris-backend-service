<script lang="ts" setup>
const props = defineProps<{
  subtitle?: string
}>()
</script>

<template>
  <div
    class="sticky top-0 z-30 flex h-[64px] flex-row items-center border-b border-solid border-gray-400 bg-blue-100 px-24 py-12"
  >
    <h1 class="text font-bold" data-testid="periodicalInfoPanelTitle">
      Periodikumsauswertung
    </h1>
    <span v-if="props.subtitle" class="m-4"> | </span>
    <div v-if="props.subtitle">{{ props.subtitle }}</div>
  </div>
</template>

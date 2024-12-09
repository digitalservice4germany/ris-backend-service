<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue"

interface Props {
  text: string
  shortcut?: string
}
withDefaults(defineProps<Props>(), {
  shortcut: undefined,
})

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === "Escape") {
    closeTooltip()
  }
}

const tooltipVisible = ref(false)

// Add the keydown event listener when the component is mounted
onMounted(() => {
  window.addEventListener("keydown", handleKeyDown)
})

// Cleanup the event listener when the component is unmounted
onUnmounted(() => {
  window.removeEventListener("keydown", handleKeyDown)
})

const openTooltip = () => {
  tooltipVisible.value = true
}

const closeTooltip = () => {
  tooltipVisible.value = false
}
</script>

<template>
  <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
  <div
    class="relative"
    @blur="closeTooltip"
    @click="closeTooltip"
    @focus="openTooltip"
    @focusin="openTooltip"
    @focusout="closeTooltip"
    @keydown="closeTooltip"
    @mouseenter="openTooltip"
    @mouseleave="closeTooltip"
    @touchstart="openTooltip"
  >
    <slot />
    <div
      v-if="tooltipVisible"
      class="ds-label-03-reg absolute left-[50%] top-[110%] z-20 w-max max-w-[18em] translate-x-[-50%] whitespace-pre-line rounded bg-gray-900 px-8 py-4 text-center text-white"
      role="tooltip"
    >
      {{ text }}
      <div v-if="shortcut">{{ shortcut }}</div>
    </div>
  </div>
</template>

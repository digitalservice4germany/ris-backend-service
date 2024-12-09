<script setup lang="ts">
const props = defineProps<{
  size?: "small"
}>()
const sizeClasses = props.size === "small" ? "h-24 w-24" : "h-32 w-32"
</script>

<template>
  <div
    aria-label="Ladestatus"
    class="inline-block animate-spin rounded-full border-[3px] border-solid border-blue-900 border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]"
    :class="sizeClasses"
    role="status"
  ></div>
</template>

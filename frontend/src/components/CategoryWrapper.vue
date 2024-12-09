<script lang="ts" setup>
import { ref } from "vue"
import TextButton from "@/components/input/TextButton.vue"
import IconAdd from "~icons/material-symbols/add"

const props = defineProps<{
  classes?: string
  label: string
  shouldShowButton: boolean
}>()

const emit = defineEmits<{
  toggled: [value: boolean]
}>()

const shouldShowButton = ref<boolean>(props.shouldShowButton)

function toggle() {
  shouldShowButton.value = !shouldShowButton.value
  emit("toggled", shouldShowButton.value)
}
</script>

<template>
  <TextButton
    v-if="shouldShowButton"
    button-type="tertiary"
    :class="classes"
    data-testid="category-wrapper-button"
    :icon="IconAdd"
    :label="label"
    size="small"
    @click="toggle"
  />
  <div v-else data-testid="category-wrapper-component">
    <slot :reset="toggle" />
  </div>
</template>

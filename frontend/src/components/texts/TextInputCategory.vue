<script lang="ts" setup>
import { nextTick } from "vue"
import CategoryWrapper from "@/components/CategoryWrapper.vue"
import TextInput from "@/components/input/TextInput.vue"

interface Props {
  id: string
  label: string
  shouldShowButton: boolean
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
})

defineEmits<{
  "update:modelValue": [value: string | undefined]
}>()

async function focusInput() {
  await nextTick()
  const inputElement = document.getElementById(props.id)
  inputElement?.focus()
}
</script>

<template>
  <CategoryWrapper
    :label="label"
    :should-show-button="shouldShowButton"
    @toggled="focusInput"
  >
    <div class="flex flex-col">
      <label class="ds-label-02-reg mb-4" :for="id">{{ label }}</label>

      <TextInput
        :id="id"
        :aria-label="label"
        :model-value="modelValue"
        size="medium"
        @update:model-value="$emit('update:modelValue', $event)"
      />
    </div>
  </CategoryWrapper>
</template>

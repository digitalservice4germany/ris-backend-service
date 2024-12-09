<script lang="ts" setup>
import { InputHTMLAttributes, computed, ref } from "vue"
import { ValidationError } from "@/components/input/types"

interface Props {
  id: string
  modelValue?: string
  ariaLabel: string
  placeholder?: string
  readOnly?: boolean
  fullHeight?: boolean
  hasError?: boolean
  size?: "regular" | "medium" | "small"
  type?: InputHTMLAttributes["type"]
  dataTestId?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: "",
  placeholder: undefined,
  size: "regular",
  type: "text",
  dataTestId: "",
})

const emit = defineEmits<{
  "update:modelValue": [value: string | undefined]
  "update:validationError": [value: ValidationError | undefined]
  "enter-released": []
}>()

const inputRef = ref<HTMLInputElement | null>()

const localModelValue = computed({
  get: () => props.modelValue,
  set: (value) =>
    value === ""
      ? emit("update:modelValue", undefined)
      : emit("update:modelValue", value),
})

const conditionalClasses = computed(() => ({
  "has-error": props.hasError,
  "h-full": props.fullHeight,
  "ds-input-medium": props.size === "medium",
  "ds-input-small": props.size === "small",
}))

const tabindex = computed(() => (props.readOnly ? -1 : 0))

function focusInput() {
  inputRef.value?.focus()
}

defineExpose({ focusInput, inputRef })
</script>

<template>
  <input
    :id="id"
    ref="inputRef"
    v-model="localModelValue"
    :aria-label="ariaLabel"
    class="ds-input"
    :class="conditionalClasses"
    :data-testid="dataTestId"
    :placeholder="placeholder"
    :readonly="readOnly"
    :tabindex="tabindex"
    :type="type"
    @input="$emit('update:validationError', undefined)"
    @keyup.enter="$emit('enter-released')"
  />
</template>

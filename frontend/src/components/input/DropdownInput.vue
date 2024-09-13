<script lang="ts" setup>
import { computed } from "vue"
import { DropdownInputModelType, DropdownItem } from "@/components/input/types"

const props = defineProps<{
  items: DropdownItem[]
  modelValue?: DropdownInputModelType
  placeholder?: string
  readOnly?: boolean
  isSmall?: boolean
}>()

const emit = defineEmits<{
  "update:modelValue": [DropdownInputModelType | undefined]
}>()

const localModelValue = computed({
  get: () => props.modelValue ?? "",
  set: (value) => {
    emit("update:modelValue", value)
  },
})

const conditionalClasses = computed(() => ({
  "ds-select-small": props.isSmall,
  "ds-select-medium": !props.isSmall,
}))

const hasPlaceholder = computed(() =>
  Boolean(!props.modelValue && props.placeholder),
)
</script>

<template>
  <!-- Label should come from the surrounding context, e.g. InputField component -->
  <!-- eslint-disable vuejs-accessibility/form-control-has-label -->
  <select
    v-model="localModelValue"
    class="ds-select"
    :class="conditionalClasses"
    :data-placeholder="hasPlaceholder ? true : undefined"
    :disabled="readOnly"
    tabindex="0"
  >
    <option v-if="placeholder && !localModelValue" disabled value="">
      {{ placeholder }}
    </option>
    <option v-for="item in items" :key="item.value" :value="item.value">
      {{ item.label }}
    </option>
  </select>
</template>

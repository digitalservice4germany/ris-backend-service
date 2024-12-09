<script lang="ts" setup>
import dayjs from "dayjs"
import customParseFormat from "dayjs/plugin/customParseFormat"
import { Mask } from "maska"
import { vMaska } from "maska/vue"
import { computed, onMounted, ref, watch } from "vue"
import { ValidationError } from "@/components/input/types"

interface Props {
  id: string
  value?: string
  modelValue?: string
  ariaLabel: string
  isFutureDate?: boolean
  hasError?: boolean
  size?: "regular" | "medium" | "small"
  readOnly?: boolean
  disabled?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "update:modelValue": [value?: string]
  "update:validationError": [value?: ValidationError]
}>()

const mask = "##.##.####"
const inputCompleted = computed(
  () => inputValue.value && new Mask({ mask }).completed(inputValue.value),
)

const inputValue = ref(
  props.modelValue ? dayjs(props.modelValue).format("DD.MM.YYYY") : undefined,
)

dayjs.extend(customParseFormat)

const isValidDate = computed(() => {
  return dayjs(inputValue.value, "DD.MM.YYYY", true).isValid()
})

const isInPast = computed(() => {
  if (props.isFutureDate) return true
  return dayjs(inputValue.value, "DD.MM.YYYY", true).isBefore(dayjs())
})

const conditionalClasses = computed(() => ({
  "has-error": props.hasError,
  "ds-input-medium": props.size === "medium",
  "ds-input-small": props.size === "small",
}))

function validateInput() {
  if (inputCompleted.value) {
    if (isValidDate.value) {
      // if valid date, check for future dates
      if (!isInPast.value && !props.isFutureDate && isValidDate.value)
        emit("update:validationError", {
          message: "Das Datum darf nicht in der Zukunft liegen",
          instance: props.id,
        })
      else emit("update:validationError", undefined)
    } else {
      emit("update:validationError", {
        message: "Kein valides Datum",
        instance: props.id,
      })
    }
  } else if (inputValue.value) {
    emit("update:validationError", {
      message: "Unvollständiges Datum",
      instance: props.id,
    })
  } else {
    emit("update:validationError", undefined)
  }
}

onMounted(() => {
  if (inputValue.value) validateInput()
})

function backspaceDelete() {
  emit("update:validationError", undefined)
  if (inputValue.value === "") emit("update:modelValue", inputValue.value)
}

function onBlur() {
  validateInput()
}

watch(
  () => props.modelValue,
  (is) => {
    inputValue.value = is
      ? dayjs(is, "YYYY-MM-DD", true).format("DD.MM.YYYY")
      : undefined
  },
)

watch(inputValue, (is) => {
  if (is === "") emit("update:modelValue", undefined)
  if (isValidDate.value && isInPast.value) {
    emit(
      "update:modelValue",
      dayjs(is, "DD.MM.YYYY", true).format("YYYY-MM-DD"),
    )
  }

  if (inputCompleted.value) {
    validateInput()
  }
})
</script>

<template>
  <input
    :id="id"
    v-model="inputValue"
    v-maska="mask"
    :aria-label="ariaLabel"
    class="ds-input"
    :class="conditionalClasses"
    :disabled="disabled"
    placeholder="TT.MM.JJJJ"
    :readonly="readOnly"
    @blur="onBlur"
    @focus="emit('update:validationError', undefined)"
    @keydown.delete="backspaceDelete"
  />
</template>

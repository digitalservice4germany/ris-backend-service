<script lang="ts" setup>
import dayjs from "dayjs"
import customParseFormat from "dayjs/plugin/customParseFormat"
import { computed, ref } from "vue"
import ChipsInput from "@/components/input/ChipsInput.vue"
import { ValidationError } from "@/components/input/types"

interface Props {
  id: string
  modelValue?: string[]
  ariaLabel: string
  hasError?: boolean
  readOnly?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "update:modelValue": [value?: string[]]
  "update:validationError": [value?: ValidationError]
}>()

const lastChipValue = ref<string | undefined>("")
const isValidYear = computed(() => validateYear(lastChipValue.value))
const isInFuture = computed(() =>
  dayjs(lastChipValue.value, "YYYY", true).isAfter(dayjs()),
)

const chips = computed<string[]>({
  get: () => {
    return props.modelValue ? props.modelValue : []
  },

  set: (newValue: string[]) => {
    if (!newValue || newValue.length === 0) {
      emit("update:modelValue", undefined)
      return
    }

    lastChipValue.value = newValue.at(-1)

    validateInput()

    if (isValidYear.value && !isInFuture.value)
      emit("update:modelValue", newValue)
  },
})

function validateInput(event?: ValidationError) {
  if (event) {
    emit("update:validationError", event)
    return
  }
  if (!isValidYear.value && lastChipValue.value) {
    emit("update:validationError", {
      message: "Kein valides Jahr",
      instance: props.id,
    })
  } else if (isInFuture.value) {
    emit("update:validationError", {
      message: props.ariaLabel + " darf nicht in der Zukunft liegen",
      instance: props.id,
    })
    return
  } else {
    emit("update:validationError", undefined)
  }
}

function validateYear(input: string | undefined): boolean {
  if (!input || input.length < 4) return false

  const date = dayjs(input, "YYYY", true)
  return date.isValid() && date.year() >= 1000 && date.year() <= 9999
}

dayjs.extend(customParseFormat)
</script>

<template>
  <ChipsInput
    :id="id"
    v-model="chips"
    :aria-label="ariaLabel"
    :has-error="hasError"
    maska="####"
    :read-only="readOnly"
    @update:validation-error="validateInput($event)"
  />
</template>

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
const isValidDate = computed(() =>
  dayjs(lastChipValue.value, "DD.MM.YYYY", true).isValid(),
)
const isInFuture = computed(() =>
  dayjs(lastChipValue.value, "DD.MM.YYYY", true).isAfter(dayjs()),
)

const chips = computed<string[]>({
  get: () => {
    return props.modelValue
      ? props.modelValue.map((value) =>
          dayjs(value, "YYYY-MM-DD", true).format("DD.MM.YYYY"),
        )
      : []
  },

  set: (newValue: string[]) => {
    if (!newValue || newValue.length === 0) {
      emit("update:modelValue", [])
      return
    }

    lastChipValue.value = newValue.at(-1)
    validateInput()

    if (isValidDate.value && !isInFuture.value)
      emit(
        "update:modelValue",
        newValue.map((value) =>
          dayjs(value, "DD.MM.YYYY", true).format("YYYY-MM-DD"),
        ),
      )
  },
})

function validateInput(event?: ValidationError) {
  if (event) {
    emit("update:validationError", event)
    return
  }
  if (!isValidDate.value && lastChipValue.value) {
    emit("update:validationError", {
      message: "Kein valides Datum",
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

dayjs.extend(customParseFormat)
</script>

<template>
  <ChipsInput
    :id="id"
    v-model="chips"
    :aria-label="ariaLabel"
    data-testid="deviating-decision-dates"
    :has-error="hasError"
    maska="##.##.####"
    :read-only="readOnly"
    @update:validation-error="validateInput($event)"
  />
</template>
